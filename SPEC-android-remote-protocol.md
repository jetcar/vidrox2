# Specification: VidroX Remote Protocol

## Goal

Provide a small, dependency-free local-network protocol shared by the VidroX receiver and the Android phone remote.

## Contract

- Transport: one UTF-8 command per TCP connection, terminated by `\n`.
- Discovery: DNS-SD service type `_vidrox._tcp.`.
- Wire format: `VIDROX/1<TAB>PAIRING_CODE<TAB>ACTION<TAB>ARGUMENT`.
- Maximum line length: 4096 characters.
- Pairing code: exactly six decimal digits, generated once by the receiver installation.
- Actions: `OPEN`, `PLAY_PAUSE`, `SEEK_BACK`, `SEEK_FORWARD`, `BACK`, `UP`, `DOWN`, `LEFT`, `RIGHT`, `SELECT`.
- `OPEN` requires a YouTube video ID or an `https://youtube.com` / `https://youtu.be` URL.
- Other actions have no argument.
- Receiver response: `OK` or `ERROR<TAB>REASON`.

## Security boundaries

- Reject malformed versions, codes, actions, tabs/newlines in arguments, oversized messages, and non-YouTube targets.
- Bind only while the VidroX receiver screen is active.
- Do not expose arbitrary URL loading or arbitrary JavaScript execution.
- Pairing protects against accidental control on a shared LAN; traffic is not encrypted.

## Success criteria

- Shared JVM tests cover round trips and all rejection paths.
- Both Android applications use the same protocol implementation.
