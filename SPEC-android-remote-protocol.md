# Specification: VidroX Remote Protocol

## Goal

Provide a small, dependency-free local-network protocol shared by the VidroX receiver and the Android phone remote.

## Contract

- Transport: one UTF-8 command per TCP connection, terminated by `\n`.
- Discovery: DNS-SD service type `_vidrox._tcp.`.
- Wire format: `VIDROX/2<TAB><TAB>ACTION<TAB>ARGUMENT`; the empty second field is retained for compatibility.
- Maximum line length: 4096 characters.
- Compatibility: updated receivers also accept legacy `VIDROX/1` commands; updated remotes report that an older receiver must be updated.
- Actions: `OPEN`, `PLAY_PAUSE`, `SEEK_BACK`, `SEEK_FORWARD`, `BACK`, `UP`, `DOWN`, `LEFT`, `RIGHT`, `SELECT`.
- `OPEN` requires a YouTube video ID or an `https://youtube.com` / `https://youtu.be` URL.
- Other actions have no argument.
- Receiver response: `OK` or `ERROR<TAB>REASON`.

## Security boundaries

- Reject malformed versions, actions, tabs/newlines in arguments, oversized messages, and non-YouTube targets.
- Bind only while the VidroX receiver screen is active.
- Do not expose arbitrary URL loading or arbitrary JavaScript execution.
- Receiver selection prevents accidental sends to another discovered VidroX. Any device on the reachable LAN that implements the protocol can control the active receiver; traffic is not encrypted.

## Success criteria

- Shared JVM tests cover round trips and all rejection paths.
- Both Android applications use the same protocol implementation.
