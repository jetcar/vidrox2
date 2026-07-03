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
  'googletagservices',
  'googleadservices',
  'ad_request',
  'adsystem',
  '2mdn',
];

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

function isAdJsonKey(key) {
  const normalized = key.toLowerCase();
  return AD_JSON_KEYS.some((candidate) => candidate.toLowerCase() === normalized) ||
    /(?:^|[A-Z_])(ad|ads|sponsored|promoted|instream|companion)[A-Za-z0-9_]*$/.test(key);
}

function isAdRendererKey(key) {
  return /(ad|ads|sponsored|promoted|instream|companion)[A-Za-z0-9_]*Renderer$/.test(key);
}

function collectAdPayloadSignals(value, signals) {
  if (!value || typeof value !== 'object') {
    return;
  }

  if (Array.isArray(value)) {
    value.forEach((entry) => collectAdPayloadSignals(entry, signals));
    return;
  }

  for (const [key, child] of Object.entries(value)) {
    if (isAdJsonKey(key)) {
      signals.keys.add(key);
    }
    if (isAdRendererKey(key)) {
      signals.renderers.add(key);
    }
    collectAdPayloadSignals(child, signals);
  }
}

async function inspectCapturedPayloads(fixturesDir, responses) {
  const signals = {
    keys: new Set(),
    renderers: new Set(),
  };

  for (const response of responses ?? []) {
    const responsePath = path.isAbsolute(response.file)
      ? response.file
      : path.join(fixturesDir, response.file);
    if (!existsSync(responsePath)) {
      continue;
    }

    try {
      const parsed = JSON.parse(await readFile(responsePath, 'utf8'));
      const payload = parsed && typeof parsed === 'object' && 'body' in parsed ? parsed.body : parsed;
      collectAdPayloadSignals(payload, signals);
    } catch {
      // Ignore malformed captures so the report still renders for the rest.
    }
  }

  return {
    keys: [...signals.keys].sort(),
    renderers: [...signals.renderers].sort(),
  };
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
  let totalAdKeys = 0;
  let totalAdRenderers = 0;

  for (const page of manifest.pages) {
    const htmlPath = path.isAbsolute(page.html) ? page.html : path.join(FIXTURES_DIR, page.html);
    const html = existsSync(htmlPath) ? await readFile(htmlPath, 'utf8') : '';
    const scriptUrls = extractExternalScriptUrls(html);
    const adScriptUrls = findAdScriptUrls(scriptUrls);
    const adClasses = findAdRelatedAttributes(html);
    const adEndpoints = findAdNetworkEndpoints(page.responses ?? []);
    const adPayloadSignals = await inspectCapturedPayloads(FIXTURES_DIR, page.responses ?? []);

    totalAdScripts += adScriptUrls.length;
    totalAdClasses += adClasses.length;
    totalAdEndpoints += adEndpoints.length;
    totalAdKeys += adPayloadSignals.keys.length;
    totalAdRenderers += adPayloadSignals.renderers.length;

    lines.push(`### Page: ${page.name}`);
    lines.push('');
    lines.push(`- **URL:** ${page.finalUrl}`);
    lines.push(`- **Title:** ${page.title}`);
    lines.push(`- **Network responses captured:** ${page.responseCount}`);
    if (!html) {
      lines.push(`- **HTML fixture:** Missing (\`${page.html}\`)`);
    }
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

    if (adPayloadSignals.keys.length > 0) {
      lines.push('**Ad-related JSON keys found in captured network bodies:**');
      lines.push('```');
      adPayloadSignals.keys.forEach((key) => lines.push(key));
      lines.push('```');
      lines.push('');
    }

    if (adPayloadSignals.renderers.length > 0) {
      lines.push('**Ad-related renderer keys found in captured network bodies:**');
      lines.push('```');
      adPayloadSignals.renderers.forEach((renderer) => lines.push(renderer));
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
  if (totalAdScripts + totalAdClasses + totalAdEndpoints + totalAdKeys + totalAdRenderers === 0) {
    lines.push(
      '✅ No ad-related scripts, class attributes, network endpoints, or payload keys were detected in this capture.',
    );
  } else {
    lines.push(
      `⚠️ Found ${totalAdScripts} ad script URL(s), ${totalAdClasses} ad class/id attribute(s), ${totalAdEndpoints} ad network endpoint(s), ${totalAdKeys} ad payload key(s), and ${totalAdRenderers} ad renderer key(s). See details above.`,
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
