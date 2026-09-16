<img src="src/main/resources/assets/mcskincreator/icon.png" width="96" align="right" alt="MC Skin Creator">

# MC Skin Creator - Minecraft mod

Fabric client mod that brings the [MC Skin Creator](https://github.com/MC-Skin-Creator)
skin editor into the game.

## Status: first version

This version does one thing, and does it on purpose: it proves the whole chain
works - the mod builds for several Minecraft versions from one source tree, the
repository packages the jars on its own, the game loads the mod, and the mod can
open a screen.

What is in:

- a **Skin Creator** button in the title screen and in the pause menu
- a screen that opens, closes with Escape and returns to the menu it came from
- English, French and Spanish translations
- multi-version builds through [Stonecutter](https://stonecutter.kikugie.dev/):
  **Minecraft 1.21.11 and 26.2** from the same code
- CI building every supported version on each push and pull request

What is not in yet: the editor itself. The element library, the layer panel, the
colour controls, the player preview, the account and the skin upload all live in
the [issues](https://github.com/MC-Skin-Creator/mcskincreator-mod/issues).

## Install

| Minecraft | Java | Fabric Loader | Fabric API |
|---|---|---|---|
| 1.21.11 | 21 | 0.19.3 or newer | 0.141.6+1.21.11 |
| 26.2 | 25 | 0.19.3 or newer | 0.160.0+26.2 |

Drop the jar matching your Minecraft version into `.minecraft/mods/`.

No release is published yet. Grab the jar from the **Actions** tab of this
repository: open the latest `build` run and download the artifact for your
version (`mcskincreator-mc1.21.11-…` or `mcskincreator-mc26.2.x-…`).

You know the mod is loaded when the log prints `MC Skin Creator 0.1.0 loaded` and
the title screen shows the **Skin Creator** button in its bottom-left corner.

> The 1.21.11 jar is the one that has been exercised in a real game. The 26.2 jar
> compiles and carries the right metadata, but has not been run yet.

## Build from source

Requires a JDK 21. Gradle downloads the Java 25 toolchain needed by the 26.2
target on its own.

```sh
./gradlew build            # every supported Minecraft version
./gradlew :1.21.11:build   # a single version
./gradlew buildAndCollect  # all versions, jars collected in build/libs/<mod version>/
```

Jars are named `mcskincreator-<mod version>+mc<minecraft version>.jar`.

Supported versions and their dependencies are declared in
[`stonecutter.properties.toml`](stonecutter.properties.toml) — the only file to
touch when adding a version or bumping a dependency. The reasoning behind the
toolchain is in [`DECISIONS.md`](DECISIONS.md), and the working rules for the
multi-version source tree are in [`CLAUDE.md`](CLAUDE.md).

## License

Proprietary, source-available - see [`LICENSE`](LICENSE). The code is readable and
auditable by anyone; redistribution and forks are not allowed.

Minecraft is a trademark of Mojang Studios. This project is not affiliated with or
endorsed by Mojang Studios or Microsoft.
