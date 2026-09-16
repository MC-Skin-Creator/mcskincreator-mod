# MC Skin Creator - Minecraft mod

Fabric client mod that brings the [MC Skin Creator](https://github.com/MC-Skin-Creator)
skin editor into the game.

## Status: first version

This version does one thing, and does it on purpose: it proves the whole chain
works - the mod builds, the repository packages a jar on its own, the game loads
the mod, and the mod can open a screen.

What is in:

- a **Skin Creator** button in the title screen and in the pause menu
- a screen that opens, closes with Escape and returns to the menu it came from
- English, French and Spanish translations
- a jar built by CI on every push and every pull request

What is not in yet: the editor itself. The element library, the layer panel, the
colour controls, the player preview, the account and the skin upload all live in
the [issues](https://github.com/MC-Skin-Creator/mcskincreator-mod/issues).

## Install

1. Minecraft **1.21.11**
2. [Fabric Loader](https://fabricmc.net/use/installer/) **0.19.5** or newer
3. [Fabric API](https://modrinth.com/mod/fabric-api) **0.141.6+1.21.11**
4. drop the mod jar into `.minecraft/mods/`

No release is published yet. Grab the jar from the **Actions** tab of this
repository: open the latest `build` run and download the `mcskincreator-<sha>`
artifact.

You know the mod is loaded when the log prints `MC Skin Creator 0.1.0 loaded` and
the title screen shows the **Skin Creator** button in its bottom-left corner.

## Build from source

Requires a JDK 21. The Gradle wrapper handles the rest.

```sh
./gradlew build
```

The jar lands in `build/libs/`. Use `./gradlew runClient` to launch a development
game with the mod loaded.

Versions are pinned in [`gradle.properties`](gradle.properties) and explained in
[`DECISIONS.md`](DECISIONS.md) - that is the only file to touch when bumping the
game version.

## License

Proprietary, source-available - see [`LICENSE`](LICENSE). The code is readable and
auditable by anyone; redistribution and forks are not allowed.

Minecraft is a trademark of Mojang Studios. This project is not affiliated with or
endorsed by Mojang Studios or Microsoft.
