# Graph Report - vidrox  (2026-11-20)

## Corpus Check
- 81 files · ~36,531 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 18 file(s) not represented in the graph (top: .xml 8, (none) 4, .properties 2)

## Summary
- 899 nodes · 1644 edges · 48 communities (25 shown, 23 thin omitted)
- Extraction: 96% EXTRACTED · 4% INFERRED · 0% AMBIGUOUS · INFERRED: 67 edges (avg confidence: 0.86)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `5433e4fa`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- WebViewModel
- YoutubeWVWebViewSupport.kt
- raw/userscripts.js
- ScriptBridge
- DownloaderVM.kt
- capture.mjs
- assets/userscripts.js
- remote/MainActivity.kt
- .pageZoom
- WebViewClient
- Implementation Plan: VidroX for iPhone
- ai-analyze.mjs
- vidrox/MainActivity.kt
- managePerms.kt
- RemoteScreen
- ADR-001: Build an unsigned iOS package for Sideloadly
- VidroX
- Spec: VidroX for iPhone
- LayoutDebugBorders.kt
- YouTube TV fixture capture for filtering
- Android VidroX Remote Tasks
- RemoteCommand
- package-ipa.sh
- RemoteRelease.kt
- RemoteControlServer.kt
- ADR-002: Use NSD and a local TCP protocol for Android remote control
- RemoteDiscovery
- manageUpdate.kt
- RemoteAction
- RemoteControlServer
- Specification: VidroX Remote Protocol
- Specification: VidroX Remote for Android
- CAPABILITY-MAP-android-remote.md
- RegistrationListener
- RemoteProtocolException

## God Nodes (most connected - your core abstractions)
1. `WebViewModel` - 28 edges
2. `YoutubeWV()` - 27 edges
3. `RemoteAction` - 16 edges
4. `RemoteCommand` - 15 edges
5. `Coordinator` - 14 edges
6. `RemoteDiscovery` - 14 edges
7. `RemoteControlServer` - 13 edges
8. `ScriptBridge` - 13 edges
9. `RemoteKey` - 12 edges
10. `JavaScriptEvaluator` - 10 edges

## Surprising Connections (you probably didn't know these)
- `Behavior` --references--> `YoutubeWV()`  [INFERRED]
  SPEC-android-receiver.md → app/src/main/java/com/jetcar/vidrox/ui/screens/YoutubeWV.kt
- `YouTubeBrowser()` --calls--> `WebChromeClient`  [EXTRACTED]
  remote/src/main/java/com/jetcar/vidrox/remote/YouTubeBrowser.kt → app/src/main/java/com/jetcar/vidrox/utils/managePerms.kt
- `ControlButton()` --references--> `RemoteAction`  [EXTRACTED]
  remote/src/main/java/com/jetcar/vidrox/remote/MainActivity.kt → remote-core/src/main/kotlin/com/jetcar/vidrox/remote/core/RemoteProtocol.kt
- `YoutubeWV()` --calls--> `SplashLoading()`  [INFERRED]
  app/src/main/java/com/jetcar/vidrox/ui/screens/YoutubeWV.kt → app/src/main/java/com/jetcar/vidrox/ui/screens/SplashLoading.kt
- `YoutubeWV()` --calls--> `DirectionPadOverlay()`  [INFERRED]
  app/src/main/java/com/jetcar/vidrox/ui/screens/YoutubeWV.kt → app/src/main/java/com/jetcar/vidrox/ui/screens/YoutubeWVOverlays.kt

## Import Cycles
- None detected.

## Communities (48 total, 23 thin omitted)

### Community 0 - "WebViewModel"
Cohesion: 0.05
Nodes (23): .body, ButtonStyle, .remoteControl, DirectionPadView, .body, RemoteControlButtonStyle, LoadingOverlay, .body (+15 more)

### Community 1 - "YoutubeWVWebViewSupport.kt"
Cohesion: 0.05
Nodes (22): WebViewRemoteDispatcher, YoutubeWV(), NetworkCallback, configureCookies(), configureFocusAndTouch(), configureScale(), configureWebSettings(), createLoggingWebViewClient() (+14 more)

### Community 2 - "raw/userscripts.js"
Cohesion: 0.07
Nodes (29): NetworkBridge, addMenuButton(), applyShortsHidingCSS(), captureAdRangesFromPlayerResponse(), collectDeclaredAdRanges(), customAction(), execute_once_dom_loaded(), filterAdsFromSectionList() (+21 more)

### Community 3 - "ScriptBridge"
Cohesion: 0.08
Nodes (8): ScriptBridge, ClientError, disallowedEndpoint, invalidResponse, invalidURL, unsuccessfulStatus, SponsorBlockClient, SponsorBlockClientTests

### Community 4 - "DownloaderVM.kt"
Cohesion: 0.06
Nodes (7): InvalidUpdateException, UpdateViewModel, fetchScripts(), UpdateInstallReceiver, RemoteUpdateInstallReceiver, InvalidRemoteUpdate, RemoteUpdateViewModel

### Community 6 - "capture.mjs"
Cohesion: 0.09
Nodes (30): playwright, AD_RESPONSE_KEYWORDS, buildCaptureTargets(), buildResponseFileName(), captureTarget(), DEFAULT_CAPTURES, isTextualContentType(), main() (+22 more)

### Community 8 - "assets/userscripts.js"
Cohesion: 0.12
Nodes (16): addMenuButton(), applyShortsHidingCSS(), customAction(), execute_once_dom_loaded(), getSearchBar(), getShelfTitleText(), installShortsHidingObserver(), isShortsEndpoint() (+8 more)

### Community 9 - "remote/MainActivity.kt"
Cohesion: 0.05
Nodes (10): UpdateAppScreen(), UpdateDialog(), YTButton(), SplashLoading(), DirectionPadOverlay(), NavigationButton(), OfflineOverlay(), RemoteUpdateDialog() (+2 more)

### Community 10 - ".pageZoom"
Cohesion: 0.06
Nodes (16): Combine, CoreGraphics, Foundation, ContentView, VidroXApp, .body, WebContentConfiguration, TelevisionWebView (+8 more)

### Community 12 - "Implementation Plan: VidroX for iPhone"
Cohesion: 0.07
Nodes (26): ExitBridge, Architecture Decisions, Architecture Decisions, Checkpoint: Complete, Checkpoint: End to End, Checkpoint: Foundation, Checkpoint: Receiver, Checkpoint: Web Integration (+18 more)

### Community 13 - "ai-analyze.mjs"
Cohesion: 0.08
Nodes (32): AD_CLASS_PATTERNS, AD_JSON_KEY_SET, AD_JSON_KEYS, AD_URL_KEYWORDS, buildPrompt(), callGitHubModels(), collectAdPayloadSignals(), __dirname (+24 more)

### Community 17 - "managePerms.kt"
Cohesion: 0.23
Nodes (3): hasPermission(), rememberWebChromeClient(), WebChromeClient

### Community 18 - "RemoteScreen"
Cohesion: 0.14
Nodes (10): ConnectionPanel(), ControlButton(), ControlsPanel(), DevicePickerDialog(), MainActivity, RemoteLayout(), RemoteScreen(), VidroXRemoteTheme() (+2 more)

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

### Community 36 - "RemoteRelease.kt"
Cohesion: 0.22
Nodes (8): compareVersions(), fetchRemoteRelease(), findRemoteApkAsset(), getRemoteUpdate(), localVersion(), remoteAssetName(), RemoteRelease, RemoteReleaseTest

### Community 40 - "ADR-002: Use NSD and a local TCP protocol for Android remote control"
Cohesion: 0.17
Nodes (11): ADR-002: Use NSD and a local TCP protocol for Android remote control, Alternatives Considered, Cloud relay, Consequences, Context, Date, Decision, Google Cast SDK (+3 more)

### Community 42 - "RemoteDiscovery"
Cohesion: 0.22
Nodes (3): RemoteDiscovery, ResolveListener, ResolveRequest

### Community 43 - "manageUpdate.kt"
Cohesion: 0.17
Nodes (9): YoutubeVM, JavaScriptEvaluator, compareVersions(), fetchUpdate(), findApkAsset(), getLocalVersion(), getSkipVersion(), getUpdate() (+1 more)

### Community 48 - "RemoteAction"
Cohesion: 0.17
Nodes (11): RemoteAction, BACK, DOWN, LEFT, OPEN, PLAY_PAUSE, RIGHT, SEEK_BACK (+3 more)

### Community 50 - "Specification: VidroX Remote Protocol"
Cohesion: 0.33
Nodes (5): Contract, Goal, Security boundaries, Specification: VidroX Remote Protocol, Success criteria

### Community 51 - "Specification: VidroX Remote for Android"
Cohesion: 0.40
Nodes (4): Behavior, Goal, Specification: VidroX Remote for Android, Success criteria

## Knowledge Gaps
- **140 isolated node(s):** `CoreGraphics`, `.body`, `.body`, `invalidURL`, `disallowedEndpoint` (+135 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 291 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **23 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `WebViewModel` connect `WebViewModel` to `.pageZoom`?**
  _High betweenness centrality (0.159) - this node is a cross-community bridge._
- **Are the 5 inferred relationships involving `WebViewModel` (e.g. with `ContentView` and `.testCancelledNavigationDoesNotShowOfflineState()`) actually correct?**
  _`WebViewModel` has 5 INFERRED edges - model-reasoned connections that need verification._
- **What connects `CoreGraphics`, `.body`, `.body` to the rest of the system?**
  _140 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `WebViewModel` be split into smaller, more focused modules?**
  _Cohesion score 0.052313883299798795 - nodes in this community are weakly interconnected._
- **Why does `ExitBridge` connect `Implementation Plan: VidroX for iPhone` to `remote/MainActivity.kt`, `YoutubeWVWebViewSupport.kt`?**
  _High betweenness centrality (0.109) - this node is a cross-community bridge._
- **Are the 12 inferred relationships involving `YoutubeWV()` (e.g. with `SplashLoading()` and `DirectionPadOverlay()`) actually correct?**
  _`YoutubeWV()` has 12 INFERRED edges - model-reasoned connections that need verification._
- **Should `YoutubeWVWebViewSupport.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.05273937532002048 - nodes in this community are weakly interconnected._