import { mkdir } from 'node:fs/promises';
import path from 'node:path';
import readline from 'node:readline/promises';
import { stdin as input, stdout as output } from 'node:process';
import { chromium } from 'playwright';

const OUTPUT_PATH = path.resolve(process.argv[2] || './youtube-tv.state.json');
const LOGIN_URL = process.env.YOUTUBE_TV_LOGIN_URL || 'https://www.youtube.com/tv';
const USER_AGENT = process.env.YOUTUBE_TV_USER_AGENT || 'Mozilla/5.0 Cobalt/25 (Sony, PS4, Wired)';

async function main() {
  await mkdir(path.dirname(OUTPUT_PATH), { recursive: true });

  const browser = await chromium.launch({ headless: false });
  const context = await browser.newContext({
    userAgent: USER_AGENT,
    viewport: { width: 1920, height: 1080 },
    screen: { width: 1920, height: 1080 },
    locale: 'en-US',
    colorScheme: 'dark',
  });

  const page = await context.newPage();
  await page.goto(LOGIN_URL, { waitUntil: 'domcontentloaded', timeout: 120_000 });

  const rl = readline.createInterface({ input, output });
  try {
    await rl.question(
      `Log into YouTube TV in the opened browser, navigate until the session is usable, then press Enter to save storage state to ${OUTPUT_PATH}.`,
    );
    await context.storageState({ path: OUTPUT_PATH });
    console.log(`Saved storage state to ${OUTPUT_PATH}`);
  } finally {
    rl.close();
    await browser.close();
  }
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});

