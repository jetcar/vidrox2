# Specification: Android VidroX Receiver

## Goal

Let the existing Android VidroX application receive video and playback/navigation commands from a phone on the same Wi-Fi network.

## Behavior

- Start a random-port TCP server only while `YoutubeWV` is resumed and stop it on pause/disposal.
- Hold a Wi-Fi multicast lock only while resumed for reliable NSD on older Android versions.
- Generate and persist a six-digit pairing code for the receiver installation.
- Advertise `_vidrox._tcp.` through Android NSD.
- Show the pairing code and receiver state as an always-visible overlay.
- Validate every command before dispatching it on the WebView UI thread.
- `OPEN` loads the canonical YouTube watch URL.
- Playback commands operate on the active HTML video element.
- Navigation commands dispatch the existing D-pad/Escape behavior.

## Failure handling

- Invalid or unauthorized commands return an error and do not affect the WebView.
- Commands received before the WebView is ready return `receiver_not_ready`.
- Network/NSD failures update the overlay without crashing video playback.
- Server shutdown closes active sockets and unregisters NSD.

## Success criteria

- Existing VidroX behavior remains intact without a remote.
- Valid paired commands reach the WebView command dispatcher.
- Receiver APK builds and installs independently.
