> [!IMPORTANT]
><p> Looks like this project motivated <i> reisXd</i> to make their work public <a href="https://github.com/reisxd/TizenTubeCobalt">@reisxd/TizenTubeCobalt</a>. Please use <b>TizenTube-Cobalt</b> from the url above which supports everything  including voice search and fixes a lot of our curent issues. And don't forget to show them your love. ❤️ </p>

# VidroX
<p align="center">
  <img src='assets/VidroX_banner.png' alt="VidroX_banner_image">
</p>

<div align="center">This project owes its existence to <a href="https://github.com/reisxd/TizenTube">@reisxd/TizenTube</a>. </div>


## Features

* YouTube Leanback UI.
* Unlocks 4K resolutions.
* Adblock, SponsorBlock, DeArrow.
* **Shorts disabled by default** (can be re-enabled in settings).
* Multi-layer shorts blocking with aggressive filtering.
* Local userscripts (no external dependencies).

## Building from Source

### Prerequisites
- JDK 17 or higher
- Android SDK

### Build Instructions

**Debug APK:**
```bash
./gradlew assembleDebug
```

**Release APK:**
```bash
./gradlew assembleRelease
```

### Release Signing

Release builds now read signing data from either GitHub Actions secrets or your local `local.properties`.

GitHub Actions secrets required for signed releases:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

For local release builds, add these keys to your untracked `local.properties`:

- `androidKeystorePath`
- `androidKeystorePassword`
- `androidKeyAlias`
- `androidKeyPassword`

The generated APKs will be located at:

- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

### After Installation
To ensure shorts blocking works properly:
1. Go to **Settings → Apps → VidroX**
2. Select **Storage → Clear Data**
3. Restart the app

## How Shorts Blocking Works

This app uses a comprehensive multi-layer approach to block YouTube Shorts:

1. **JSON API Filtering** - Intercepts YouTube's API responses and removes shorts before rendering
2. **CSS Hiding** - Hides shorts elements using aggressive CSS selectors
3. **DOM Mutation Observer** - Continuously monitors and removes dynamically added shorts
4. **Periodic Cleanup** - Runs cleanup every 2 seconds to catch late-loading content

Shorts can be re-enabled through the in-app settings menu if desired.

## Maintaining filters with YouTube TV fixtures

This repo includes a maintainer-only GitHub Actions workflow for capturing signed-in YouTube TV pages and network fixtures.

Use it when YouTube changes renderer shapes or starts surfacing unwanted content in new ways.

- Workflow: `.github/workflows/capture-youtube-tv-fixtures.yml`
- Guide: `docs/shorts-fixtures.md`

The workflow is designed to help inspect real payloads and update `app/src/main/res/raw/userscripts.js` safely. It does not auto-rewrite filtering logic.

## Contributing

You can help by creating new issues or directly contributing to the development.<br>
For developers, please follow these guidelines:
1.  Fork the repository.
2.  Create a new branch for your feature or bug fix.
3.  Submit a pull request explaining your changes.
