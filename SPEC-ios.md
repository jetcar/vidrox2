# Spec: VidroX for iPhone

## Objective

Create an iPhone-native VidroX wrapper that preserves the existing YouTube TV experience and produces an unsigned `VidroX.ipa` suitable for signing and installation with Sideloadly on Windows.

The first release targets iOS 16 and later. It is distributed outside the App Store and does not require the developer to own a Mac.

## Tech Stack

- Swift 6 and SwiftUI.
- WebKit `WKWebView` for YouTube TV.
- `WKUserScript` with `.atDocumentStart` for the bundled userscript.
- `WKScriptMessageHandler` for the native SponsorBlock network bridge.
- `WKContentRuleList` for blocking known advertising hosts.
- XcodeGen for a reviewable, generated Xcode project.
- GitHub Actions macOS runner for simulator tests and unsigned device IPA packaging.

Official API references:

- https://developer.apple.com/documentation/webkit/wkuserscriptinjectiontime
- https://developer.apple.com/documentation/webkit/wkscriptmessagehandler
- https://developer.apple.com/documentation/webkit/wkcontentruleliststore
- https://docs.github.com/en/actions/how-tos/write-workflows/choose-where-workflows-run/choose-the-runner-for-a-job

## Commands

Run on macOS or the GitHub Actions macOS runner from the repository root:

```sh
brew install xcodegen
cd ios
xcodegen generate
xcodebuild -project VidroX.xcodeproj -scheme VidroX -sdk iphonesimulator -destination 'platform=iOS Simulator,name=iPhone 16 Pro' test
xcodebuild -project VidroX.xcodeproj -scheme VidroX -configuration Release -sdk iphoneos -destination 'generic/platform=iOS' CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO build
```

The packaging step copies `VidroX.app` into `Payload/` and creates `VidroX-unsigned.ipa` with `ditto`.

## Project Structure

```text
ios/
  project.yml                 XcodeGen project definition
  VidroX/
    App/                      SwiftUI entry point and app state
    Web/                      WKWebView, navigation, script bridge, content rules
    UI/                       loading, offline, and touch D-pad overlays
    Resources/                userscript, icons, and asset catalog
  VidroXTests/                unit tests for bridge validation and content rules
.github/workflows/
  ios-ipa.yml                 macOS build, test, and IPA artifact workflow
```

The Android project remains unchanged. The existing `userscripts.js` is copied into the iOS bundle by the build workflow so both platforms use the same script source.

## Code Style

Use small Swift types, explicit access control, structured concurrency, and `@MainActor` for UI/WebKit ownership.

```swift
@MainActor
final class WebViewModel: ObservableObject {
    @Published private(set) var isOffline = false

    func reload(_ webView: WKWebView) {
        isOffline = false
        webView.reload()
    }
}
```

Swift files use four-space indentation, `UpperCamelCase` types, `lowerCamelCase` members, and no force unwraps in production code.

## Testing Strategy

- Unit-test SponsorBlock URL validation and response filtering.
- Unit-test generated WebKit content-blocking JSON.
- Build and test on an iOS simulator in GitHub Actions.
- Build an unsigned `iphoneos` application and verify the IPA contains `Payload/VidroX.app/VidroX` and the bundled `userscripts.js`.
- Perform the final device check manually with Sideloadly because signing uses the user's Apple ID and physical iPhone.

## Boundaries

- Always: keep Android behavior unchanged; bundle the userscript locally; restrict the native network bridge to the SponsorBlock API; use HTTPS; avoid embedded credentials.
- Ask first: change the minimum iOS version, add third-party Swift packages, add Apple capabilities or entitlements, or switch from Sideloadly distribution.
- Never: commit Apple IDs, passwords, certificates, provisioning profiles, signing cookies, or device identifiers; bypass iOS code-signing checks; include App Store submission automation.

## Success Criteria

- The app opens `https://www.youtube.com/tv` in a full-screen `WKWebView` on iOS 16+.
- The existing userscript runs at document start.
- SponsorBlock requests work through a validated native bridge.
- Known advertising hosts are blocked using WebKit content rules.
- Loading, offline/retry, Escape/back, touch D-pad, and version display are available.
- GitHub Actions passes tests and publishes `VidroX-unsigned.ipa` as an artifact.
- Sideloadly can sign and install that IPA using a free Apple ID.
- No signing secret is required to build the artifact.

## Open Questions

None. The user approved iOS 16+, Windows/Sideloadly installation, retained WebView blocking features, touch D-pad, and free seven-day signing.
