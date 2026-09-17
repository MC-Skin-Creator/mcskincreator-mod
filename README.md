<img src="src/main/resources/assets/mcskincreator/icon.png" width="96" align="right" alt="MC Skin Creator">

# MC Skin Creator - Minecraft mod

Fabric client mod that brings the [MC Skin Creator](https://github.com/MC-Skin-Creator)
skin editor into the game.

## Status: the editor, in the site's own interface

The editor stacks a skin out of layers and shows it on a player model. It does not
apply the skin yet: nothing you build leaves the screen.

The screen takes its arrangement from the site — the same three zones, the same
gestures — and its paint from Minecraft: every surface on it is one of the game's own
interface sprites, so a resource pack that restyles the game restyles this screen too.
The rules it follows are in [`INTERFACE.md`](INTERFACE.md).

What is in:

- a **Skin Creator** panel on the right of the title screen and the pause menu
- the **library**, left: regions, the categories of a region as a fixed row of
  icons, search across every region, and a scrolling grid of elements in inventory
  slots, named in your language
- the **scene**, middle: the player model, which turns under the mouse and never
  goes away — it shrinks, it never hides. Point at an element to try it on
- the **layers**, right: the stack grouped by region, drag to reorder, show and
  hide, duplicate and remove, and an inspector for opacity, hue, saturation and
  brightness
- three columns while the window is wide enough, drawers below that, either side
  column foldable
- undo and redo over 60 states, with one entry per gesture rather than per frame
- export of the composed 64x64 sheet as a PNG, into `<game>/mcskincreator/`
- English, French and Spanish, with a label and a tooltip for every control
- multi-version builds through [Stonecutter](https://stonecutter.kikugie.dev/):
  **Minecraft 1.21.11 and 26.2** from the same code
- CI building every supported version on each push and pull request

What is not in yet: the pixel drawing tools, per-element colours, importing a
texture, the account, and applying the skin. They all live in the
[issues](https://github.com/MC-Skin-Creator/mcskincreator-mod/issues).

The editor reads the MC Skin Creator API at `https://mcskincreator.app/api/v1`, so
it needs to reach it. Point the mod at another deployment - a local back-end, say -
with a system property or an environment variable, whichever is easier to set where
you launch the game:

```sh
-Dmcskincreator.api=http://localhost:3000/api/v1
MCSKINCREATOR_API=http://localhost:3000/api/v1
```

Neither is needed to play. When the library does not arrive, the screen says which
address the mod tried and what came back, so a wrong address reads differently from
a network that is down.

## Install

| Minecraft | Java | Fabric Loader | Fabric API |
|---|---|---|---|
| 1.21.11 | 21 | 0.19.3 or newer | 0.141.6+1.21.11 |
| 26.2 | 25 | 0.19.3 or newer | 0.160.0+26.2 |

Drop the jar matching your Minecraft version into `.minecraft/mods/`.

Builds are published on the [releases](https://github.com/MC-Skin-Creator/mcskincreator-mod/releases)
page, one jar per supported Minecraft version:

- **releases** (`v0.2.0`) are cut from `main` and are the ones to use;
- **pre-releases** (`v0.2.0-dev.7`) are cut from `develop` on every merge. They are
  previews of the next release, built but not tested. Use them to try something
  early, not to play.

You know the mod is loaded when the log prints `MC Skin Creator 0.1.0 loaded` and
the title screen shows the **Skin Creator** panel against its right edge.

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

Jars are named `mcskincreator-<mod version>+mc<minecraft version>.jar`. The mod
version and the Minecraft version are independent: supporting a new game version is
not a new version of the mod.

Supported versions and their dependencies are declared in
[`stonecutter.properties.toml`](stonecutter.properties.toml) — the only file to
touch when adding a version or bumping a dependency. The reasoning behind the
toolchain is in [`DECISIONS.md`](DECISIONS.md), the working rules for the
multi-version source tree are in [`CLAUDE.md`](CLAUDE.md), and the rules the
interface follows are in [`INTERFACE.md`](INTERFACE.md).

## License

Proprietary, source-available - see [`LICENSE`](LICENSE). The code is readable and
auditable by anyone; redistribution and forks are not allowed.

Minecraft is a trademark of Mojang Studios. This project is not affiliated with or
endorsed by Mojang Studios or Microsoft.
