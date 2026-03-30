# YouTube TV fixture capture for filtering

This repo already filters unwanted content in `app/src/main/res/raw/userscripts.js` using multiple layers:

- JSON-level filtering via the `JSON.parse` hook
- renderer checks such as `TVHTML5_SHELF_RENDERER_TYPE_SHORTS`
- endpoint checks such as `reelWatchEndpoint`
- CSS hiding fallback
- DOM cleanup via `MutationObserver`

A live YouTube TV capture helps when those response shapes change.

## What the capture workflow does

The manual GitHub Actions workflow `capture-youtube-tv-fixtures.yml`:

1. restores a signed-in Playwright browser session from a secret
2. opens configured YouTube TV pages
3. saves:
   - rendered HTML
   - screenshots
   - captured JSON responses
   - a HAR file
   - a manifest describing each page

It does **not** modify filter code automatically.

## Why this is safer than auto-rewriting filters

YouTube TV changes often, and the current logic is intentionally conservative. A workflow should help you inspect new payloads, not blindly rewrite `userscripts.js`.

Review captured fixtures, then update filters manually.

## Secret required

Create a repository or environment secret:

- `YOUTUBE_TV_STORAGE_STATE_BASE64`

This should contain a base64-encoded Playwright `storageState` JSON file for a throwaway or maintainer-owned YouTube account.

## Create a storage state locally

From the repo root:

```powershell
cd C:\repo\NoTubeTV\tools\youtube-tv-capture
npm install
npm run save-storage-state -- .\youtube-tv.state.json
```

Log into `https://www.youtube.com/tv` in the opened browser, then press Enter in the terminal.

Base64-encode the saved file for GitHub Secrets:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes('C:\repo\NoTubeTV\tools\youtube-tv-capture\youtube-tv.state.json')) | Set-Content -NoNewline 'C:\repo\NoTubeTV\tools\youtube-tv-capture\youtube-tv.state.base64.txt'
```

Copy the contents of `youtube-tv.state.base64.txt` into `YOUTUBE_TV_STORAGE_STATE_BASE64`.

## Recommended capture targets

Typical workflow inputs:

- `home_url`: `https://www.youtube.com/tv`
- `browse_url`: a signed-in browse/list route you care about
- `details_url`: a details/watch page that shows problematic content nearby

If YouTube TV uses hash routes for your session, pass the exact URLs you want from a real browser session.

## How to use the artifacts

After a capture run, inspect:

- `manifest.json`
- `home/`, `browse/`, `details/`
- `network/*.json`
- `session.har`

Look for:

- shelves marked `TVHTML5_SHELF_RENDERER_TYPE_SHORTS`
- any item containing `reelWatchEndpoint`
- new renderer names used for shorts-like content
- non-shorts shelves that your current CSS selectors may accidentally hide

Then update `app/src/main/res/raw/userscripts.js` accordingly.

## Good maintenance workflow

1. Run the manual capture workflow when YouTube TV behavior changes.
2. Compare new payloads against existing assumptions in `userscripts.js`.
3. Make a small targeted filter change.
4. Test on a real TV/device.
5. Re-run capture later if YouTube changes again.

