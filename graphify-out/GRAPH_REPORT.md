# Graph Report - vidrox  (2026-11-02)

## Corpus Check
- 59 files · ~28,985 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 15 file(s) not represented in the graph (top: .xml 5, (none) 4, .properties 2)

## Summary
- 636 nodes · 1072 edges · 35 communities (21 shown, 14 thin omitted)
- Extraction: 95% EXTRACTED · 5% INFERRED · 0% AMBIGUOUS · INFERRED: 51 edges (avg confidence: 0.86)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `dc29b884`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- WebViewModel
- YoutubeWVWebViewSupport.kt
- raw/userscripts.js
- ScriptBridge
- DownloaderVM.kt
- capture.mjs
- .body
- assets/userscripts.js
- Downloader.kt
- .pageZoom
- analyze.mjs
- Implementation Plan: VidroX for iPhone
- ai-analyze.mjs
- YoutubeWVOverlays.kt
- UpdateDialog.kt
- managePerms.kt
- .onCreate
- ADR-001: Build an unsigned iOS package for Sideloadly
- VidroX
- Spec: VidroX for iPhone
- LayoutDebugBorders.kt
- YouTube TV fixture capture for filtering
- VidroX for iPhone Tasks
- MainActivity.kt
- ReleaseData
- package-ipa.sh
- manageUpdate.kt

## God Nodes (most connected - your core abstractions)
1. `WebViewModel` - 29 edges
2. `YoutubeWV()` - 25 edges
3. `Coordinator` - 14 edges
4. `ScriptBridge` - 13 edges
5. `RemoteKey` - 11 edges
6. `WebViewClient` - 10 edges
7. `SponsorBlockHandler` - 10 edges
8. `SponsorBlockHandler` - 10 edges
9. `Spec: VidroX for iPhone` - 10 edges
10. `ReleaseData` - 9 edges

## Surprising Connections (you probably didn't know these)
- `YoutubeWV()` --calls--> `SplashLoading()`  [INFERRED]
  app/src/main/java/com/jetcar/vidrox/ui/screens/YoutubeWV.kt → app/src/main/java/com/jetcar/vidrox/ui/screens/SplashLoading.kt
- `YoutubeWV()` --calls--> `DirectionPadOverlay()`  [INFERRED]
  app/src/main/java/com/jetcar/vidrox/ui/screens/YoutubeWV.kt → app/src/main/java/com/jetcar/vidrox/ui/screens/YoutubeWVOverlays.kt
- `YoutubeWV()` --calls--> `OfflineOverlay()`  [INFERRED]
  app/src/main/java/com/jetcar/vidrox/ui/screens/YoutubeWV.kt → app/src/main/java/com/jetcar/vidrox/ui/screens/YoutubeWVOverlays.kt
- `YoutubeWV()` --calls--> `configureFocusAndTouch()`  [INFERRED]
  app/src/main/java/com/jetcar/vidrox/ui/screens/YoutubeWV.kt → app/src/main/java/com/jetcar/vidrox/ui/screens/YoutubeWVWebViewSupport.kt
- `YoutubeWV()` --calls--> `configureScale()`  [INFERRED]
  app/src/main/java/com/jetcar/vidrox/ui/screens/YoutubeWV.kt → app/src/main/java/com/jetcar/vidrox/ui/screens/YoutubeWVWebViewSupport.kt

## Import Cycles
- None detected.

## Communities (35 total, 14 thin omitted)

### Community 0 - "WebViewModel"
Cohesion: 0.06
Nodes (14): RemoteKey, down, escape, .event, left, right, select, up (+6 more)

### Community 1 - "YoutubeWVWebViewSupport.kt"
Cohesion: 0.08
Nodes (14): YoutubeWV(), NetworkCallback, configureCookies(), configureFocusAndTouch(), configureScale(), configureWebSettings(), createLoggingWebViewClient(), WebViewClient (+6 more)

### Community 2 - "raw/userscripts.js"
Cohesion: 0.08
Nodes (28): addMenuButton(), applyShortsHidingCSS(), captureAdRangesFromPlayerResponse(), collectDeclaredAdRanges(), customAction(), execute_once_dom_loaded(), filterAdsFromSectionList(), filterShortsFromSectionList() (+20 more)

### Community 3 - "ScriptBridge"
Cohesion: 0.08
Nodes (8): ScriptBridge, ClientError, disallowedEndpoint, invalidResponse, invalidURL, unsuccessfulStatus, SponsorBlockClient, SponsorBlockClientTests

### Community 4 - "DownloaderVM.kt"
Cohesion: 0.08
Nodes (3): InvalidUpdateException, UpdateViewModel, UpdateInstallReceiver

### Community 6 - "capture.mjs"
Cohesion: 0.07
Nodes (31): playwright, AD_RESPONSE_KEYWORDS, buildCaptureTargets(), buildResponseFileName(), captureTarget(), DEFAULT_CAPTURES, isTextualContentType(), main() (+23 more)

### Community 7 - ".body"
Cohesion: 0.09
Nodes (15): ContentView, .body, VidroXApp, .body, ButtonStyle, .remoteControl, DirectionPadView, .body (+7 more)

### Community 8 - "assets/userscripts.js"
Cohesion: 0.12
Nodes (16): addMenuButton(), applyShortsHidingCSS(), customAction(), execute_once_dom_loaded(), getSearchBar(), getShelfTitleText(), installShortsHidingObserver(), isShortsEndpoint() (+8 more)

### Community 10 - ".pageZoom"
Cohesion: 0.08
Nodes (10): Combine, CoreGraphics, Foundation, WebContentConfiguration, ContentRuleTests, ContentViewLayoutTests, TelevisionWebViewTests, VidroX (+2 more)

### Community 11 - "analyze.mjs"
Cohesion: 0.23
Nodes (14): AD_CLASS_PATTERNS, AD_JSON_KEY_SET, AD_JSON_KEYS, AD_KEYWORDS, analyzeFixtures(), collectAdPayloadSignals(), extractExternalScriptUrls(), findAdNetworkEndpoints() (+6 more)

### Community 12 - "Implementation Plan: VidroX for iPhone"
Cohesion: 0.11
Nodes (15): ExitBridge, Architecture Decisions, Checkpoint: Complete, Checkpoint: Foundation, Checkpoint: Web Integration, Dependency Order, Implementation Plan: VidroX for iPhone, Open Questions (+7 more)

### Community 13 - "ai-analyze.mjs"
Cohesion: 0.17
Nodes (17): AD_CLASS_PATTERNS, AD_JSON_KEY_SET, AD_JSON_KEYS, AD_URL_KEYWORDS, buildPrompt(), callGitHubModels(), collectAdPayloadSignals(), __dirname (+9 more)

### Community 14 - "YoutubeWVOverlays.kt"
Cohesion: 0.18
Nodes (3): DirectionPadOverlay(), NavigationButton(), OfflineOverlay()

### Community 15 - "UpdateDialog.kt"
Cohesion: 0.17
Nodes (3): UpdateAppScreen(), UpdateDialog(), YTButton()

### Community 17 - "managePerms.kt"
Cohesion: 0.19
Nodes (3): hasPermission(), rememberWebChromeClient(), WebChromeClient

### Community 19 - "ADR-001: Build an unsigned iOS package for Sideloadly"
Cohesion: 0.17
Nodes (11): Ad Hoc signed IPA, ADR-001: Build an unsigned iOS package for Sideloadly, Alternatives Considered, Consequences, Context, Date, Decision, Local Xcode build (+3 more)

### Community 20 - "VidroX"
Cohesion: 0.17
Nodes (11): After Installation, Build Instructions, Building from Source, Contributing, Features, How Ad Blocking Works, How Shorts Blocking Works, iPhone build for Sideloadly (+3 more)

### Community 21 - "Spec: VidroX for iPhone"
Cohesion: 0.18
Nodes (10): Boundaries, Code Style, Commands, Objective, Open Questions, Project Structure, Spec: VidroX for iPhone, Success Criteria (+2 more)

### Community 23 - "YouTube TV fixture capture for filtering"
Cohesion: 0.22
Nodes (8): Create a storage state locally, Good maintenance workflow, How to use the artifacts, Recommended capture targets, Secret required, What the capture workflow does, Why this is safer than auto-rewriting filters, YouTube TV fixture capture for filtering

### Community 24 - "VidroX for iPhone Tasks"
Cohesion: 0.22
Nodes (8): Checkpoint: Complete, Checkpoint: Web Integration, Task 1: Add the iOS project and WebView shell, Task 2: Inject scripts and block advertising hosts, Task 3: Implement native JavaScript bridges, Task 4: Add iPhone controls and connection UI, Task 5: Build and publish the unsigned IPA, VidroX for iPhone Tasks

### Community 36 - "manageUpdate.kt"
Cohesion: 0.10
Nodes (10): fetchScripts(), JavaScriptEvaluator, compareVersions(), fetchUpdate(), findApkAsset(), getLocalVersion(), getSkipVersion(), getUpdate() (+2 more)

## Knowledge Gaps
- **93 isolated node(s):** `CoreGraphics`, `.body`, `.body`, `invalidURL`, `disallowedEndpoint` (+88 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 228 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **14 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `WebViewModel` connect `WebViewModel` to `.pageZoom`, `.body`?**
  _High betweenness centrality (0.205) - this node is a cross-community bridge._
- **Are the 4 inferred relationships involving `WebViewModel` (e.g. with `ContentView` and `.testCancelledNavigationDoesNotShowOfflineState()`) actually correct?**
  _`WebViewModel` has 4 INFERRED edges - model-reasoned connections that need verification._
- **What connects `CoreGraphics`, `.body`, `.body` to the rest of the system?**
  _93 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `WebViewModel` be split into smaller, more focused modules?**
  _Cohesion score 0.0649895178197065 - nodes in this community are weakly interconnected._
- **Why does `ExitBridge` connect `Implementation Plan: VidroX for iPhone` to `YoutubeWV.kt`, `YoutubeWVWebViewSupport.kt`?**
  _High betweenness centrality (0.140) - this node is a cross-community bridge._
- **Are the 11 inferred relationships involving `YoutubeWV()` (e.g. with `SplashLoading()` and `DirectionPadOverlay()`) actually correct?**
  _`YoutubeWV()` has 11 INFERRED edges - model-reasoned connections that need verification._
- **Should `YoutubeWVWebViewSupport.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.07777777777777778 - nodes in this community are weakly interconnected._