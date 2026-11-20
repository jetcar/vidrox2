# ADR-002: Use NSD and a paired local TCP protocol for Android remote control

## Status

Accepted

## Date

2026-11-20

## Context

VidroX needs YouTube-style phone-to-screen video sending and playback control. The receiver is the existing Android application; the controller must be a separate Android phone application. Both devices can be assumed to share a Wi-Fi network. No cloud backend or Google Cast receiver registration is available.

## Decision

Advertise the receiver with Android Network Service Discovery using `_vidrox._tcp.` and exchange one versioned text command per TCP connection. A random six-digit code generated for the receiver installation and shown on screen is required on every command. Protocol and validation code live in a shared JVM module.

The receiver exists only while the VidroX screen is active. It accepts a fixed command allowlist and canonical YouTube targets; it never accepts arbitrary JavaScript or arbitrary URLs.

## Alternatives Considered

### Google Cast SDK

- Familiar casting UX and media controls.
- Requires Cast receiver/application setup and introduces Google Play Services dependencies.
- Rejected because VidroX is the receiver and needs direct control without external registration.

### Cloud relay

- Works across different networks.
- Requires hosting, accounts, persistent identity, and Internet availability.
- Rejected because the requested same-room control does not justify a backend.

### WebSocket/HTTP server

- Browser tooling and richer persistent sessions.
- Adds protocol/server dependencies for a small fixed command set.
- Rejected in favor of a bounded standard-library TCP implementation.

## Consequences

- Receiver and remote must be on the same reachable LAN; guest Wi-Fi client isolation can prevent discovery or connection.
- Pairing prevents casual accidental control but does not encrypt traffic.
- SDK 36 receives implicit LAN access through `INTERNET`; targeting SDK 37 later requires declaring and requesting `ACCESS_LOCAL_NETWORK`.
- NSD registration and discovery follow the resumed application lifecycle and use a scoped Wi-Fi multicast lock for Android versions where the system does not manage mDNS reception.

## References

- https://developer.android.com/develop/connectivity/wifi/use-nsd
- https://developer.android.com/privacy-and-security/local-network-permission
- https://developer.android.com/reference/android/webkit/WebView#evaluateJavascript(java.lang.String,%20android.webkit.ValueCallback%3Cjava.lang.String%3E)
