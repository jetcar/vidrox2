# Specification: Android VidroX Receiver

## Goal

Let the existing Android VidroX application receive video and playback/navigation commands from a phone on the same Wi-Fi network.

## Behavior

- Start a random-port TCP server only while `YoutubeWV` is resumed and stop it on pause/disposal.
- Hold a Wi-Fi multicast lock only while resumed for reliable NSD on older Android versions.
- Advertise `_vidrox._tcp.` through Android NSD.
- Show the receiver state as an always-visible overlay.
- Validate every command before dispatching it on the WebView UI thread.
- `OPEN` loads the selected video through the YouTube TV watch route.
- Playback commands operate on the active HTML video element.
- Navigation commands dispatch the existing D-pad/Escape behavior.

## Failure handling

- Invalid commands return an error and do not affect the WebView.
- Commands received before the WebView is ready return `receiver_not_ready`.
- Network/NSD failures update the overlay without crashing video playback.
- Server shutdown closes active sockets and unregisters NSD.

## Success criteria

- Existing VidroX behavior remains intact without a remote.
- Valid local-network commands reach the WebView command dispatcher.
- Receiver APK builds and installs independently.
