# Specification: VidroX Remote for Android

## Goal

Provide a separate touch-first Android phone application for sending YouTube videos to VidroX and controlling playback.

## Behavior

- Discover VidroX receivers on the current LAN via Android NSD.
- Let the user choose a discovered receiver without a pairing code.
- Browse YouTube in the app and send the current video with one action; also accept shared YouTube links.
- Appear in Android's text share sheet so a YouTube link can be sent into the remote.
- Provide only always-visible play/pause and seek ±10 seconds controls.
- Show discovery, connection, success, and error states in Russian.
- Stop discovery and release its multicast lock whenever the screen is paused.
- Keep controls inside safe display insets and adapt to short/landscape screens.
- Check the latest GitHub Release at startup and select only the matching `VidroX-vX.Y.Z.remote.apk` file.

## Success criteria

- Invalid video targets are rejected before connecting.
- Commands are sent off the UI thread with timeouts.
- Remote APK builds and installs independently from VidroX.
- Update downloads are accepted only after size, SHA-256 digest, and package-name validation.
