# Changelog

## 1.0.1

Maintenance and compatibility release for the Forge 1.20.1 port.

### Added

- Optional Simple Voice Chat recording and replay playback support
- Optional Immersive Vehicles replay compatibility for vehicles, parts, seats, and riders
- Deterministic Oculus/Iris shader timing during video export
- Additional GUI visibility controls backported from newer Flashback releases

### Fixed

- Replay exit could wait forever after the replay viewer disconnected
- Chat could still render while the replay chat option was disabled
- Rewinding could leave stale lighting, missing blocks, ghost entities, or particles
- Paused replays could continue advancing particles, animated textures, players, and modded vehicles
- Forge payload errors could disconnect playback with “Too many network errors”
- Vehicle riders could remain behind, dismount immediately, or lose attached parts after seeking
- Replay title-screen button placement could overlap buttons added by other mods
- Replay GUI input could remain active while the interface was hidden

### Compatibility

- Minecraft 1.20.1
- Forge 47.4.10 or newer in the 1.20.1 series
- Java 17
- Optional Distant Horizons 3.2.0-b or newer
- Optional Immersive Vehicles 24.x
- Optional Simple Voice Chat 2.6.x

## 1.0.0

Initial Forge 1.20.1 compatibility build.

### Added

- Native Forge 47.4.10 integration for Minecraft 1.20.1
- Recording controls and replay-selection improvements backported from newer Flashback versions
- Player list, movement, render-filter, freeze, entity-tracking, and export-completion UI
- Optional Distant Horizons 3.2.0-b integration
- Japanese localization
- Windows taskbar export progress

### Fixed

- Startup crashes caused by Fabric-only initialization paths
- Missing replay players and untranslated entity names in editor windows
- Video export freezes, corrupted output frames, and missing completion dialog
- FFmpeg native loading conflicts between Modrinth profiles
- Replay-server packet synchronization failures with Forge mod payloads

### Compatibility

- Minecraft 1.20.1
- Forge 47.4.10 or newer in the 1.20.1 series
- Java 17

This is an unofficial compatibility port and is not supported by the original Flashback project.
