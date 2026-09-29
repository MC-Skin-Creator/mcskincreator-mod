# MC Skin Creator

**Build your Minecraft skin without leaving Minecraft.**

MC Skin Creator brings the [MC Skin Creator](https://mcskincreator.app) skin editor
into the game. Stack a skin out of layers, watch it on a player model, and put it on
your account with one button — and on your player, straight away, without restarting.

## What you can do

- **Pick pieces from the library.** Regions, categories, a search across everything, and
  a grid of elements laid out like inventory slots, named in your language. Point at
  one to try it on the model before you commit.
- **Stack layers.** Grouped by region, drag to reorder, show and hide, duplicate,
  remove. An inspector adjusts opacity, hue, saturation and brightness.
- **Start from a whole character or an outfit.** Ready-made models replace your stack;
  outfits go on over what you are wearing. Either one is a single undo away.
- **See where everything comes from.** The "i" on a thumbnail shows the work an element
  was cut out of: title, author, licence.
- **Undo and redo** over 60 states, one entry per gesture.
- **Keep your skins.** *My skins* opens, saves and deletes the skins kept on the MC
  Skin Creator server — the same ones the website shows.
- **Export** the composed 64×64 sheet, or the character seen from the front, to
  `<game>/mcskincreator/`.
- **Apply it to your Minecraft account**, classic or slim model, and **wear it right
  away**: once Mojang accepts the upload, your player here changes without a restart.
  Other players see it when Minecraft's own profile servers catch up.

The editor takes its layout from the website — three zones, the same gestures — and its
look from the game: every surface is one of Minecraft's own interface sprites, so a
resource pack that restyles the game restyles the editor too. When the window is
narrow, the side columns become drawers.

Open it from the **Skin Creator** panel on the title screen or the pause menu.
Available in English, French and Spanish.

## What is not there yet

Pixel drawing tools, per-element colours, importing a texture, an MC Skin Creator
account for the saved skins, and a fitting room that tries a skin on locally without
touching your account. They are tracked in the
[issues](https://github.com/MC-Skin-Creator/mcskincreator-mod/issues).

## Your session token

Applying a skin is the one thing that needs your Minecraft session token, so here is
exactly what happens to it:

- It is read at the moment you press **Apply** and sent to one address only,
  `https://api.minecraftservices.com/minecraft/profile/skins` — the call the official
  launcher makes.
- It is never sent to the MC Skin Creator server, never logged, never put in a crash
  report, and never kept between two uploads.
- Only two short files can touch it. Nothing else in the mod can reach it.
- An upload happens once per press. Never on a timer, never in a batch, and the
  button locks between two uploads.
- The button confirms first, and it is absent — with the reason — when you are not
  signed in with a Microsoft account.

The source is public precisely so you can check this yourself.

## Network use

Besides the upload above, the mod talks to the MC Skin Creator API
(`https://mcskincreator.app/api/v1`) to read the catalogue and to keep your saved
skins. Saved skins are tied to a random client identifier the mod stores in
`config/mcskincreator-client.txt`. Nothing else leaves your machine.

## Requirements

| Minecraft | Java | Fabric Loader | Fabric API |
|---|---|---|---|
| 1.21.11 | 21 | 0.19.3+ | required |
| 26.2 | 25 | 0.19.3+ | required |

Client-side only: nothing to install on a server, and it works in singleplayer and on
any server.

> The **1.21.11** build is the one that has been played in a real game. The **26.2**
> build compiles and carries the right metadata but has not been run yet, so it is
> published as an alpha.

## Links

- [Source code](https://github.com/MC-Skin-Creator/mcskincreator-mod)
- [Report a bug or ask for a feature](https://github.com/MC-Skin-Creator/mcskincreator-mod/issues)
- [The website](https://mcskincreator.app)

## License

Source-available, not open source: you can read, audit and build the code for your own
use, but redistribution, forks and reuse are not allowed. See the
[license](https://github.com/MC-Skin-Creator/mcskincreator-mod/blob/main/LICENSE).
The official downloads are this page, CurseForge and the GitHub releases; a jar from
anywhere else should not be trusted.

*Minecraft is a trademark of Mojang Studios. This project is not affiliated with or
endorsed by Mojang Studios or Microsoft.*
