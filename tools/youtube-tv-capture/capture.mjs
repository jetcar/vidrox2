import { mkdir, writeFile } from 'node:fs/promises';
import path from 'node:path';
import { chromium } from 'playwright';

const DEFAULT_CAPTURES = [
  { name: 'home', url: 'https://www.youtube.com/tv' },
  { name: 'browse', url: 'https://www.youtube.com/tv#/browse' },
  { name: 'details', url: 'https://www.youtube.com/tv#/watch?v=dQw4w9WgXcQ' },
];

const YOUTUBE_TV_USER_AGENT =
  process.env.YOUTUBE_TV_USER_AGENT || 'Mozilla/5.0 Cobalt/25 (Sony, PS4, Wired)';
const OUTPUT_DIR = path.resolve(process.env.OUTPUT_DIR || './out');
const STORAGE_STATE_PATH = process.env.STORAGE_STATE_PATH;
const WAIT_MS = Number.parseInt(process.env.WAIT_MS || '8000', 10);
const VIEWPORT_WIDTH = Number.parseInt(process.env.VIEWPORT_WIDTH || '3840', 10);
const VIEWPORT_HEIGHT = Number.parseInt(process.env.VIEWPORT_HEIGHT || '2160', 10);
const SKIP_SCREENSHOT = process.env.SKIP_SCREENSHOT === 'true';
const SKIP_HAR = process.env.SKIP_HAR === 'true';

function sanitizeName(value) {
  return value.replace(/[^a-z0-9._-]+/gi, '_').replace(/^_+|_+$/g, '').toLowerCase();
}

const SENSITIVE_PATH_PATTERNS = [
  '/o/oauth2/',
  '/oauth2/',
  '/token',
  '/signin',
  '/logout',
];

// Redact Google API keys (AIza... format) from captured content so they are
// never committed to version control.
const GOOGLE_API_KEY_PATTERN = /AIza[0-9A-Za-z\-_]{35}/g;

function sanitizeContent(text) {
  return text.replace(GOOGLE_API_KEY_PATTERN, 'REDACTED');
}

function shouldCaptureResponse(url, contentType) {
  if (!url.includes('youtube.com')) {
    return false;
  }

  let parsedPath;
  try {
    parsedPath = new URL(url).pathname;
  } catch {
    parsedPath = url;
  }

  if (SENSITIVE_PATH_PATTERNS.some((pattern) => parsedPath.includes(pattern))) {
    return false;
  }

  if (url.includes('/youtubei/v1/')) {
    return true;
  }

  return Boolean(contentType && contentType.includes('application/json'));
}

function buildCaptureTargets() {
  const raw = process.env.CAPTURE_URLS_JSON;
  if (!raw) {
    return DEFAULT_CAPTURES;
  }

  const parsed = JSON.parse(raw);
  if (!Array.isArray(parsed) || parsed.length === 0) {
    throw new Error('CAPTURE_URLS_JSON must be a non-empty JSON array.');
  }

  return parsed.map((entry, index) => {
    if (!entry || typeof entry.url !== 'string') {
      throw new Error(`CAPTURE_URLS_JSON[${index}] is missing a string url.`);
    }

    return {
      name: sanitizeName(entry.name || `capture-${index + 1}`),
      url: entry.url,
    };
  });
}

async function captureTarget(context, target, manifest) {
  const page = await context.newPage();
  const targetDir = path.join(OUTPUT_DIR, target.name);
  const networkDir = path.join(targetDir, 'network');
  await mkdir(networkDir, { recursive: true });

  const capturedResponses = [];
  let responseIndex = 0;

  page.on('response', async (response) => {
    const url = response.url();
    const headers = response.headers();
    const contentType = headers['content-type'] || '';

    if (!shouldCaptureResponse(url, contentType)) {
      return;
    }

    const body = await response.text().catch(() => null);
    if (body == null) {
      return;
    }

    const fileName = `${String(++responseIndex).padStart(3, '0')}-${sanitizeName(new URL(url).pathname || 'response')}.json`;
    const filePath = path.join(networkDir, fileName);

    await writeFile(filePath, sanitizeContent(body), 'utf8');
    capturedResponses.push({
      url,
      status: response.status(),
      contentType,
      file: path.relative(OUTPUT_DIR, filePath).replace(/\\/g, '/'),
    });
  });

  const startedAt = new Date().toISOString();
  await page.goto(target.url, { waitUntil: 'domcontentloaded', timeout: 120_000 });
  await page.waitForLoadState('networkidle', { timeout: 20_000 }).catch(() => { });
  await page.waitForTimeout(WAIT_MS);

  const htmlPath = path.join(targetDir, `${target.name}.html`);
  const screenshotPath = path.join(targetDir, `${target.name}.png`);
  const metadataPath = path.join(targetDir, `${target.name}.json`);

  await writeFile(htmlPath, sanitizeContent(await page.content()), 'utf8');
  if (!SKIP_SCREENSHOT) {
    await page.screenshot({ path: screenshotPath, fullPage: true });
  }

  const pageMetadata = {
    name: target.name,
    requestedUrl: target.url,
    finalUrl: page.url(),
    title: await page.title(),
    startedAt,
    capturedAt: new Date().toISOString(),
    html: path.relative(OUTPUT_DIR, htmlPath).replace(/\\/g, '/'),
    screenshot: SKIP_SCREENSHOT ? null : path.relative(OUTPUT_DIR, screenshotPath).replace(/\\/g, '/'),
    responseCount: capturedResponses.length,
    responses: capturedResponses,
  };

  await writeFile(metadataPath, JSON.stringify(pageMetadata, null, 2), 'utf8');
  manifest.pages.push(pageMetadata);
  await page.close();
}

async function main() {
  if (!STORAGE_STATE_PATH) {
    throw new Error('STORAGE_STATE_PATH is required. Decode a Playwright storageState JSON file before running capture.');
  }

  const captureTargets = buildCaptureTargets();
  await mkdir(OUTPUT_DIR, { recursive: true });

  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({
    storageState: STORAGE_STATE_PATH,
    userAgent: YOUTUBE_TV_USER_AGENT,
    viewport: { width: VIEWPORT_WIDTH, height: VIEWPORT_HEIGHT },
    screen: { width: VIEWPORT_WIDTH, height: VIEWPORT_HEIGHT },
    deviceScaleFactor: 1,
    colorScheme: 'dark',
    locale: 'en-US',
    ...(SKIP_HAR
      ? {}
      : {
        recordHar: {
          path: path.join(OUTPUT_DIR, 'session.har'),
          mode: 'minimal',
          content: 'embed',
        },
      }),
  });

  const manifest = {
    capturedAt: new Date().toISOString(),
    userAgent: YOUTUBE_TV_USER_AGENT,
    viewport: { width: VIEWPORT_WIDTH, height: VIEWPORT_HEIGHT },
    pages: [],
  };

  try {
    for (const target of captureTargets) {
      await captureTarget(context, target, manifest);
    }
  } finally {
    await context.close();
    await browser.close();
  }

  await writeFile(
    path.join(OUTPUT_DIR, 'manifest.json'),
    JSON.stringify(manifest, null, 2),
    'utf8',
  );
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});

