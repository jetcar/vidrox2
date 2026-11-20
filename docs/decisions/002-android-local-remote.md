# ADR-002: Use NSD and a local TCP protocol for Android remote control

## Status

Accepted

## Date

2026-11-20

## Context

VidroX needs YouTube-style phone-to-screen video sending and playback control. The receiver is the existing Android application; the controller must be a separate Android phone application. Both devices can be assumed to share a Wi-Fi network. No cloud backend or Google Cast receiver registration is available.

## Decision

Advertise the receiver with Android Network Service Discovery using `_vidrox._tcp.` and exchange one versioned text command per TCP connection. The phone sends commands directly after the user selects a discovered receiver; no pairing code or account is required. Protocol and validation code live in a shared JVM module. No-code commands use `VIDROX/2`; the receiver also accepts legacy `VIDROX/1` commands, while an updated phone reports that an older receiver must be updated. The legacy pairing field remains in the wire format as an ignored compatibility field.

The receiver exists only while the VidroX screen is active. It accepts a fixed command allowlist and canonical YouTube targets; it never accepts arbitrary JavaScript or arbitrary URLs.

The phone controller hosts the mobile YouTube website in an Android WebView. It tracks main-frame history changes, including YouTube single-page navigation, and enables **На ТВ** only when the current address resolves to a canonical YouTube video. Every send opens a live picker containing the VidroX receivers currently discovered on the Wi-Fi network; the command is sent only after the user chooses one. The browser stays alive behind the full-screen remote overlay so video selection is not lost while playback controls are used. Top-level navigation is limited to HTTPS YouTube and Google domains; arbitrary web addresses are never sent to the receiver.

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
- Selecting a discovered receiver avoids accidental sends to another VidroX, but any device on the same reachable LAN that implements the protocol can control an active receiver. The traffic is not encrypted.
- SDK 36 receives implicit LAN access through `INTERNET`; targeting SDK 37 later requires declaring and requesting `ACCESS_LOCAL_NETWORK`.
- NSD registration and discovery follow the resumed application lifecycle and use a scoped Wi-Fi multicast lock for Android versions where the system does not manage mDNS reception.
- YouTube requires JavaScript and DOM storage in the phone WebView. Video targets are still revalidated by the shared protocol and again by the receiver.

## References

- https://developer.android.com/develop/connectivity/wifi/use-nsd
- https://developer.android.com/privacy-and-security/local-network-permission
- https://developer.android.com/reference/android/webkit/WebView#evaluateJavascript(java.lang.String,%20android.webkit.ValueCallback%3Cjava.lang.String%3E)
