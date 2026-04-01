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

## How Shorts Blocking Works

A multi-layer approach is used to block YouTube Shorts:

1. **JSON API Filtering** — Intercepts YouTube's API responses and removes shorts before rendering
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
