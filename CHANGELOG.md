# Changelog

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
