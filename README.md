<p align="center">
  <img src="src/main/resources/assets/mcskincreator/icon.png" width="128" alt="MC Skin Creator">
</p>

<h1 align="center">MC Skin Creator</h1>

<p align="center">
  Create your Minecraft skin without leaving the game.
</p>

MC Skin Creator brings the [skin editor of the website](https://mcskincreator.app/)
into Minecraft. Pick pieces, stack them, see the result on your character, and wear it
on your account in one click.

## What you can do

- **Open the editor from the menus.** A Skin Creator button sits on the title screen and
  the pause menu.
- **Browse a big library of pieces.** Hair, faces, clothes, accessories and more, with a
  search across everything. Names come in your language, and the "i" on a piece tells
  you who made it.
- **Try before you commit.** Point at a piece to see it on your character, who turns
  around as you move the mouse.
- **Build your skin in layers.** Reorder, hide, duplicate or remove pieces, and change
  their colours, opacity, hue, saturation and brightness.
- **Start from something ready-made.** Take a complete starter character, or add an outfit
  on top of what you are wearing. Changed your mind? Undo brings it back.
- **Keep your skins.** My skins stores your creations, and they are the same ones you
  see on the website.
- **Save a picture of your skin.** Export the skin file, or the character seen from the
  front.
- **Wear it for real.** Apply the skin to your Minecraft account, classic or slim arms.
  You see it on your own character right away, no restart. Other players see it a little
  later, once Minecraft's servers catch up.

It is available in English, French and Spanish, and it only runs on your own game:
nothing to install on a server, nothing changed for other players.

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

Runs on **Fabric**, **Quilt**, **NeoForge** or **Forge**: each release has one jar per
Minecraft version and per loader.

On Fabric, it requires [Fabric Loader](https://fabricmc.net/use/) and the Fabric API.

| Minecraft | Java | Fabric Loader | Fabric API |
|---|---|---|---|
| 1.20, 1.20.1 | 17 | 0.19.3 or newer | 0.92.12+1.20.1 |
| 1.20.2 | 17 | 0.19.3 or newer | 0.91.6+1.20.2 |
| 1.20.3, 1.20.4 | 17 | 0.19.3 or newer | 0.97.3+1.20.4 |
| 1.20.5, 1.20.6 | 21 | 0.19.3 or newer | 0.100.8+1.20.6 |
| 1.21, 1.21.1 | 21 | 0.19.3 or newer | 0.116.17+1.21.1 |
| 1.21.2, 1.21.3 | 21 | 0.19.3 or newer | 0.114.1+1.21.3 |
| 1.21.4 | 21 | 0.19.3 or newer | 0.119.4+1.21.4 |
| 1.21.5 | 21 | 0.19.3 or newer | 0.128.2+1.21.5 |
| 1.21.6, 1.21.7, 1.21.8 | 21 | 0.19.3 or newer | 0.136.1+1.21.8 |
| 1.21.9, 1.21.10 | 21 | 0.19.3 or newer | 0.138.4+1.21.10 |
| 1.21.11 | 21 | 0.19.3 or newer | 0.141.6+1.21.11 |
| 26.1, 26.1.1, 26.1.2 | 25 | 0.19.3 or newer | 0.155.3+26.1.2 |
| 26.2 | 25 | 0.19.3 or newer | 0.160.0+26.2 |
| 26.3 | 25 | 0.19.3 or newer | 0.161.0+26.3 |

On [Quilt](https://quiltmc.org/), nothing else is needed either: no QSL, no Fabric API.
The Quilt jars end in `-quilt.jar`.

| Minecraft | Java | Quilt Loader |
|---|---|---|
| 1.20, 1.20.1 | 17 | 0.30.1 or newer |
| 1.20.2 | 17 | 0.30.1 or newer |
| 1.20.3, 1.20.4 | 17 | 0.30.1 or newer |
| 1.20.5, 1.20.6 | 21 | 0.30.1 or newer |
| 1.21, 1.21.1 | 21 | 0.30.1 or newer |
| 1.21.2, 1.21.3 | 21 | 0.30.1 or newer |
| 1.21.4 | 21 | 0.30.1 or newer |
| 1.21.5 | 21 | 0.30.1 or newer |
| 1.21.6, 1.21.7, 1.21.8 | 21 | 0.30.1 or newer |
| 1.21.9, 1.21.10 | 21 | 0.30.1 or newer |
| 1.21.11 | 21 | 0.30.1 or newer |
| 26.1, 26.1.1, 26.1.2 | 25 | 0.30.1 or newer |
| 26.2 | 25 | 0.30.1 or newer |
| 26.3 | 25 | 0.30.1 or newer |

On [NeoForge](https://neoforged.net/), nothing else is needed. The NeoForge jars end in
`-neoforge.jar`. It starts at 1.20.6: before that, use the Forge, Fabric or Quilt jar.

| Minecraft | Java | NeoForge |
|---|---|---|
| 1.20.6 | 21 | 20.6.141 or newer |
| 1.21.1 | 21 | 21.1.203 or newer |
| 1.21.3 | 21 | 21.3.97 or newer |
| 1.21.4 | 21 | 21.4.158 or newer |
| 1.21.5 | 21 | 21.5.98 or newer |
| 1.21.8 | 21 | 21.8.54 or newer |
| 1.21.10 | 21 | 21.10.64 or newer |
| 1.21.11 | 21 | 21.11.45 or newer |
| 26.1, 26.1.1, 26.1.2 | 25 | 26.1.2.112 or newer |
| 26.2 | 25 | 26.2.0.88 or newer |
| 26.3 | 25 | 26.3.0.36-beta or newer |

On [Forge](https://files.minecraftforge.net/), nothing else is needed either. The Forge
jars end in `-forge.jar`.

| Minecraft | Java | Forge |
|---|---|---|
| 1.20.1 | 17 | 47.4.10 or newer |
| 1.20.2 | 17 | 48.1.0 or newer |
| 1.20.4 | 17 | 49.2.0 or newer |
| 1.20.6 | 21 | 50.2.0 or newer |
| 1.21.1 | 21 | 52.1.0 or newer |
| 1.21.3 | 21 | 53.1.0 or newer |
| 1.21.4 | 21 | 54.1.14 or newer |
| 1.21.5 | 21 | 55.1.0 or newer |
| 1.21.8 | 21 | 58.1.0 or newer |
| 1.21.10 | 21 | 60.1.15 or newer |
| 1.21.11 | 21 | 61.2.1 or newer |
| 26.1.2 | 25 | 64.1.3 or newer |
| 26.2 | 25 | 65.1.3 or newer |
| 26.3 | 25 | 66.0.9 or newer |

Drop the jar matching your Minecraft version and your loader into `.minecraft/mods/`. You know it is
loaded when the title screen shows the **Skin Creator** panel against its right edge.

Jars come from the [releases](https://github.com/MC-Skin-Creator/mcskincreator-mod/releases)
page, one per Minecraft version. **Releases** (`v0.2.0`) are the ones to use.
**Pre-releases** (`v0.2.0-dev.7`) are built from `develop` on every merge and are not
tested: to try something early, not to play.

## License

Proprietary, source-available: see [`LICENSE`](LICENSE). The code is public so that
anyone can read and audit what a mod that handles a session token does.

- Modpacks may include the unmodified official jar, as long as the modpack is free.
- Forks are allowed only to propose a change through a pull request. Contributions are
  not expected, and there is no promise to review them.
- Any other redistribution, modified version or commercial use is not allowed.

The official channels are this repository's releases, plus Modrinth and CurseForge under
the copyright holder's account. A jar from anywhere else is not covered and should not
be trusted.

Minecraft is a trademark of Mojang Studios. This project is not affiliated with or
endorsed by Mojang Studios or Microsoft.
