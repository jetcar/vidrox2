# ADR-001: Build an unsigned iOS package for Sideloadly

## Status

Accepted

## Date

2026-11-02

## Context

VidroX currently targets Android. The user needs an iPhone version but does not own a Mac and wants to install the package directly with Sideloadly on Windows. Apple device packages normally require signing tied to a developer identity and provisioning profile.

## Decision

Build the iPhone application on a GitHub-hosted macOS runner with code signing disabled. Package the resulting device `.app` as `Payload/VidroX.app` inside `VidroX-unsigned.ipa`. Sideloadly will apply the user's Apple ID signing identity and install the application.

The repository will not store Apple credentials, signing certificates, provisioning profiles, or device identifiers.

## Alternatives Considered

### Ad Hoc signed IPA

- Allows installation on registered devices without Sideloadly.
- Requires a paid Apple Developer membership, a distribution certificate, and each device UDID.
- Rejected because those assets are unavailable and the user selected Sideloadly.

### TestFlight

- Provides managed beta installation and updates.
- Requires Apple Developer Program membership and App Store Connect setup.
- Rejected for the initial direct-install requirement.

### Local Xcode build

- Uses Apple's standard local development workflow.
- Requires macOS hardware.
- Rejected because the user does not own a Mac.

## Consequences

- GitHub Actions is the authoritative iOS build environment.
- The IPA is not directly installable until Sideloadly signs it.
- A free Apple account normally requires refreshing the installation every seven days.
- Final physical-device verification remains a user-run acceptance step.

## References

- https://developer.apple.com/help/account/basics/about-your-developer-account
- https://developer.apple.com/documentation/xcode/distributing-your-app-to-registered-devices
- https://sideloadly.io/faq
