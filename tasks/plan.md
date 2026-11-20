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

---

# Implementation Plan: Android VidroX Remote

## Overview

Add a local-network receiver to the existing Android VidroX app and a separate Android phone remote. Share a strict command protocol, discover and select receivers with NSD without a pairing code, and produce two versioned APKs.

## Architecture Decisions

- Use `_vidrox._tcp.` DNS-SD discovery and a bounded TCP line protocol.
- Keep protocol parsing and YouTube target validation in `remote-core`.
- Keep the receiver lifecycle scoped to the active VidroX screen.
- Keep the controller as an independent application ID and APK.

See `docs/decisions/002-android-local-remote.md`.

## Task List

### Phase 1: Foundation

- [x] Task A1: Add the shared protocol module and tests.
- [x] Task A2: Add receiver server lifecycle and command dispatch.

### Checkpoint: Receiver

- [x] Protocol tests pass.
- [x] Existing app builds with the receiver active.

### Phase 2: Controller

- [x] Task A3: Add NSD discovery and TCP command client.
- [x] Task A4: Add the touch remote UI and validation.

### Checkpoint: End to End

- [x] Both apps build.
- [ ] Manual same-LAN command flow is ready for device verification.

### Phase 3: Distribution

- [x] Task A5: Produce versioned installable APKs in one output folder.
- [x] Task A6: Update usage and build documentation.

## Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Router isolates Wi-Fi clients | High | Show clear discovery/connect errors; document same non-guest network requirement. |
| YouTube DOM/player changes | Medium | Use generic HTML video controls and existing keyboard navigation. |
| Unauthorized LAN control | Medium | Require a per-session random code and validate every command. |
| NSD API differences | Medium | Use the API 24-compatible discovery/resolve path. |

## Open Questions

None; implementation follows the user-approved separate Android remote direction.
