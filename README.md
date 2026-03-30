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

## Contributing

You can help by creating new issues or directly contributing to the development.<br>
For developers, please follow these guidelines:
1.  Fork the repository.
2.  Create a new branch for your feature or bug fix.
3.  Submit a pull request explaining your changes.
