import { readFile } from 'node:fs/promises';
import { existsSync } from 'node:fs';
import path from 'node:path';

const FIXTURES_DIR = path.resolve(process.env.FIXTURES_DIR || './fixtures');

const AD_KEYWORDS = [
  'pagead',
  'doubleclick',
  'adservice',
  'googlesyndication',
  'adsbygoogle',
  'googleads',
  'ad_request',
  'adsystem',
];

const AD_CLASS_PATTERNS = [
  /\bad[-_]?slot\b/i,
  /\bad[-_]?container\b/i,
  /\bad[-_]?banner\b/i,
  /\bad[-_]?overlay\b/i,
  /\bad[-_]?unit\b/i,
  /\bsponsored\b/i,
  /\bpromo[-_]?banner\b/i,
  /\bytp[-_]?ad\b/i,
];

function extractExternalScriptUrls(html) {
  const urls = new Set();
  const pattern = /<script[^>]+\bsrc\s*=\s*["']([^"']+)["']/gi;
  let match;
  while ((match = pattern.exec(html)) !== null) {
    urls.add(match[1]);
  }
  return [...urls].sort();
}

function findAdScriptUrls(scriptUrls) {
  return scriptUrls.filter((url) =>
    AD_KEYWORDS.some((kw) => url.toLowerCase().includes(kw)),
  );
}

function findAdRelatedAttributes(html) {
  const found = new Set();
  const pattern = /(?:class|id)\s*=\s*["']([^"']*)["']/gi;
  let match;
  while ((match = pattern.exec(html)) !== null) {
    const value = match[1];
    if (AD_CLASS_PATTERNS.some((re) => re.test(value))) {
      found.add(value.trim());
    }
  }
  return [...found].sort();
}

function findAdNetworkEndpoints(responses) {
  return responses
    .filter((r) => AD_KEYWORDS.some((kw) => r.url.toLowerCase().includes(kw)))
    .map((r) => r.url);
}

async function analyzeFixtures() {
  const manifestPath = path.join(FIXTURES_DIR, 'manifest.json');
  if (!existsSync(manifestPath)) {
    throw new Error(`No manifest found at ${manifestPath}. Run the capture step first.`);
  }

  const manifest = JSON.parse(await readFile(manifestPath, 'utf8'));
  const lines = [];

  lines.push('## YouTube TV Fixture Analysis');
  lines.push('');
  lines.push(`**Captured at:** ${manifest.capturedAt}`);
  lines.push(`**User agent:** ${manifest.userAgent}`);
  lines.push('');
  lines.push(
    'Review the diff below and update `app/src/main/res/raw/userscripts.js` if any ad-blocking selectors or patterns need to change.',
  );
  lines.push('');

  let totalAdScripts = 0;
  let totalAdClasses = 0;
  let totalAdEndpoints = 0;

  for (const page of manifest.pages) {
    const htmlPath = path.join(FIXTURES_DIR, page.html);
    if (!existsSync(htmlPath)) {
      lines.push(`### Page: ${page.name}`);
      lines.push('');
      lines.push(`> ⚠️ HTML file not found: \`${page.html}\``);
      lines.push('');
      continue;
    }

    const html = await readFile(htmlPath, 'utf8');
    const scriptUrls = extractExternalScriptUrls(html);
    const adScriptUrls = findAdScriptUrls(scriptUrls);
    const adClasses = findAdRelatedAttributes(html);
    const adEndpoints = findAdNetworkEndpoints(page.responses ?? []);

    totalAdScripts += adScriptUrls.length;
    totalAdClasses += adClasses.length;
    totalAdEndpoints += adEndpoints.length;

    lines.push(`### Page: ${page.name}`);
    lines.push('');
    lines.push(`- **URL:** ${page.finalUrl}`);
    lines.push(`- **Title:** ${page.title}`);
    lines.push(`- **Network responses captured:** ${page.responseCount}`);
    lines.push('');

    if (adScriptUrls.length > 0) {
      lines.push('**⚠️ Ad-related external scripts:**');
      lines.push('```');
      adScriptUrls.forEach((url) => lines.push(url));
      lines.push('```');
      lines.push('');
    }

    if (adClasses.length > 0) {
      lines.push('**Ad-related class/id attributes found in HTML:**');
      lines.push('```');
      adClasses.slice(0, 20).forEach((cls) => lines.push(cls));
      if (adClasses.length > 20) {
        lines.push(`… and ${adClasses.length - 20} more`);
      }
      lines.push('```');
      lines.push('');
    }

    if (adEndpoints.length > 0) {
      lines.push('**Ad-related network endpoints captured:**');
      lines.push('```');
      adEndpoints.forEach((url) => lines.push(url));
      lines.push('```');
      lines.push('');
    }

    lines.push(
      `<details><summary>All external scripts (${scriptUrls.length} total)</summary>`,
    );
    lines.push('');
    lines.push('```');
    scriptUrls.forEach((url) => lines.push(url));
    lines.push('```');
    lines.push('</details>');
    lines.push('');
  }

  lines.push('---');
  lines.push('');
  if (totalAdScripts + totalAdClasses + totalAdEndpoints === 0) {
    lines.push(
      '✅ No ad-related scripts, class attributes, or network endpoints were detected in this capture.',
    );
  } else {
    lines.push(
      `⚠️ Found ${totalAdScripts} ad script URL(s), ${totalAdClasses} ad class/id attribute(s), and ${totalAdEndpoints} ad network endpoint(s). See details above.`,
    );
  }

  return lines.join('\n');
}

analyzeFixtures()
  .then((report) => {
    process.stdout.write(report + '\n');
  })
  .catch((error) => {
    console.error(error);
    process.exitCode = 1;
  });
