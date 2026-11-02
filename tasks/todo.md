# VidroX for iPhone Tasks

## Task 1: Add the iOS project and WebView shell

**Description:** Define the XcodeGen project, SwiftUI app entry point, and a minimal `WKWebView` that loads YouTube TV.

**Acceptance criteria:**
- [ ] The project targets iPhone on iOS 16+ with bundle ID `com.jetcar.vidrox`.
- [ ] The application loads `https://www.youtube.com/tv` in a full-screen WebView.
- [ ] No signing identity is required for a simulator build.

**Verification:**
- [ ] `xcodegen generate`
- [ ] `xcodebuild -project VidroX.xcodeproj -scheme VidroX -sdk iphonesimulator build`

**Dependencies:** None

**Files likely touched:**
- `ios/project.yml`
- `ios/VidroX/App/VidroXApp.swift`
- `ios/VidroX/App/ContentView.swift`
- `ios/VidroX/Web/YouTubeWebView.swift`

**Estimated scope:** Medium

## Task 2: Inject scripts and block advertising hosts

**Description:** Bundle the shared userscript, inject it at document start, and apply a compiled WebKit content-rule list for known advertising hosts.

**Acceptance criteria:**
- [ ] The Android `userscripts.js` is copied into the iOS application resources during the build.
- [ ] The script is registered with `.atDocumentStart` before the first navigation.
- [ ] Content rules block the same known advertising host families as Android.

**Verification:**
- [ ] Unit tests validate generated content-rule JSON.
- [ ] Simulator build contains `userscripts.js`.

**Dependencies:** Task 1

**Files likely touched:**
- `ios/project.yml`
- `ios/VidroX/Web/WebContentConfiguration.swift`
- `ios/VidroXTests/ContentRuleTests.swift`
- `ios/VidroX/Resources/ios_bridge.js`

**Estimated scope:** Medium

## Task 3: Implement native JavaScript bridges

**Description:** Provide iOS-compatible exit and SponsorBlock network bridges while retaining the JavaScript API expected by the shared userscript.

**Acceptance criteria:**
- [ ] `NetworkBridge.fetch(url, videoId)` works from the shared userscript.
- [ ] Only HTTPS requests to the SponsorBlock API are allowed.
- [ ] The response is filtered to the requested video and delivered through `window.onNetworkBridgeResponse`.

**Verification:**
- [ ] Unit tests reject non-HTTPS and non-SponsorBlock URLs.
- [ ] Unit tests cover matching, missing, and malformed SponsorBlock responses.

**Dependencies:** Task 2

**Files likely touched:**
- `ios/VidroX/Web/ScriptBridge.swift`
- `ios/VidroX/Web/SponsorBlockClient.swift`
- `ios/VidroX/Resources/ios_bridge.js`
- `ios/VidroXTests/SponsorBlockClientTests.swift`

**Estimated scope:** Medium

## Checkpoint: Web Integration

- [ ] All unit tests pass.
- [ ] WebView configuration loads both scripts and the content rule list.
- [ ] No unrestricted native network interface is exposed.

## Task 4: Add iPhone controls and connection UI

**Description:** Add progress, offline/retry, Escape, touch D-pad, auto-hide, and version overlays around the WebView.

**Acceptance criteria:**
- [ ] Touch controls dispatch keyboard events understood by YouTube TV.
- [ ] Controls auto-hide and reappear after interaction.
- [ ] Load failures show a retry action without destroying the WebView.

**Verification:**
- [ ] Simulator build succeeds.
- [ ] UI state tests cover progress and offline transitions where practical.

**Dependencies:** Task 3

**Files likely touched:**
- `ios/VidroX/App/ContentView.swift`
- `ios/VidroX/Web/YouTubeWebView.swift`
- `ios/VidroX/UI/DirectionPadView.swift`
- `ios/VidroX/UI/StatusOverlay.swift`

**Estimated scope:** Medium

## Task 5: Build and publish the unsigned IPA

**Description:** Add a GitHub Actions workflow that generates the project, runs tests, builds for `iphoneos` without signing, packages the app, validates the archive, and uploads it.

**Acceptance criteria:**
- [ ] Manual workflow runs on a stable macOS image.
- [ ] The artifact is named `VidroX-unsigned.ipa`.
- [ ] The IPA contains the executable and both JavaScript resources under `Payload/VidroX.app`.

**Verification:**
- [ ] GitHub Actions simulator tests pass.
- [ ] `unzip -l VidroX-unsigned.ipa` shows the expected files.
- [ ] Sideloadly installation steps are documented in the README.

**Dependencies:** Task 4

**Files likely touched:**
- `.github/workflows/ios-ipa.yml`
- `README.md`
- `ios/scripts/package-ipa.sh`

**Estimated scope:** Medium

## Checkpoint: Complete

- [ ] Every success criterion in `SPEC-ios.md` is satisfied.
- [ ] Android build remains unchanged.
- [ ] The generated IPA is ready for the user's Sideloadly device test.
