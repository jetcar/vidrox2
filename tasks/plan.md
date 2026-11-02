# Implementation Plan: VidroX for iPhone

## Overview

Add a native SwiftUI/WebKit iPhone application beside the existing Android application. A macOS GitHub Actions runner generates the Xcode project, runs simulator tests, builds without code signing, packages `VidroX-unsigned.ipa`, and uploads it for installation through Sideloadly.

## Architecture Decisions

- Keep Android and iOS as separate native shells while sharing the existing bundled `userscripts.js` source.
- Use XcodeGen so the Xcode project is reproducible and can be generated without committing opaque project metadata.
- Inject the main userscript with `WKUserScript` at document start.
- Add a small iOS compatibility script that exposes the Android-style `NetworkBridge.fetch` and `ExitBridge.onExitCalled` objects expected by the shared userscript.
- Restrict native HTTP requests to the SponsorBlock HTTPS API and return results through the existing JavaScript callback.
- Replace Android request interception with `WKContentRuleList` ad-host rules.
- Build an unsigned device app and let Sideloadly perform user-specific signing.

The distribution decision is recorded in `docs/decisions/001-ios-unsigned-ipa.md`.

## Dependency Order

```text
XcodeGen project
    -> WKWebView shell
        -> userscript and content rules
            -> native script bridges
                -> iPhone controls and offline UI
                    -> CI IPA packaging
```

## Task List

### Phase 1: Build Foundation

- [x] Task 1: Add the generated iOS project and a minimal WKWebView shell.

### Checkpoint: Foundation

- [x] XcodeGen creates the project on macOS.
- [x] The simulator build opens the YouTube TV URL.

### Phase 2: Blocking and Native Integration

- [x] Task 2: Inject shared scripts and compile WebKit ad-host rules.
- [x] Task 3: Implement the validated SponsorBlock and exit bridges.

### Checkpoint: Web Integration

- [x] Unit tests cover content rules, URL validation, and SponsorBlock filtering.
- [x] The shared userscript can call both compatibility bridge objects.

### Phase 3: iPhone UX and Distribution

- [x] Task 4: Add loading, offline/retry, Escape, touch D-pad, and version UI.
- [x] Task 5: Add macOS CI tests and unsigned IPA artifact packaging.

### Checkpoint: Complete

- [x] Simulator tests pass.
- [x] Unsigned device build succeeds.
- [x] IPA structure and bundled resources are verified.
- [x] Installation instructions cover Sideloadly and seven-day refresh.

## Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Windows cannot run Xcode | High | Make GitHub Actions macOS the authoritative build and test environment. |
| YouTube changes its web application | High | Reuse the current script and keep iOS compatibility code isolated. |
| WKWebView differs from Android WebView | High | Use document-start injection, WebKit content rules, focused unit tests, and a physical-device acceptance step. |
| Native bridge becomes an open network proxy | High | Accept only HTTPS SponsorBlock API URLs and reject all other hosts. |
| Sideloadly rejects the package structure | Medium | Package exactly `Payload/VidroX.app`, verify contents in CI, and leave final signing to Sideloadly. |
| GitHub runner image changes | Medium | Pin a stable macOS runner and an explicit XcodeGen release. |

## Verification Strategy

- Focused unit tests after Tasks 2 and 3.
- Simulator build after Tasks 1 and 4.
- Full test, unsigned device build, and IPA inspection after Task 5.
- Final manual installation with Sideloadly on the user's Windows machine and iPhone.

## Open Questions

None.
