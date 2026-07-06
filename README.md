# VidroX

<p align="center">
  <img src='assets/VidroX_banner.png' alt="VidroX_banner_image">
</p>

<div align="center">
  An Android WebView wrapper for YouTube TV with adblock, SponsorBlock, and Shorts blocking.<br>
  Inspired by <a href="https://github.com/reisxd/TizenTube">@reisxd/TizenTube</a>.
</div>

---

## Features

* YouTube Leanback UI on Android TV and tablets.
* Unlocks 4K resolutions.
* Adblock, SponsorBlock, DeArrow.
* **Shorts disabled by default** (can be re-enabled in settings).
* Multi-layer shorts blocking with aggressive filtering.
* Local userscripts (no external dependencies).
* Auto-update with countdown via GitHub Releases.
* D-pad overlay for tablet/touch use.

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

Release builds read signing data from either GitHub Actions secrets or your local `local.properties`.

**GitHub Actions secrets required for signed releases:**

| Secret | Description |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | Base64-encoded keystore file |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password |
| `ANDROID_KEY_ALIAS` | Key alias |
| `ANDROID_KEY_PASSWORD` | Key password |

**For local release builds**, add to your untracked `local.properties`:

```properties
androidKeystorePath=/path/to/keystore.jks
androidKeystorePassword=...
androidKeyAlias=...
androidKeyPassword=...
```

The generated APKs will be at:

- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

## After Installation

To ensure shorts blocking works properly:
1. Go to **Settings → Apps → VidroX**
2. Select **Storage → Clear Data**
3. Restart the app

## How Ad Blocking Works

Ads are removed with four cooperating layers:

1. **Document-Start Injection** — The userscript is registered via
   `WebViewCompat.addDocumentStartJavaScript`, so its hooks are installed
   *before* any YouTube code runs (older WebViews fall back to injection after
   page load). This closes the race where YouTube's bootstrap requests were
   parsed before the ad filter existed.
2. **JSON API Scrubbing** — Every path YouTube uses to parse API responses is
   hooked (`JSON.parse`, `Response.json`, the XHR `response` getter, and the
   bundle's captured `window._yttv[*].JSON` references), and known ad payloads
   are stripped before they reach the player.
3. **Network-Level Blocking** — Requests to known ad-serving domains are
   dropped in `shouldInterceptRequest`.
4. **Playback Watchdog** — Ads that survive scrubbing (e.g. server-stitched
   ones) are skipped during playback: visible skip-ad buttons are auto-clicked,
   and declared ad time ranges are seeked past while ad UI is on screen.

## How Shorts Blocking Works

A multi-layer approach is used to block YouTube Shorts:

1. **JSON API Filtering** — Intercepts YouTube's API responses and removes shorts/reels before rendering, on every surface: home, search, subscription and channel tabs, the related-videos shelf, paged-in continuations, and the reel player queue
2. **CSS Hiding** — Hides shorts elements using aggressive CSS selectors
3. **DOM Mutation Observer** — Continuously monitors and removes dynamically added shorts
4. **Periodic Cleanup** — Runs cleanup every 2 seconds to catch late-loading content

Shorts can be re-enabled through the in-app settings menu.

## Contributing

Issues and pull requests are welcome.
Merging commits to the default branch automatically triggers the GitHub release workflow.

1. Fork the repository.
2. Create a new branch for your feature or bug fix.
3. Submit a pull request describing your changes.
