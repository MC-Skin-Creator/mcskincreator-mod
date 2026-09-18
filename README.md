<img src="src/main/resources/assets/mcskincreator/icon.png" width="96" align="right" alt="MC Skin Creator">

# MC Skin Creator - Minecraft mod

Fabric client mod that brings the [MC Skin Creator](https://github.com/MC-Skin-Creator)
skin editor into the game.

## Status: the editor, and the skin on your account

The editor stacks a skin out of layers, shows it on a player model, and can put it
on your Minecraft account — the real one, the one everybody sees. That last step is
the only thing the mod does that leaves your machine, it happens once per press of a
button, and it never happens on its own.

The screen is the site's, in the game — the same materials, the same palette, the
same gestures, the same three zones. The rules it follows are in
[`INTERFACE.md`](INTERFACE.md).

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
- **where every element comes from**: the "i" on a thumbnail opens the work it was
  cut out of — title, author, licence — and the starter models it is a piece of
- **My skins**: the skins kept on the server, opened, saved and deleted from the
  game. They are the same entries the site's own library holds
- three columns while the window is wide enough, drawers below that, either side
  column foldable
- undo and redo over 60 states, with one entry per gesture rather than per frame
- export into `<game>/mcskincreator/`: the composed 64x64 sheet, or the character
  seen from the front
- **applying the skin to your Minecraft account**, through Mojang's own endpoint, with
  the classic or slim model you chose. The button confirms first and says what it will
  cost, it locks between two uploads, and it is absent — with the reason — when the
  session is not signed in with Microsoft
- English, French and Spanish, with a label and a tooltip for every control
- multi-version builds through [Stonecutter](https://stonecutter.kikugie.dev/):
  **Minecraft 1.21.11 and 26.2** from the same code
- CI building every supported version on each push and pull request

What is not in yet: the pixel drawing tools, per-element colours, importing a
texture, the MC Skin Creator account the saved skins will hang off, and the local
fitting room — trying a skin on in your own client without touching the account.
They all live in the
[issues](https://github.com/MC-Skin-Creator/mcskincreator-mod/issues).

### What the mod does with your session

Applying a skin is the one thing here that needs your Minecraft session token, so it
is worth saying plainly where it goes.

- The token is read at the moment you press **Apply** and sent to exactly one address,
  `https://api.minecraftservices.com/minecraft/profile/skins` — the same call the
  official launcher makes for its own account.
- It is never sent to the MC Skin Creator back-end, never written to a log, never put
  in a crash report, and never kept in a field between two uploads.
- Two files hold all of it, and they are short on purpose:
  [`MojangSkins`](src/main/java/fr/clixmods/mcsc/mod/account/MojangSkins.java) and
  [`GameSession`](src/main/java/fr/clixmods/mcsc/mod/account/GameSession.java). Nothing
  outside that package can reach the token at all.
- Uploading happens once per press. The mod never uploads on its own, on a timer, or
  in a batch, and it locks the button between two uploads so a session cannot be
  rate-limited by an impatient click.

A dedicated `SECURITY.md` is [issue #13](https://github.com/MC-Skin-Creator/mcskincreator-mod/issues/13).

The editor reads the MC Skin Creator API at `https://mcskincreator.app/api/v1`, and
uses every route of that contract: the catalogue and its atlases, the search, an
element's provenance, the composed texture and front view, and the five routes of
the saved-skin library. Nothing outside `/api/v1` is called — the rest of the site's
API travels with its own front end and promises nothing to a mod. Point the mod at
another deployment - a local back-end, say - with a system property or an
environment variable, whichever is easier to set where you launch the game:

```sh
-Dmcskincreator.api=http://localhost:3000/api/v1
MCSKINCREATOR_API=http://localhost:3000/api/v1
```

Neither is needed to play. When the library does not arrive, the screen says which
address the mod tried and what came back, so a wrong address reads differently from
a network that is down.

The saved skins have no account behind them yet. The mod draws a client identifier
once, keeps it in `config/mcskincreator-client.txt`, and sends it with every call to
the library — exactly as the site does in the browser. Deleting that file does not
delete the skins on the server, but it does lose the way back to them.

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
