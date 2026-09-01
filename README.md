<p align="center">
  <img src="src/main/resources/assets/flashback/icon.png" alt="Flashback icon" width="128">
</p>

<h1 align="center">Flashback Forge 1.20.1</h1>

<p align="center">
  Record Minecraft gameplay, replay it, and create cinematic video exports.
</p>

> [!IMPORTANT]
> This is an unofficial Forge 1.20.1 compatibility port of Moulberry's Flashback. It is not an official Flashback release and is not supported by the original project's support channels.

Copyright 2024 Moulberry. Do not reupload or redistribute without permission from the copyright holder.

## Features

- Records and plays back Minecraft sessions
- Timeline-based camera and visual editing
- MP4 and other FFmpeg-backed video exports
- Backported editor features and UI behavior from newer Flashback releases
- Optional Distant Horizons 3.2.0-b integration
- English and Japanese localization

## Requirements

- Minecraft 1.20.1
- Minecraft Forge 47.4.10 or newer in the 1.20.1 series
- Java 17
- Distant Horizons 3.2.0-b or newer is optional

This build is client-side only. Do not install it on a dedicated server.

## How to install

1. Install Minecraft Forge for Minecraft 1.20.1.
2. Build the project locally with Java 17.
3. Copy `build/libs/flashback-1.0.0.jar` into the profile's `mods` folder.
4. Remove older Flashback JARs from that folder, then start the game.

## Building

On Windows PowerShell:

```powershell
$env:JAVA_HOME = "C:\Path\To\jdk-17"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat clean build --no-daemon
```

On Linux or macOS:

```bash
JAVA_HOME=/path/to/jdk-17 ./gradlew clean build --no-daemon
```

The distributable artifact is generated at:

```text
build/libs/flashback-1.0.0.jar
```

The `-slim.jar` artifact does not contain the bundled runtime libraries and should not be installed by end users.

Repository release instructions are documented in [RELEASING.md](RELEASING.md).

## Credits

- Flashback was created by [Moulberry](https://github.com/Moulberry/Flashback).
- This repository contains an unofficial Minecraft Forge 1.20.1 port.
- Distant Horizons compatibility uses its public API and remains optional.

## Support

Do not request support for this port in the official Flashback support channels. Problems specific to this Forge port should be tracked in this repository.

## License

This project uses Flashback's custom license. See [LICENSE.md](LICENSE.md). The license states that redistribution is not permitted. Localization files under `src/main/resources/assets/flashback/lang` are separately covered by the terms documented in that license.
