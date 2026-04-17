/**
 * ai-analyze.mjs
 *
 * Uses GitHub Models to analyze captured YouTube TV HTML fixtures and suggest
 * ad-blocking improvements to app/src/main/res/raw/userscripts.js.
 *
 * Environment variables:
 *   FIXTURES_DIR          - path to the fixtures directory (default: ./fixtures)
 *   USERSCRIPTS_PATH      - path to userscripts.js (default: resolved relative to this file)
 *   GH_MODELS_TOKEN       - GitHub token with models:read scope (falls back to GITHUB_TOKEN / GH_TOKEN)
 *   GITHUB_TOKEN          - standard Actions token (fallback)
 *   GH_TOKEN              - PAT fallback
 *   GITHUB_MODELS_MODEL   - model to use (default: openai/gpt-4.1)
 *
 * Outputs a markdown report to stdout.
 * If the AI detects new ad patterns it writes the updated userscripts.js in place.
 */

import { readFile, writeFile } from 'node:fs/promises';
import { existsSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

const FIXTURES_DIR = path.resolve(process.env.FIXTURES_DIR || path.join(__dirname, 'fixtures'));
const USERSCRIPTS_PATH = path.resolve(
    process.env.USERSCRIPTS_PATH ||
    path.join(__dirname, '../../app/src/main/res/raw/userscripts.js'),
);

const GITHUB_MODELS_API_URL = 'https://models.github.ai/inference/chat/completions';
const GITHUB_API_VERSION = '2026-03-10';
const MODEL = process.env.GITHUB_MODELS_MODEL || 'openai/gpt-4.1';

/** Ad-related URL substrings used to identify ad scripts and network requests. */
const AD_URL_KEYWORDS = [
    'pagead',
    'doubleclick',
    'adservice',
    'googlesyndication',
    'adsbygoogle',
    'googleads',
    'ad_request',
    'adsystem',
];

/**
 * JSON keys that indicate ad-related data in YouTube's API responses.
 * These are what we look for in inline scripts to surface new patterns to the AI.
 */
const AD_JSON_KEYS = [
    'adPlacements',
    'playerAds',
    'adSlots',
    'adSlotRenderer',
    'promotedSparklesTextSearchRenderer',
    'mastheadAd',
    'adBreakHeartbeatParams',
    'adMetadata',
    'adPreviewRenderer',
    'adInfoRenderer',
    'adBadgeRenderer',
    'companionAdSlot',
    'linearAdSequenceRenderer',
    'instreamVideoAdRenderer',
    'adLayoutLoggingData',
    'adActionInterstitialRenderer',
    'adDurationRemaining',
];

const AD_CLASS_PATTERNS = [
    /\bad[-_]?slot\b/i,
    /\bad[-_]?container\b/i,
    /\bad[-_]?banner\b/i,
    /\bad[-_]?overlay\b/i,
    /\bad[-_]?unit\b/i,
    /\bsponsored\b/i,
    /\bytp[-_]?ad\b/i,
];

// ---------------------------------------------------------------------------
// Token resolution
// ---------------------------------------------------------------------------

function resolveToken() {
    return (
        process.env.GH_MODELS_TOKEN ||
        process.env.GITHUB_MODELS_TOKEN ||
        process.env.GITHUB_TOKEN ||
        process.env.GH_TOKEN ||
        ''
    ).trim();
}

// ---------------------------------------------------------------------------
// HTML analysis helpers
// ---------------------------------------------------------------------------

/**
 * Extracts ad-related signals from a captured HTML page:
 *  - External script URLs that match ad networks
 *  - Class/ID attributes matching known ad patterns
 *  - Short snippets from inline scripts that mention ad JSON keys
 */
function extractAdPatterns(html) {
    // External script URLs
    const adScriptUrls = [];
    const scriptSrcRe = /<script[^>]+\bsrc\s*=\s*["']([^"']+)["']/gi;
    for (const m of html.matchAll(scriptSrcRe)) {
        const url = m[1];
        if (AD_URL_KEYWORDS.some((kw) => url.toLowerCase().includes(kw))) {
            adScriptUrls.push(url);
        }
    }

    // Ad-related class/id attribute values
    const adClasses = new Set();
    const classRe = /(?:class|id)\s*=\s*["']([^"']*)["']/gi;
    for (const m of html.matchAll(classRe)) {
        const value = m[1];
        if (AD_CLASS_PATTERNS.some((re) => re.test(value))) {
            adClasses.add(value.trim());
        }
    }

    // Inline script snippets that reference ad JSON keys
    const adJsonSnippets = [];
    const inlineScriptRe = /<script(?![^>]*\bsrc\s*=)[^>]*>([\s\S]*?)<\/script>/gi;
    for (const scriptMatch of html.matchAll(inlineScriptRe)) {
        const content = scriptMatch[1];
        const foundKeys = AD_JSON_KEYS.filter((k) => content.includes(k));
        if (foundKeys.length === 0) continue;

        for (const key of foundKeys.slice(0, 4)) {
            const idx = content.indexOf(key);
            const start = Math.max(0, idx - 80);
            const end = Math.min(content.length, idx + 400);
            adJsonSnippets.push({
                key,
                snippet: content.slice(start, end).replace(/\s+/g, ' ').trim(),
            });
        }
        // Avoid overwhelming the prompt – keep at most 8 snippets total
        if (adJsonSnippets.length >= 8) break;
    }

    return { adScriptUrls, adClasses: [...adClasses], adJsonSnippets };
}

// ---------------------------------------------------------------------------
// userscripts.js helpers
// ---------------------------------------------------------------------------

/**
 * Locates the JSON.parse override block that handles ad filtering.
 * Returns the code string plus its start/end offsets in the source.
 */
function extractAdBlockSection(source) {
    const startMarker = 'const origParse = JSON.parse;';
    const endMarker = 'function getShelfTitleText(';
    const start = source.indexOf(startMarker);
    const end = source.indexOf(endMarker);
    if (start === -1 || end === -1) return null;
    return { code: source.slice(start, end).trimEnd(), start, end };
}

// ---------------------------------------------------------------------------
// GitHub Models API
// ---------------------------------------------------------------------------

async function callGitHubModels(token, messages) {
    const response = await fetch(GITHUB_MODELS_API_URL, {
        method: 'POST',
        headers: {
            Accept: 'application/vnd.github+json',
            Authorization: `Bearer ${token}`,
            'Content-Type': 'application/json',
            'X-GitHub-Api-Version': GITHUB_API_VERSION,
        },
        body: JSON.stringify({
            model: MODEL,
            messages,
            temperature: 0.1,
            max_tokens: 3000,
            stream: false,
        }),
    });

    if (!response.ok) {
        const text = await response.text();
        throw new Error(`GitHub Models API ${response.status}: ${text.slice(0, 500)}`);
    }

    const data = await response.json();
    return data.choices[0].message.content.trim();
}

// ---------------------------------------------------------------------------
// Prompt builder
// ---------------------------------------------------------------------------

function buildPrompt(adBlockCode, pageAnalyses) {
    const pagesSection = pageAnalyses
        .map(({ name, url, adScriptUrls, adClasses, adJsonSnippets }) => {
            const parts = [`### Page: ${name} (${url})`];

            if (adScriptUrls.length) {
                parts.push('Ad-related external scripts: ' + adScriptUrls.join(', '));
            }
            if (adClasses.length) {
                parts.push('Ad-related class/id attributes: ' + adClasses.slice(0, 15).join(', '));
            }
            if (adJsonSnippets.length) {
                parts.push('Inline JSON snippets referencing ad keys:');
                adJsonSnippets.forEach(({ key, snippet }) => {
                    parts.push(`  [${key}]: ...${snippet.slice(0, 300)}...`);
                });
            }
            if (!adScriptUrls.length && !adClasses.length && !adJsonSnippets.length) {
                parts.push('No ad signals detected on this page.');
            }

            return parts.join('\n');
        })
        .join('\n\n');

    return `You are analyzing YouTube TV page data to improve ad-blocking in an Android WebView userscript.

The userscript intercepts JSON.parse() calls and strips ad-related fields from YouTube's API responses before they reach the player.

CURRENT AD-BLOCK CODE (the JSON.parse override inside userscripts.js):
\`\`\`javascript
${adBlockCode}
\`\`\`

CAPTURED PAGE ANALYSIS:
${pagesSection}

YOUR TASK:
1. Review the inline JSON snippets for ad-related keys NOT already handled by the current code.
2. Keys already handled: adPlacements, playerAds, adSlots, adSlotRenderer (masthead via filter).
3. If new patterns are found (new renderer types, new JSON keys containing ad data), suggest additions to the JSON.parse override.
4. Only add code when you have concrete evidence from the snippets above. Do not speculate.
5. Preserve all existing logic — only append new if/delete blocks inside JSON.parse.

RETURN ONLY VALID JSON (no markdown fences, no extra text):
{
  "hasChanges": false,
  "summary": "What you found and why changes are or are not needed",
  "newPatterns": [],
  "updatedCode": null
}

If hasChanges is true:
- Set updatedCode to the complete replacement block starting with "const origParse = JSON.parse;" and ending just before "function getShelfTitleText(" (do NOT include that function).
- List the new top-level JSON key names you are now blocking in newPatterns.`;
}

// ---------------------------------------------------------------------------
// Main
// ---------------------------------------------------------------------------

async function run() {
    const token = resolveToken();
    if (!token) {
        console.error(
            'No GitHub token found. Set GH_MODELS_TOKEN, GITHUB_TOKEN, or GH_TOKEN.',
        );
        process.exitCode = 1;
        return;
    }

    const manifestPath = path.join(FIXTURES_DIR, 'manifest.json');
    if (!existsSync(manifestPath)) {
        console.error(`Manifest not found at ${manifestPath}. Run the capture step first.`);
        process.exitCode = 1;
        return;
    }

    const manifest = JSON.parse(await readFile(manifestPath, 'utf8'));

    const userscripts = await readFile(USERSCRIPTS_PATH, 'utf8');
    const adBlock = extractAdBlockSection(userscripts);
    if (!adBlock) {
        console.error(
            'Could not locate the JSON.parse override block in userscripts.js. ' +
            'Expected markers: "const origParse = JSON.parse;" and "function getShelfTitleText(".',
        );
        process.exitCode = 1;
        return;
    }

    // Analyze each captured HTML page
    const pageAnalyses = [];
    for (const page of manifest.pages) {
        const htmlPath = path.isAbsolute(page.html) ? page.html : path.join(FIXTURES_DIR, page.html);
        if (!existsSync(htmlPath)) continue;
        const html = await readFile(htmlPath, 'utf8');
        pageAnalyses.push({
            name: page.name,
            url: page.finalUrl ?? page.url ?? page.name,
            ...extractAdPatterns(html),
        });
    }

    if (pageAnalyses.length === 0) {
        const report =
            '## AI Ad-Filter Analysis\n\nNo fixture HTML files found. Skipping AI analysis.';
        process.stdout.write(report + '\n');
        return;
    }

    const prompt = buildPrompt(adBlock.code, pageAnalyses);

    console.error(`Calling GitHub Models (${MODEL}) for ad-filter analysis…`);
    let rawResponse;
    try {
        rawResponse = await callGitHubModels(token, [
            {
                role: 'system',
                content:
                    'You are a careful code analysis assistant. Return only valid JSON without markdown code fences.',
            },
            { role: 'user', content: prompt },
        ]);
    } catch (err) {
        const report = [
            '## AI Ad-Filter Analysis',
            '',
            `⚠️ GitHub Models API call failed: ${err.message}`,
        ].join('\n');
        process.stdout.write(report + '\n');
        // Don't set exitCode — let the workflow continue with the static analysis
        return;
    }

    // Parse AI response
    let result;
    try {
        const jsonMatch = rawResponse.match(/\{[\s\S]*\}/);
        if (!jsonMatch) throw new Error('No JSON object found in response');
        result = JSON.parse(jsonMatch[0]);
    } catch (err) {
        const report = [
            '## AI Ad-Filter Analysis',
            '',
            `⚠️ Failed to parse AI response: ${err.message}`,
            '',
            '<details><summary>Raw AI response</summary>',
            '',
            '```',
            rawResponse.slice(0, 1500),
            '```',
            '</details>',
        ].join('\n');
        process.stdout.write(report + '\n');
        return;
    }

    // Build markdown report
    const lines = [
        '## AI Ad-Filter Analysis',
        '',
        `**Model:** ${MODEL}`,
        `**Fixtures captured at:** ${manifest.capturedAt}`,
        '',
        '### Summary',
        '',
        result.summary || 'No summary provided.',
        '',
    ];

    if (result.hasChanges && result.updatedCode) {
        const trimmedCode = result.updatedCode.trimEnd();

        // Splice the new code into the source, preserving everything else
        const updatedSource =
            userscripts.slice(0, adBlock.start) +
            trimmedCode +
            '\n\n  ' +
            userscripts.slice(adBlock.end);

        await writeFile(USERSCRIPTS_PATH, updatedSource, 'utf8');
        console.error('✅ userscripts.js updated with new ad-blocking patterns.');

        lines.push('### Changes Applied to `userscripts.js`');
        lines.push('');
        lines.push(
            `✅ Updated with **${result.newPatterns?.length ?? 'new'}** new ad-blocking pattern(s).`,
        );
        if (result.newPatterns?.length) {
            lines.push('');
            lines.push('**New patterns now blocked:**');
            result.newPatterns.forEach((p) => lines.push(`- \`${p}\``));
        }
        lines.push('');
        lines.push('<details><summary>Updated ad-block code</summary>');
        lines.push('');
        lines.push('```javascript');
        lines.push(trimmedCode);
        lines.push('```');
        lines.push('</details>');
    } else {
        lines.push('### Result');
        lines.push('');
        lines.push('✅ No new ad patterns detected. `userscripts.js` is up to date.');
    }

    process.stdout.write(lines.join('\n') + '\n');
}

run().catch((err) => {
    console.error(err);
    process.exitCode = 1;
});
