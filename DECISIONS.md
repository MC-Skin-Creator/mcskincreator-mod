# Technical decisions

The four decisions of issue #1, plus the toolchain versions that follow from them.
Everything here was checked against `meta.fabricmc.net`, `maven.fabricmc.net` and
Mojang's version manifest, not copied from a tutorial.

## 1. Skin level

Three levels were on the table. We ship **level 1 first**:

| Level | What it does | Who sees the skin | Status |
|---|---|---|---|
| 1. Fitting room | the mod replaces the skin texture locally | you only | planned first |
| 2. Mojang upload | upload to the account through the official API | everyone, everywhere | after level 1 |
| 3. Server mod | rewriting the `textures` property server-side | everyone on that server | out of scope, another repository |

Players **will** mistake level 1 for a real skin change. The interface has to spell
the difference out, or every attempt will look like a bug.

## 2. Loader: Fabric

Light, follows game versions within days, and fits a client-side mod that is mostly
GUI plus network calls. NeoForge brings an ecosystem this mod does not need.
Architectury / multi-loader doubles the build complexity for a mod with no users
yet, so not at the start.

## 3. Game versions: 1.21.11 and 26.2, through Stonecutter

The mod is **not** pinned to a single version any more. [Stonecutter](https://stonecutter.kikugie.dev/)
compiles one shared source tree against several Minecraft versions, so Git branches
keep tracking work (`main`, `feature/*`) while Stonecutter tracks game
compatibility. Adding a version is a table in a TOML file, not a branch.

| Piece | Version | Why |
|---|---|---|
| Minecraft | `1.21.11`, `26.2` | Active target is 1.21.11, the version being tested in-game |
| Mappings | Official Mojang | See below |
| Fabric Loader | `0.19.3` | Lowest version the mod is built and declared against |
| Fabric API | `0.141.6+1.21.11`, `0.160.0+26.2` | Latest per target |
| Stonecutter | `0.9.8` | Current release |
| Loom | `1.17-SNAPSHOT` via `loom-back-compat` | See below |
| Gradle | `9.7.1` | Required by the Loom plugin line |
| Java | `21` on 1.21.11, `25` on 26.2 | Mojang's requirement per version |

**Yarn was dropped for official Mojang mappings.** Yarn has no build past
1.21.11 — nothing for 26.1 and later — so a shared source tree spanning both
generations cannot use it. Every API the mod uses was checked to have a direct
Mojang equivalent before the switch; nothing was lost. This also matches what the
official Stonecutter Fabric template does.

**Loom is selected per version by `dev.kikugie.loom-back-compat`.** Minecraft
below 26 is obfuscated and needs `fabric-loom-remap`; 26 and later ship
unobfuscated and use `fabric-loom`. The `1.17` line is pinned deliberately: the
1.18 line refuses to run on anything below a Java 25 JVM, which would impose Java
25 on the 1.21.11 target as well.

## 4. License: proprietary, source-available

The site is "all rights reserved", and the mod is distributed, so it has to say
under which terms. See [`LICENSE`](LICENSE): reading, auditing, building for
personal use and quoting the code are allowed; redistribution, forks and reuse are
not.

This is not a detail. Level 2 makes the mod handle the player's session token, and
the only honest argument for trusting it is that anyone can read the repository and
check. A repository with no license file is unusable even for someone who only
wants to audit it.

## 5. Naming

| Item | Value |
|---|---|
| Mod id | `mcskincreator` |
| Java package | `fr.clixmods.mcsc.mod` |
| Display name | MC Skin Creator |
| Jar base name | `mcskincreator` |

Artifacts are named `mcskincreator-<mod version>+mc<minecraft version>.jar`: the
mod version and the Minecraft version are different things and a compatibility
update is not a version bump.

The entry point is **client only** (`"environment": "client"` in `fabric.mod.json`):
there is no server side to this mod, and declaring one would only make it refuse to
load on servers for no reason.

## 6. Versioning, branches and releases

Semantic versioning, and **the Minecraft version is never part of the mod
version**. They answer different questions: the mod version says what the mod
does, the `+mc…` suffix says which game it was built against. Adding a game
version is not a release of the mod.

The number is derived from conventional-commit subjects since the last stable tag
by `.github/scripts/next-version.sh`. Below `1.0.0`, a breaking change bumps the
minor — there is no major to bump. **`1.0.0` is cut by hand**, when the mod
actually edits and applies a skin, and never by a commit message.

Branches follow a trimmed Git Flow: `main` is stable, `develop` integrates,
`feature/*` and `fix/*` do the work. `release/*` and `hotfix/*` are deliberately
absent until there is a shipped version worth hotfixing. This is orthogonal to
Stonecutter: Git branches track work, Stonecutter tracks game compatibility, and
no branch ever stands for a Minecraft version.

Releases are cut **on merge**, not from a hand-pushed tag: pushing to `main`
produces a release, pushing to `develop` produces a pre-release, and the workflow
creates the tag itself. This differs from the original wording of issue #15, which
assumed a tag as the trigger; the outcome it asked for — one artifact set per
release, no superfluous secret — is unchanged.

## 7. The interface

The mod reproduces the site's editor rather than designing a new one. The site
already imitates Minecraft, so there is nothing to "adapt to the Minecraft style" —
the work runs the other way. The rules are in [`INTERFACE.md`](INTERFACE.md); what
belongs here is the three decisions that shaped the code.

**Drawing goes through one object.** 26.x replaced immediate-mode GUI drawing with a
render-state extraction pass: the graphics object changed name and so did its text
calls. Everything else the mod draws with turned out to be the same call on both
targets — `fill`, `blit`, the scissor stack, tooltips, `NativeImage`,
`DynamicTexture`, `TextureManager`, `Identifier`, `RenderPipelines`, and every input
event, all checked with `javap` against both jars. So `Canvas` wraps that one object
and is the only file in the interface that names a Minecraft version. Some forty
other files draw without a single conditional.

**The widgets are the mod's own, not dressed-up vanilla ones.** The site's look is a
material and a bevel rather than a skin over a button, and half of these controls —
a category tab, an element thumbnail, a layer row — have no vanilla equivalent to
dress. What stays the game's is what the game is right about: its font, its GUI
scale, the player model, and a focus ring a keyboard can walk. That last one is not
decoration. Hovering exists with a mouse and not with a controller, and the site
reveals real functionality on hover — an element's provenance, a layer's delete
button — so anything revealed that way has to be reachable by focus too. Whatever
holds the focus draws as whatever is hovered, and that is decided in one place.

**The interface owns no pixels of its own.** The model is vanilla's
`PlayerSkinWidget`, the thumbnails are folded out of the category atlases the API
already serves, and the stack is composed by the server. The mod generates exactly
two textures: the stone grain under the panels and the icon atlas, both from
deterministic noise and pixel drawings written out as rows of characters — because a
drawing that can be read in a diff can be corrected in one, and a PNG of the same
thing cannot.

The layer stack is the mod's, and it follows the server's format rather than
inventing a second one: the project validator is the authority on what a project is,
and two representations of the same thing would drift. `ProjectJson` writes it in one
method for that reason.

## 8. Everything the mod asks of the site goes through `/api/v1`

The site's API answers under two prefixes, and they are not the same promise.
`/api/…` travels in the same jar as the site's own front end, changes with it on the
same day and guarantees nothing. `/api/v1/…` is a frozen contract: routes and fields
are added there, never removed or renamed, and a client compiled against it keeps
working. A mod is installed on somebody's machine and is a version — or ten — behind,
so it is the second one it calls, and only the second one.

That decides what the mod can be built on. The eleven routes of the contract are
all used: the catalogue and its atlases, the search, an element's provenance, the
composed texture and the front view, and the five routes of the saved-skin library.
The routes outside it are left alone even where they would be convenient — the
editor's autosave, the PNG import, the share image, the random draws, and the
`POST /credits` that would group the works of a whole stack in one call. The mod
groups them itself, out of the catalogue it already holds, rather than lean on a
route that may move.

**The project document is the site's, to the letter.** It is one shape everywhere:
composed by `POST /textures`, stored by `PUT /skins/{id}`, and read back from
storage. Its rules are the server's validator, and three of them are silent when
broken — the model is a `slim` boolean rather than a `model` string, a layer names
its element with `cat` and `preset`, and opacity and the adjustments are factors
rather than the whole percentages this mod's sliders work in. Writing them any other
way is refused with a 400 naming the path, which is how the first version of this
was found: nothing ever composed. `ProjectJson` is therefore the only place that
writes or reads a project, and it has a test per rule.

**The saved skins are per installation, not per account.** There is no account yet.
The storage routes ask for an `X-Client-Id` header, a UUID, and refuse the call
outright without one; the site draws it in the browser, and the mod draws it once and
keeps it in `config/mcskincreator-client.txt`. That file is the way back to the
library rather than the library itself — losing it leaves the skins on the server and
loses the door to them. The day accounts exist, one will gather several of these ids
without this side of the contract changing (issue #9).
