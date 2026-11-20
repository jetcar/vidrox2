# Specification: VidroX Remote for Android

## Goal

Provide a separate touch-first Android phone application for sending YouTube videos to VidroX and controlling playback.

## Behavior

- Discover VidroX receivers on the current LAN via Android NSD.
- Let the user select a receiver and enter its six-digit code.
- Accept pasted YouTube URLs or video IDs and send them with one action.
- Appear in Android's text share sheet so a YouTube link can be sent into the remote.
- Remember the last pairing code on the phone.
- Provide always-visible play/pause, seek ±10 seconds, back, D-pad, and select controls.
- Show discovery, connection, success, and error states in Russian.
- Stop discovery and release its multicast lock whenever the screen is paused.
- Keep controls inside safe display insets and adapt to short/landscape screens.
- Check the latest GitHub Release at startup and select only the matching `VidroX-Remote-*` APK.

## Success criteria

- Invalid codes and video targets are rejected before connecting.
- Commands are sent off the UI thread with timeouts.
- Remote APK builds and installs independently from VidroX.
- Update downloads are accepted only after size, SHA-256 digest, and package-name validation.
