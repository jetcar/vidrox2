# Capability Map: Android Remote Control

| Module | Responsibility | Depends on |
|---|---|---|
| `remote-core` | Versioned command protocol, validation, YouTube target parsing | None |
| `android-receiver` | Advertise VidroX on the LAN, authenticate commands, control the WebView | `remote-core` |
| `android-controller` | Discover receivers, pair by code, send videos and controls | `remote-core` |
| `android-distribution` | Build and copy both installable APKs with versioned names | Receiver, controller |

Build order: protocol → receiver/controller → distribution.
