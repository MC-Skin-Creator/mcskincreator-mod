<img src="src/main/resources/assets/mcskincreator/icon.png" width="96" align="right" alt="MC Skin Creator">

# MC Skin Creator - Minecraft mod

Fabric client mod that brings the [MC Skin Creator](https://mcskincreator.app/) skin
editor into the game: build a skin out of layers, see it on your character, and put it
on your Minecraft account without leaving Minecraft.

Client side only. Nothing to install on a server, and it does nothing to other players.

> **Early release (0.x).** The editor and the upload to your account work. Some
> things are not built yet, listed [below](#not-in-yet).

## What you can do

- open the editor from the **Skin Creator** panel on the title screen and the pause menu
- browse the **library**: regions, categories, a search across everything, and every
  element's source ("i" on a thumbnail: title, author, licence), named in your language
- try an element on by pointing at it, and see the result on the **scene** player model,
  which turns under the mouse
- stack elements as **layers**: reorder by dragging, show or hide, duplicate, remove, and
  adjust opacity, hue, saturation and brightness, with a swatch for each colour an
  element declares
- start from a **starter model** or add an **outfit**, and take either back with one undo
- undo and redo over 60 states
- keep skins in **My skins**, the same library the website uses
- **export** the composed 64x64 sheet, or the character seen from the front, to
  `<game>/mcskincreator/`
- **apply the skin to your Minecraft account**, with the classic or slim model. The button
  confirms first, locks between two uploads, and is absent (with the reason) when the
  session is not signed in with Microsoft. Once Mojang accepts it you wear it straight
  away, without restarting; other players see it when Minecraft's profile servers
  catch up

Available in English, French and Spanish. The window adapts to its size: three columns
when there is room, drawers below that.

### Not in yet

The pixel drawing tools, importing a texture, an MC Skin Creator account for the saved
skins, and a fitting room to try a skin on locally without touching your account. Follow
them in the [issues](https://github.com/MC-Skin-Creator/mcskincreator-mod/issues).

## Your Minecraft session

Applying a skin is the one thing that needs your session token, so here is where it goes:

- it is read when you press **Apply** and sent to exactly one address, Mojang's own skin
  endpoint (`api.minecraftservices.com`), the call the official launcher makes;
- it is never sent to the MC Skin Creator back-end, never logged and never written to disk;
- the mod never uploads on its own, on a timer or in a batch.

The code that touches it is two short files,
[`GameSession`](src/main/java/fr/clixmods/mcsc/mod/account/GameSession.java) and
[`MojangSkins`](src/main/java/fr/clixmods/mcsc/mod/account/MojangSkins.java), and nothing
outside that package can reach the token. [`SECURITY.md`](SECURITY.md) has every address
the mod contacts, what it stores on your disk, and how to report a vulnerability.

The editor reads the MC Skin Creator API (`https://mcskincreator.app/api/v1`) for the
catalogue, search, textures and the saved-skin library.

## Install

Requires [Fabric Loader](https://fabricmc.net/use/) and the Fabric API.

| Minecraft | Java | Fabric Loader | Fabric API |
|---|---|---|---|
| 1.21.11 | 21 | 0.19.3 or newer | 0.141.6+1.21.11 |
| 26.2 | 25 | 0.19.3 or newer | 0.160.0+26.2 |

Drop the jar matching your Minecraft version into `.minecraft/mods/`. You know it is
loaded when the title screen shows the **Skin Creator** panel against its right edge.

Jars come from the [releases](https://github.com/MC-Skin-Creator/mcskincreator-mod/releases)
page, one per Minecraft version. **Releases** (`v0.2.0`) are the ones to use.
**Pre-releases** (`v0.2.0-dev.7`) are built from `develop` on every merge and are not
tested: to try something early, not to play.

## License

Proprietary, source-available: see [`LICENSE`](LICENSE). The code is public so that
anyone can read and audit what a mod that handles a session token does. Redistribution,
forks and commercial use are not allowed. The
official channels are this repository's releases, plus Modrinth and CurseForge under
the copyright holder's account. A jar from anywhere else is not covered and should not
be trusted.

Minecraft is a trademark of Mojang Studios. This project is not affiliated with or
endorsed by Mojang Studios or Microsoft.
