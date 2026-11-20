# Graph Report - vidrox  (2026-11-20)

## Corpus Check
- 80 files · ~35,651 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 18 file(s) not represented in the graph (top: .xml 8, (none) 4, .properties 2)

## Summary
- 876 nodes · 1568 edges · 57 communities (32 shown, 25 thin omitted)
- Extraction: 96% EXTRACTED · 4% INFERRED · 0% AMBIGUOUS · INFERRED: 65 edges (avg confidence: 0.86)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `981eed08`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- WebViewModel
- YoutubeWVWebViewSupport.kt
- raw/userscripts.js
- SponsorBlockClient
- DownloaderVM.kt
- Coordinator
- capture.mjs
- View
- assets/userscripts.js
- SplashLoading.kt
- Foundation
- ScriptBridge
- Implementation Plan: VidroX for iPhone
- ai-analyze.mjs
- YoutubeWVOverlays.kt
- UpdateDialog.kt
- managePerms.kt
- .send
- ADR-001: Build an unsigned iOS package for Sideloadly
- VidroX
- Spec: VidroX for iPhone
- LayoutDebugBorders.kt
- YouTube TV fixture capture for filtering
- Android VidroX Remote Tasks
- RemoteCommand
- ReleaseData
- Theme.kt
- package-ipa.sh
- vidrox/MainActivity.kt
- RemoteRelease.kt
- RemoteKey
- ContentView
- ADR-002: Use NSD and a paired local TCP protocol for Android remote control
- .body
- RemoteDiscovery
- manageUpdate.kt
- RemoteProtocolException
- RemoteAction
- RemoteControlServer
- Specification: VidroX Remote Protocol
- Specification: VidroX Remote for Android
- CAPABILITY-MAP-android-remote.md
- RegistrationListener
- .addUserScript
- .pageZoom
- ClientError
- XCTestCase

## God Nodes (most connected - your core abstractions)
1. `YoutubeWV()` - 28 edges
2. `WebViewModel` - 28 edges
3. `RemoteAction` - 16 edges
4. `RemoteControlServer` - 14 edges
5. `Coordinator` - 14 edges
6. `RemoteDiscovery` - 14 edges
7. `ScriptBridge` - 13 edges
8. `RemoteCommand` - 13 edges
9. `RemoteKey` - 12 edges
10. `WebViewClient` - 10 edges

## Surprising Connections (you probably didn't know these)
- `Behavior` --references--> `YoutubeWV()`  [INFERRED]
  SPEC-android-receiver.md → app/src/main/java/com/jetcar/vidrox/ui/screens/YoutubeWV.kt
- `ControlButton()` --references--> `RemoteAction`  [EXTRACTED]
  remote/src/main/java/com/jetcar/vidrox/remote/MainActivity.kt → remote-core/src/main/kotlin/com/jetcar/vidrox/remote/core/RemoteProtocol.kt
- `YoutubeWV()` --calls--> `SplashLoading()`  [INFERRED]
  app/src/main/java/com/jetcar/vidrox/ui/screens/YoutubeWV.kt → app/src/main/java/com/jetcar/vidrox/ui/screens/SplashLoading.kt
- `YoutubeWV()` --calls--> `DirectionPadOverlay()`  [INFERRED]
  app/src/main/java/com/jetcar/vidrox/ui/screens/YoutubeWV.kt → app/src/main/java/com/jetcar/vidrox/ui/screens/YoutubeWVOverlays.kt
- `YoutubeWV()` --calls--> `OfflineOverlay()`  [INFERRED]
  app/src/main/java/com/jetcar/vidrox/ui/screens/YoutubeWV.kt → app/src/main/java/com/jetcar/vidrox/ui/screens/YoutubeWVOverlays.kt

## Import Cycles
- None detected.

## Communities (57 total, 25 thin omitted)

### Community 0 - "WebViewModel"
Cohesion: 0.17
Nodes (3): WebViewModel, .versionLabel, WebViewModelTests

### Community 1 - "YoutubeWVWebViewSupport.kt"
Cohesion: 0.06
Nodes (21): WebViewRemoteDispatcher, YoutubeWV(), NetworkCallback, configureCookies(), configureFocusAndTouch(), configureScale(), configureWebSettings(), createLoggingWebViewClient() (+13 more)

### Community 2 - "raw/userscripts.js"
Cohesion: 0.08
Nodes (28): addMenuButton(), applyShortsHidingCSS(), captureAdRangesFromPlayerResponse(), collectDeclaredAdRanges(), customAction(), execute_once_dom_loaded(), filterAdsFromSectionList(), filterShortsFromSectionList() (+20 more)

### Community 3 - "SponsorBlockClient"
Cohesion: 0.19
Nodes (3): unsuccessfulStatus, SponsorBlockClient, SponsorBlockClientTests

### Community 4 - "DownloaderVM.kt"
Cohesion: 0.06
Nodes (7): InvalidUpdateException, UpdateViewModel, fetchScripts(), readRaw(), UpdateInstallReceiver, RemoteUpdateInstallReceiver, InvalidRemoteUpdate

### Community 6 - "capture.mjs"
Cohesion: 0.06
Nodes (45): playwright, AD_CLASS_PATTERNS, AD_JSON_KEY_SET, AD_JSON_KEYS, AD_KEYWORDS, analyzeFixtures(), collectAdPayloadSignals(), extractExternalScriptUrls() (+37 more)

### Community 7 - "View"
Cohesion: 0.24
Nodes (5): ButtonStyle, .remoteControl, DirectionPadView, .body, RemoteControlButtonStyle

### Community 8 - "assets/userscripts.js"
Cohesion: 0.08
Nodes (19): ExitBridge, NetworkBridge, addMenuButton(), applyShortsHidingCSS(), customAction(), execute_once_dom_loaded(), getSearchBar(), getShelfTitleText() (+11 more)

### Community 10 - "Foundation"
Cohesion: 0.23
Nodes (6): Combine, CoreGraphics, Foundation, VidroX, WebKit, XCTest

### Community 12 - "Implementation Plan: VidroX for iPhone"
Cohesion: 0.08
Nodes (24): Architecture Decisions, Checkpoint: Complete, Checkpoint: End to End, Checkpoint: Foundation, Checkpoint: Receiver, Checkpoint: Web Integration, Dependency Order, Implementation Plan: Android VidroX Remote (+16 more)

### Community 13 - "ai-analyze.mjs"
Cohesion: 0.17
Nodes (17): AD_CLASS_PATTERNS, AD_JSON_KEY_SET, AD_JSON_KEYS, AD_URL_KEYWORDS, buildPrompt(), callGitHubModels(), collectAdPayloadSignals(), __dirname (+9 more)

### Community 14 - "YoutubeWVOverlays.kt"
Cohesion: 0.20
Nodes (3): DirectionPadOverlay(), NavigationButton(), OfflineOverlay()

### Community 17 - "managePerms.kt"
Cohesion: 0.21
Nodes (3): hasPermission(), rememberWebChromeClient(), WebChromeClient

### Community 18 - ".send"
Cohesion: 0.15
Nodes (9): ConnectionPanel(), ControlButton(), ControlsPanel(), MainActivity, RemoteLayout(), RemoteScreen(), VidroXRemoteTheme(), RemoteCommandClient (+1 more)

### Community 19 - "ADR-001: Build an unsigned iOS package for Sideloadly"
Cohesion: 0.17
Nodes (11): Ad Hoc signed IPA, ADR-001: Build an unsigned iOS package for Sideloadly, Alternatives Considered, Consequences, Context, Date, Decision, Local Xcode build (+3 more)

### Community 20 - "VidroX"
Cohesion: 0.17
Nodes (11): After Installation, Android phone remote, Build Instructions, Building from Source, Contributing, Features, How Shorts Blocking Works, iPhone build for Sideloadly (+3 more)

### Community 21 - "Spec: VidroX for iPhone"
Cohesion: 0.18
Nodes (10): Boundaries, Code Style, Commands, Objective, Open Questions, Project Structure, Spec: VidroX for iPhone, Success Criteria (+2 more)

### Community 23 - "YouTube TV fixture capture for filtering"
Cohesion: 0.22
Nodes (8): Create a storage state locally, Good maintenance workflow, How to use the artifacts, Recommended capture targets, Secret required, What the capture workflow does, Why this is safer than auto-rewriting filters, YouTube TV fixture capture for filtering

### Community 24 - "Android VidroX Remote Tasks"
Cohesion: 0.12
Nodes (16): Android VidroX Remote Tasks, Checkpoint: Complete, Checkpoint: End to End, Checkpoint: Web Integration, Task 1: Add the iOS project and WebView shell, Task 2: Inject scripts and block advertising hosts, Task 3: Implement native JavaScript bridges, Task 4: Add iPhone controls and connection UI (+8 more)

### Community 25 - "RemoteCommand"
Cohesion: 0.24
Nodes (3): RemoteCommand, RemoteProtocol, RemoteProtocolTest

### Community 26 - "ReleaseData"
Cohesion: 0.21
Nodes (5): UpdateAppScreen(), UpdateDialog(), YTButton(), YoutubeVM, ReleaseData

### Community 36 - "RemoteRelease.kt"
Cohesion: 0.15
Nodes (9): compareVersions(), fetchRemoteRelease(), findRemoteApkAsset(), getRemoteUpdate(), localVersion(), remoteAssetName(), RemoteRelease, RemoteUpdateViewModel (+1 more)

### Community 37 - "RemoteKey"
Cohesion: 0.18
Nodes (8): RemoteKey, down, escape, .event, left, right, select, up

### Community 38 - "ContentView"
Cohesion: 0.20
Nodes (5): ContentView, VidroXApp, .body, SwiftUI, UIKit

### Community 40 - "ADR-002: Use NSD and a paired local TCP protocol for Android remote control"
Cohesion: 0.17
Nodes (11): ADR-002: Use NSD and a paired local TCP protocol for Android remote control, Alternatives Considered, Cloud relay, Consequences, Context, Date, Decision, Google Cast SDK (+3 more)

### Community 41 - ".body"
Cohesion: 0.24
Nodes (5): .body, LoadingOverlay, .body, OfflineOverlay, .body

### Community 42 - "RemoteDiscovery"
Cohesion: 0.22
Nodes (3): RemoteDiscovery, ResolveListener, ResolveRequest

### Community 43 - "manageUpdate.kt"
Cohesion: 0.26
Nodes (7): JavaScriptEvaluator, compareVersions(), fetchUpdate(), findApkAsset(), getLocalVersion(), getSkipVersion(), getUpdate()

### Community 48 - "RemoteAction"
Cohesion: 0.17
Nodes (11): RemoteAction, BACK, DOWN, LEFT, OPEN, PLAY_PAUSE, RIGHT, SEEK_BACK (+3 more)

### Community 50 - "Specification: VidroX Remote Protocol"
Cohesion: 0.33
Nodes (5): Contract, Goal, Security boundaries, Specification: VidroX Remote Protocol, Success criteria

### Community 51 - "Specification: VidroX Remote for Android"
Cohesion: 0.40
Nodes (4): Behavior, Goal, Specification: VidroX Remote for Android, Success criteria

### Community 59 - "ClientError"
Cohesion: 0.29
Nodes (4): ClientError, disallowedEndpoint, invalidResponse, invalidURL

## Knowledge Gaps
- **140 isolated node(s):** `CoreGraphics`, `.body`, `.body`, `invalidURL`, `disallowedEndpoint` (+135 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 292 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **25 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `WebViewModel` connect `WebViewModel` to `Coordinator`, `RemoteKey`, `ContentView`, `View`, `.body`, `Foundation`?**
  _High betweenness centrality (0.163) - this node is a cross-community bridge._
- **Are the 12 inferred relationships involving `YoutubeWV()` (e.g. with `SplashLoading()` and `DirectionPadOverlay()`) actually correct?**
  _`YoutubeWV()` has 12 INFERRED edges - model-reasoned connections that need verification._
- **What connects `CoreGraphics`, `.body`, `.body` to the rest of the system?**
  _140 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `YoutubeWVWebViewSupport.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.05669199298655757 - nodes in this community are weakly interconnected._
- **Why does `ExitBridge` connect `assets/userscripts.js` to `remote/MainActivity.kt`, `YoutubeWVWebViewSupport.kt`?**
  _High betweenness centrality (0.111) - this node is a cross-community bridge._
- **Are the 5 inferred relationships involving `WebViewModel` (e.g. with `ContentView` and `.testCancelledNavigationDoesNotShowOfflineState()`) actually correct?**
  _`WebViewModel` has 5 INFERRED edges - model-reasoned connections that need verification._
- **Should `raw/userscripts.js` be split into smaller, more focused modules?**
  _Cohesion score 0.08080808080808081 - nodes in this community are weakly interconnected._