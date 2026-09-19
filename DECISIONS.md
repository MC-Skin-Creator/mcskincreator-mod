# Technical decisions

The four decisions of issue #1, plus the toolchain versions that follow from them.
Everything here was checked against `meta.fabricmc.net`, `maven.fabricmc.net` and
Mojang's version manifest, not copied from a tutorial.

## 1. Skin level

Three levels were on the table:

| Level | What it does | Who sees the skin | Status |
|---|---|---|---|
| 1. Fitting room | the mod replaces the skin texture locally | you only | not built, issue #11 |
| 2. Mojang upload | upload to the account through the official API | everyone, everywhere | **built** |
| 3. Server mod | rewriting the `textures` property server-side | everyone on that server | out of scope, another repository |

**Level 2 shipped before level 1**, which reverses the order this file first set out.
The reason is worth recording, because the original order was not wrong so much as
answering a different question: level 1 was first because it is the safe one, not
because it is the one a player wants. What makes the mod a product is the skin
actually changing, and level 1 is a mirror — every hour of it would have been spent
explaining to players that the change they just made is not a change. Level 2 needs
no such explaining, and it turned out not to depend on level 1 for anything: the
composed sheet already existed, because export writes it to a file.

Level 1 is still worth having, and it is still issue #11. Its value is what it always
was, only smaller than it looked: trying a skin on without spending an upload against
Mojang's rate limit, and working offline.

Players **will** mistake level 1 for a real skin change when it lands. The interface
has to spell the difference out, or every attempt will look like a bug — and now that
level 2 exists to be confused with, that is no longer a hypothetical.

### What level 2 costs, and what pays for it

The session token is the account. Three rules hold the whole design, and each is one
a reader of this repository can check rather than take on trust:

- **One address.** The token goes to `api.minecraftservices.com` and nowhere else,
  from one file — `account/MojangSkins.java`. Nothing outside `account/` can reach it:
  `GameSession` is package-private and `AccountSkin` is the only way in. `MojangSkinsTest`
  pins the endpoint so that changing it has to be argued for in a diff.
- **Never in a log.** `SkinUploadException` carries a status code and a `Retry-After`,
  and deliberately nothing of the request. A failure ends up in crash reports, and a
  crash report is the likeliest way a token escapes a mod that meant well.
- **Never on its own.** One press, one upload. The mod has no timer, no batch and no
  retry, and `UploadCooldown` locks the button between two uploads — with Mojang's own
  `Retry-After` honoured when it sends one.

The cooldown's floor after a success is the mod's own number, not a published limit:
Mojang documents no rate for skin changes. A generous floor costs a player nothing —
the CDN takes longer than the floor does to show the change anyway — and a session
rate-limited by an impatient click costs them ten minutes.

### Wearing it before Mojang has propagated it

An upload changes the account, and the account is not what a running client draws. The
profile the client was handed on joining still carries the old `textures` property, and
Minecraft's caches sit on top of that, so the new skin used to appear only on a
restart — a minute in which a successful upload is indistinguishable from a failed one.

The mod closes that minute by wearing the uploaded pixels itself: `skin/AppliedSkin`
holds them, and they reach the screen through **two doors**, because the game draws the
player's skin two ways. The mod's one mixin takes the return of
`AbstractClientPlayer#getSkin`, which covers everything drawn from a player entity; the
panel on the title and pause menus has no entity and asks `SkinManager` for a supplier
instead, so it wraps that supplier with `AppliedSkin.over(…)`. Missing the second door
is what left the menu preview showing the old skin while the player in the world already
wore the new one. Wrapping the supplier rather than its result is deliberate:
`PlayerSkinWidget` keeps the supplier and calls it as it draws, so a panel built before
the upload updates without being rebuilt.

Three further choices are deliberate:

- **After the upload, never before.** A client wearing a skin that failed to send would
  be a worse lie than the wait it replaces.
- **The body only.** The cape and the elytra stay the ones the game resolved, because
  they belong to the account and this mod has not touched them. An override that
  dropped them would take a player's cape off to show them a skin.
- **It lasts the session.** There is no reliable moment at which the real profile can be
  seen to have caught up — the client is not told, and its copy is not refreshed until
  it reconnects — so rather than guess at one, the override stands. It cannot drift:
  the pixels are the ones the account now holds, and the only thing that replaces them
  is another upload, which replaces the account skin in the same breath.

Editing the game's state instead — clearing the entity's cached `PlayerInfo`, as is
sometimes suggested — does not work and is worth writing down so it is not tried again:
the profile it would be rebuilt from still carries the old texture property, so the
game would resolve the old skin a second time.

**Other players are out of scope, and not by preference.** Their clients read the
profile from the server; a client-side mod cannot make them refresh. Doing it properly
needs something server-side, which is level 3 above and another repository. Until then
the honest thing is to say so in the interface, which is what the confirmation does.

`PlayerModelType` goes up with the sheet as Mojang's `classic` or `slim`, so the arms
are the width the skin was drawn for. The upload needs a PNG, and the composer answers
either a PNG or a raw RGBA buffer; `NativeImage` on both target versions can read a PNG
and write one to a *file*, but has no call that hands back the bytes — so `skin/Png.java`
encodes the raw shape. Sixty lines of well-specified format beat a temporary file
between two buffers already in memory.

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

The mod brings the site's editor into the game. What it takes from the site is the
arrangement — three zones, a fixed row of category tabs, the stack grouped by region,
the gestures. What it takes from Minecraft is the paint. The rules are in
[`INTERFACE.md`](INTERFACE.md); what belongs here is the three decisions that shaped
the code.

**Every surface is one of the game's own sprites.** The site hand-draws its panels,
slots and buttons out of bevels, because a web page has nothing else to build them
from. A mod does, and using them buys three things a copy cannot: the player already
knows what a Minecraft button and slot mean, a resource pack that restyles the game
restyles this screen with it, and a sprite that changes in a future version changes
here too. So the editor names no material colours, ships no GUI textures and draws no
icon the game does not already have. Where the game has no sprite for something the
site says with colour, it is said with the game's own words instead: a checkbox for a
yes, a selected tab for one choice of several, the right mouse button for a second
action. The mark is `assets/mcskincreator/icon.png` — the icon the mod already ships,
because there is one MC Skin Creator logo and a second one drawn for this screen
would drift from it.

**Drawing goes through one object.** 26.x replaced immediate-mode GUI drawing with a
render-state extraction pass: the graphics object changed name and so did its text
calls. Everything else the mod draws with turned out to be the same call on both
targets — `fill`, `blit`, `blitSprite`, the scissor stack, tooltips, `NativeImage`,
`DynamicTexture`, `TextureManager`, `Identifier`, `RenderPipelines`, and every input
event, all checked with `javap` against both jars. So `Canvas` wraps that one object
and is the only file in the interface that names a Minecraft version.

**The widgets are the mod's own, drawn with the game's sprites.** Half of these
controls — a category tab, an element thumbnail, a layer row — have no vanilla
equivalent to subclass, and the ones that do need behaviour vanilla's lack: a label
that scrolls when it is too long, a row whose actions are out of reach until it is
pointed at. What stays the game's is what the game is right about: its font, its GUI
scale, its sprites, the player model, and a focus ring a keyboard can walk. That last
one is not decoration — hovering exists with a mouse and not with a controller, and
the site reveals real functionality on hover, so whatever holds the focus draws as
whatever is hovered.

**The interface owns almost no pixels of its own.** The figure is drawn by the game
from a render state the mod fills in, the thumbnails are folded out of the category
atlases the API already serves, and the stack being edited is composed by the server. One
texture is drawn from nothing: the stone grain under the panels, from deterministic
noise, because a tile generated from a function cannot go out of step with the palette
it is tinted by and a PNG of the same thing can.

The one exception is the pictures of the ready-made stacks — the starter models and
the outfits — which the mod stacks and folds itself (`Composite`, `ReadyMadeSkins`).
The catalogue offers two hundred and odd of them, and asking the server for two hundred
compositions to fill one panel is not a thing to do to a service, or to a player waiting
on it. The blend copies the server's, half rounded to even like the
`Uint8ClampedArray` the site composes into, so the picture is the one the stack will
actually produce. They are then a `CategorySprites` sheet like any other, so the library
draws them with the tile it already had.

The layer stack is the mod's, and it follows the server's format rather than
inventing a second one: the project validator is the authority on what a project is,
and two representations of the same thing would drift. `ProjectJson` writes it in one
method for that reason.

### The figure goes through the entity route, not the skin route

The game offers two ways to put a player in a GUI, and only one of them is the same
call on both targets. Checked with `javap` against both jars:

| | 1.21.11 | 26.2 |
|---|---|---|
| entity | `GuiGraphics#submitEntityRenderState(EntityRenderState, float, Vector3f, Quaternionf, Quaternionf, int, int, int, int)` | `GuiGraphicsExtractor#entity(…, Vector3fc, Quaternionfc, Quaternionfc, …)` |
| skin | `submitSkinRenderState(PlayerModel, …)` | `skin(Model.Simple, …)` |

The skin route changes the *type* of its first parameter — a `PlayerModel` is not a
`Model.Simple` — which is a real incompatibility rather than a rename. The entity
route differs only in the method name, so it costs one versioned comment in `Canvas`
and nothing anywhere else. It is also the more capable of the two: the scale is the
zoom, the translation is the pan, the quaternions are the tilt, and the animation
comes off the render state instead of having to be applied to a model by hand.

Two facts read out of the game rather than assumed, both of which the design rests on:

- **an `AvatarRenderState` is dispatched on its skin**, not on an entity type
  (`EntityRenderDispatcher#getRenderer`), and the skin's model type is what picks the
  classic or the slim renderer. So the figure can be drawn with no entity at all,
  which is what the title screen has;
- **a plain `new AvatarRenderState()` is already usable**: full-bright light, scale
  one, standing pose, empty hands, every overlay shown. That is why a fresh one is
  built each frame instead of one being kept and reset — a kept state is a list of
  fields to remember to clear, and the one forgotten leaves the last pose's crouch on
  the next pose.

The camera numbers themselves are vanilla's, from
`InventoryScreen#renderEntityInInventoryFollowsMouse`, down to the signs: the figure's
turn is `bodyRot` in degrees, the tilt is a quaternion multiplied into the base flip,
and the translation is applied *before* the rotation and so is unaffected by it —
positive y is down on screen. Every one of those is a chance to send the figure off
the side of the panel, and none of them can be checked in a screenshot.

### The local player is only drawn when they are the camera

The in-game view was first built the obvious way: borrow the game's camera, fly a
client-side marker around the character, let the game render the world. It showed an
empty world. The reason is a rule in `LevelRenderer.renderLevel`, and it is not
negotiable through any setting:

```java
if (entity instanceof LocalPlayer && camera.entity() != entity) continue;
```

**Your own character is drawn only while they are the camera entity.** Put the camera
anywhere else and they are skipped — and a camera that *is* them is either inside their
head (first person) or locked to vanilla's fixed third-person distance. Reaching past
that means injecting into the entity loop of the largest method in the renderer, which
is exactly the kind of second mixin this file argues against.

What this rules out is a *free* camera, not third person. Third person renders the
character because the camera is **detached**, not because it has moved: the camera entity
is still the player. So the in-game view is the game's own third person, and going round
the character is done by turning their view — the camera sits behind wherever they look —
with their body pinned so they keep facing the way they were. That is real game state:
yaw, pitch and body yaw are saved on the way in and put back on the way out, and in
multiplayer other people see the character turn. There is no zoom, because vanilla fixes
its own third-person distance and exposes no way to change it.

Compositing the figure over the world was tried in between, and it is why
`scene/SceneBackdrop` exists rather than being deleted: it is the right answer to a
different question. The in-game view shows the **real** character, which cannot be posed;
putting the live world *behind the workshop figure* is what keeps the animations, the
zoom and the pan available with a landscape behind them.

Two things had to stop painting over the world for this to be visible at all, and both
were hiding it completely rather than partly:

- **vanilla's screen backdrop.** `Screen.renderBackground` blurs what is behind and then
  lays the opaque tiled menu background over it. `SkinCreatorScreen` now overrides it to
  draw nothing — the editor has always painted its own backdrop, so the only thing lost
  is the blur;
- **the editor's own panels.** The first-person arm is drawn low and to the right, which
  is where the layers panel sat. Both side panels fold themselves away under a camera
  that overlays the game, and the player's own fold choice is put back on the way out.
  A game *backdrop* does not fold them: that is scenery, and taking the catalogue off
  the screen in the middle of picking from it would be a poor trade;
- **the game's own HUD.** The hotbar, the hearts and the crosshair are drawn over the
  world whether or not a screen is open. Hidden while a world camera holds, put back on
  release. The flag moved between the targets — `Options.hideGui` on 1.21.11,
  `Gui.hud.toggle()` behind `isHidden()` on 26.2 — and both are public, so it stays a
  rename rather than a reason for a mixin.

  **But one flag covers the HUD and the hand.** Both targets guard the
  `ItemInHandRenderer` call with the same boolean they guard the HUD with — read out of
  the bytecode of `GameRenderer.renderItemInHand` on both jars, not assumed. So hiding
  the HUD in the first-person view hid the arm, the one thing that view exists for, and
  the first version shipped keeping the HUD instead, because the alternative was keeping
  no arm.

  **That was the wrong half to give up**, and it is what `GuiMixin` is for. The flag is
  a setting the player owns — it is F1 — and it is now only used where the hand is
  unwanted too: a world backdrop, where it is exactly how the arm is got rid of.
  Everywhere else the drawing is skipped for the frame instead, which is the half of
  that flag the editor actually wanted. The two targets draw the HUD through different
  names but from the same class, so it stays one mixin with a versioned injection: 1.21.11
  cancels `Gui.render`, which is the HUD and nothing else, and 26.2 forces the first
  boolean of `Gui.extractRenderState` to false, because that one gates the HUD while the
  second gates the screen — cancelling there would take the editor with it.

### The head cannot be tilted away from the camera

The in-game orbit turns the character's view, and the head has to stay put while it does
— a head that swivels to follow the camera is the one thing that makes the view read as
a bug. Yaw is easy: pin `yHeadRot` and `yBodyRot` to where they were, and re-pin them
every tick, because a tick pulls the head back towards the view and the body after it.

Pitch is not, and cannot be: **the camera's pitch and the head's pitch are the same
field.** The camera reads `Entity.xRot` through `getViewXRot`; the model reads it off the
render state. Tilting one tilts the other. So the pitch is held level and the orbit is
horizontal only — a vertical drag does nothing in that view. The workshop camera, which
owns its own angles, keeps its tilt.

### The second mixin: posing a character who is in the world

The in-game view draws the real character inside the world, and that is exactly why the
animation chooser could not reach it: the workshop figure is a render state the mod
builds, and a character in the world is a render state the game extracts from a live
entity. There is nothing to hand over.

Two ways in, and they are not close. **Animating the entity** is real game state: written
every frame, sent to the server, seen by everybody on it, and left behind if the client
goes down between setting it and putting it back — somebody permanently crouched in
their own save. **Taking the render state on its way out** changes a picture, on one
client, for as long as one window is open; nothing leaves the machine and nothing
survives the window closing.

So `mixin/AvatarRendererMixin` injects at the return of
`AvatarRenderer#extractRenderState`, reads `scene/WorldPose`, and applies the pose when
the avatar being drawn is the one the editor is looking at. It clears the animation
fields first, because a state filled in from an entity arrives carrying whatever that
entity was really doing.

It pays for itself twice over: it is also what fixed the head. On the entity the camera's
pitch and the head's pitch are the same field, so tilting the view tilted the head — the
one thing that view must not do. On the render state they are two fields, so the head is
simply pinned and the vertical orbit came back.

Three things were checked rather than assumed, and the third is the one a future mixin
should copy: the method is public with the same signature on both targets (so no
Stonecutter directive, the repository's own test of whether a mixin is the right answer);
it has **three** overloads, so the injection spells out the full descriptor; and the
remap was read out of both built jars, where `extractRenderState(…Avatar;…F)V` comes out
as `method_62604(Lnet/minecraft/class_11890;Lnet/minecraft/class_10055;F)V` on 1.21.11 —
Loom rewrites the parameter types, not only the name.

### What a backdrop is, and what it is not

The world behind the workshop figure is a **backdrop**: the game draws the world, and the
figure is a GUI element drawn over it, after the world pass and after any shader pack has
had its say. It will not take world lighting, world shadows or shader effects, and
nothing short of rendering it inside the level pass would change that. The view that puts
the character *in* the world, shaders included, is the in-game one — which is the whole
reason both exist, and which now animates too, through the mixin above.

### The editor pauses a single-player game

`isPauseScreen()` returned false, which nobody noticed while the scene was a figure on a
flat panel. It became obvious the moment a camera looked at the real world: mobs closing
in behind the editor while a hat was being chosen. Somebody editing a skin is not
playing, and the pause is not theirs to lose.

Screens tick regardless of the pause (checked against `Minecraft.tick`, where the
`screen.tick()` call sits outside the pause guard), so the composition debounce, the
search and the camera all keep running. What does stop is the character's own animation
— which is why the first-person swing plays out on a server and stands still at home.
That is the right way round: nobody wants to be eaten for the sake of a wave.

### Wearing the edit in the world, and why that is not level 1

The two in-game cameras are only worth having if the character is wearing what is
being drawn, and nothing else can put it there: the profile the client joined with
still describes the old skin. So `AppliedSkin` carries a second override beside the
one that follows an upload — the editor's own preview, on the local player, on this
client, for as long as the editor is open, through the same door and so through the
existing mixin.

It is a slice of level 1 above, which this file says is not built, and the warning
there still stands: a local override looks exactly like a real skin change. What
disarms it here is the scope, and the scope is the whole design — it lasts the window,
it is dropped in the screen's teardown so the game closing the editor ends it too,
nothing is sent anywhere, and nobody else sees it. It must not grow: a fitting room
that outlives its window is issue #11 and wants the interface work issue #11
describes.

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
