# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Language rule

Everything in this repository is in **English**: code, comments, documentation,
commit messages, pull request descriptions. Do not write French into the
repository. User-facing strings are the exception and are translated through
`assets/mcskincreator/lang/` — never hardcode a display string in the code.

Commit subjects follow **conventional commits**: `type(scope): summary`, still
short, imperative and in English. The type is not decoration — it decides the next
released version number, see **Versioning and releases**. Do not add AI attribution
lines (`Co-Authored-By`, session links) to commits or pull requests.

Keep the commit body short: **5 lines maximum**. No exhaustive rationale, no
restating the diff line by line — a couple of sentences on the why is enough.

## What this is

A Fabric **client-side** mod that brings the MC Skin Creator skin editor into
Minecraft. There is no server side and no server entry point.

| | |
|---|---|
| Loader | Fabric |
| Supported Minecraft versions | `1.21.11`, `26.2` |
| Active / default version | `1.21.11` |
| Mappings | Official Mojang mappings on every target |
| Java | 21 on 1.21.11, **25** on 26.2 (Mojang's requirement, not a choice) |
| Mod id / package | `mcskincreator` / `fr.clixmods.mcsc.mod` |

Yarn mappings are **not** usable here: they have no build past 1.21.11, which is
why the whole project is on Mojang mappings.

### Source layout

```
src/main/java/fr/clixmods/mcsc/mod/
├── MCSkinCreatorClient.java   ClientModInitializer: logs on load, registers the menu buttons
├── remote/                    the HTTP side of the site's /api/v1 contract
├── catalog/                   what the catalogue says: regions, categories, elements, crops,
│                               and the ready-made models and outfits it offers
├── skin/                      pixels: front sprites, category and model sheets, the previewed
│                               skin, the blend behind a model's picture, textures
├── project/                   what is being edited: the layer stack and its history
├── scene/                     how the character is looked at: the camera, the backdrop,
│                               the animations in the game's own terms, the render state
│                               the game draws from, and the game camera the two world
│                               views borrow
├── account/                   the Mojang upload, and the only code that holds the session token
├── mixin/                     the one mixin: the applied skin, worn before Mojang propagates it
├── style/                     the design system: palette, metrics, the four materials, the grain
└── ui/
    ├── Canvas.java            the drawing surface, as an interface: everything paints through it
    ├── GameCanvas.java        the implementation that paints on the game, and the only file the interface versions
    ├── ScreenCompat.java      the two call renames outside drawing
    ├── Element.java, Paint.java   the widget base and the per-frame context
    ├── EditorChrome.java      where the four zones go: columns, drawers, paint order, hit list
    ├── EditorScale.java       the GUI scale this screen takes for itself, and gives back
    ├── SkinCreatorScreen.java the editor: project, catalogue, requests, windows, input
    ├── Figure.java, PlayerFigure.java   the player in the scene, and the game's way of drawing one
    ├── MenuButtons.java, SkinPanel.java   the entry on the vanilla menus
    ├── widget/                button, tabs, tile, field, slider, checkbox, dropdown, layer row
    ├── panel/                 top bar, library, scene, layers
    └── window/                the modal base and the windows built on it
```

One shared source tree serves every Minecraft version. There is no `src-1.21.11/`
and there never should be.

`src/test/java/` holds the tests, and is shared the same way: every target compiles
and runs all of them. They cover the pure logic — the layer stack and its history,
the project body sent to the server, the catalogue reader and its fallbacks — and
need no game, though they do resolve against the target's Minecraft jar for
`Component` and `PlayerModelType`. Nothing that draws is tested: that is what
running the game is for.

The interface has its own rules — the four materials, the palette, the scale it takes
for itself, what is deliberately not built — in [`INTERFACE.md`](INTERFACE.md). **Read
it before changing anything that draws**: the short version is that every surface is
one of four materials in `style/Surface.java`, every colour is a line of the site's own
stylesheet transcribed into `style/Palette.java`, and naming a colour anywhere else is
the one thing not to do.

**The editor can be rendered without the game.** `./gradlew :1.21.11:test` writes
`build/ui-preview/`: the whole screen at the sizes a window actually produces, in
French because that is the language the mod speaks longest, drawn with the real font
read out of the Minecraft jar on the test classpath. `materials.png` beside it is every
material at every height it is used at. Look at it before and after changing anything that
draws — it is the only way to see this screen in this repository, and it is faster than
a client either way. `src/test/java/fr/clixmods/mcsc/mod/preview/` is how it works, and
what keeps it working is that nothing in `ui/` reaches for `Minecraft.getInstance()`:
the scene is handed a `Figure`, the top bar is handed its mark.

## Stonecutter architecture

Stonecutter (0.9.8) compiles the one shared source tree against several Minecraft
versions. **Git branches track work** (`main`, `feature/*`, `fix/*`); **Stonecutter
tracks Minecraft compatibility**. Never create a Git branch to support a Minecraft
version.

| File | Role |
|---|---|
| `settings.gradle.kts` | Declares the Stonecutter targets and the plugins: Stonecutter, `loom-back-compat`, foojay toolchain resolver |
| `stonecutter.gradle.kts` | Holds the active version (`stonecutter active "…"`) — **rewritten by the switch task, do not edit by hand** |
| `stonecutter.properties.toml` | Every mod and per-version value: mod identity, Fabric Loader, Loom variant version, and one table per target |
| `build.gradle.kts` | Applied to each target: Java level per version, Mojang mappings, dependencies, resource processing, `buildAndCollect` |
| `versions/` | Generated per-target build output. Git-ignored, never committed |

`dev.kikugie.loom-back-compat` picks the Loom variant per target: Minecraft below
26 is obfuscated and uses `fabric-loom-remap`, 26+ ships unobfuscated and uses
`fabric-loom`. The `loomx.loom_version` property in the TOML pins that line to
`1.17-SNAPSHOT`; the 1.18 line refuses to run below a Java 25 JVM, which would
force Java 25 on every target.

Gradle itself runs on Java 21 and downloads the Java 25 toolchain for the 26.2
target on its own.

## Commands

All of these were run and verified in this repository.

```sh
./gradlew build                        # builds and tests EVERY target
./gradlew :1.21.11:build               # builds and tests one target
./gradlew :26.2.x:build
./gradlew test                         # every target's tests, nothing else
./gradlew :26.2.x:test                 # one target's tests
./gradlew buildAndCollect              # all targets, jars collected in build/libs/0.1.0/
./gradlew tasks                        # lists the generated Stonecutter tasks
```

`buildAndCollect` copies the jar and nothing more: it does not run the tests, which
is why CI asks for `build` as well as `buildAndCollect`. A test report lands in
`versions/<target>/build/reports/tests/test/`.

Switching the active version (note the spaces — the task name is a sentence):

```sh
./gradlew "Set active project to 26.2.x"
./gradlew "Set active project to 1.21.11"
```

Switching rewrites the source files in place, flipping which branch of each
versioned comment is live, and updates `stonecutter.gradle.kts`. A round-trip
restores the tree exactly. **Leave the active version on `1.21.11` in commits**
unless you intend to change it — an accidental switch shows up as a diff across
every conditional in the repository.

`./gradlew runClient` launches a development game, and takes the active version.
It has never been run in this environment (no GPU); treat it as unverified.

## Compatibility rules

In order of preference:

1. **Same API on every target → write plain shared code.** Do not add a
   Stonecutter directive "just in case".
2. **Small structural difference → a versioned comment**, as small and as local
   as possible.
3. **A pure rename used in several places → put it in `ScreenCompat`** rather than
   scattering the same conditional around.
4. **Genuinely different implementations → duplicate the smallest possible part.**
   Last resort.

Never solve a compile-time API difference with a runtime version check
(`if (version.startsWith("26"))`): the losing branch cannot compile on the other
target. That is exactly what Stonecutter exists for.

### The syntax used here

The inactive branch is kept as a block comment; the active branch is live code.
Real example from `ScreenCompat.java`, with `1.21.11` active:

```java
//? if >=26.1 {
/*client.setScreenAndShow(screen);
*///?} else {
client.setScreen(screen);
//?}
```

**Never reformat, re-indent, auto-format or "simplify" these comments**, and never
let a bulk refactor rewrite them. The `/*`, the `*///?` and the placement of `{`
and `}` are load-bearing: breaking them silently changes what a target compiles.

### Known 26.x differences already handled

| Concern | 1.21.11 | 26.2 | Where |
|---|---|---|---|
| Open a screen | `Minecraft#setScreen` | `setScreenAndShow` | `ScreenCompat` |
| Screen widget list | `Screens#getButtons` | `Screens#getWidgets` (Fabric screen API v5) | `ScreenCompat` |
| Screen draw hook | `render(GuiGraphics, …)` | `extractRenderState(GuiGraphicsExtractor, …)` | `SkinCreatorScreen` |
| Drawing object | `GuiGraphics` | `GuiGraphicsExtractor` | `Canvas` |
| Draw a string | `drawString` / `drawCenteredString` | `text` / `centeredText` | `Canvas` |
| Draw a widget | `Renderable#render` | `Renderable#extractRenderState` | `Canvas` |
| Centered text | `drawCenteredString` | `centeredText` | `SkinCreatorScreen` |
| Draw an entity | `GuiGraphics#submitEntityRenderState` | `GuiGraphicsExtractor#entity` | `Canvas` |
| Screen backdrop hook | `renderBackground(GuiGraphics, …)` | `extractBackground(GuiGraphicsExtractor, …)` | `SkinCreatorScreen` |
| Hide the game HUD | `Options.hideGui` | `Gui.hud.toggle()` / `isHidden()` | `scene/GameCamera` |

26.x replaced immediate-mode GUI drawing with a render-state extraction pass, so
any new drawing code will need the same treatment.

Not every 3D route across the two is a rename. The GUI's **skin** route changed the
type of its first parameter (`PlayerModel` on 1.21.11, `Model.Simple` on 26.2), which
is a real incompatibility; the **entity** route differs only in the method name, which
is why the scene goes through that one. See `DECISIONS.md`, "The figure goes through
the entity route".

## Before changing Minecraft-facing code

1. Check whether the API differs across the supported versions — the Minecraft jar
   for each target is unobfuscated or Mojang-mapped, so `javap` on the jar in
   `~/.gradle/caches/fabric-loom/` answers the question definitively.
2. Read the existing conditionals around the code you are touching.
3. Prefer a shared implementation.
4. Add version-specific code only for a real incompatibility.
5. **Build every target** (`./gradlew build`). A change is not done because the
   active target compiles.

## Mixins

There is **one**, and the bar for a second is high: everything else the mod does, it
does through public API. `mixin/AbstractClientPlayerMixin` takes the return of
`AbstractClientPlayer#getSkin` so the player wears the skin they just applied without
restarting the game. Every other way in is private on at least one target —
`SkinManager#registerTextures` is package-private on 1.21.11 and private on 26.2,
`PlayerInfo#skinLookup` is private on both, all checked with `javap`.

It covers what is drawn from a player **entity**, and only that. A menu has no player
entity: `SkinPanel` asks `SkinManager#createLookup` for a supplier, so it takes the
same override through `AppliedSkin.over(…)`, which wraps that supplier. Anything else
that comes to draw the local player's skin needs one door or the other — the mixin is
not a catch-all, and forgetting this is how the title screen kept showing the old skin
after the rest of the game had moved on.

Two things about the setup are worth knowing before touching it:

- **No refmap, and none is needed.** Loom rewrites the annotation itself when it remaps
  the jar: `method = "getSkin"` comes out as `method_52814` in the 1.21.11 jar and
  stays `getSkin` in the 26.2 one, which ships unobfuscated. Verified by unzipping both
  jars, not assumed. If you add a mixin, check the same way rather than trusting it.
- **`compatibilityLevel` is expanded, not written.** `mcskincreator.mixins.json` says
  `JAVA_${java}` and `processResources` fills it in per target, because the mixin
  classes are Java 21 bytecode on one target and Java 25 on the other and Mixin checks
  the class file version against that level. A hardcoded level is wrong on one of them.

`getSkin` has the same signature on both targets, so the mixin itself carries no
Stonecutter directive. One that needed one would be an argument for solving the problem
another way.

## Dependencies

Per-version dependency versions live in `stonecutter.properties.toml`, never
hardcoded in the build scripts. Before adding a dependency, check it exists for
every target and give each target its own version if they differ. Do not downgrade
a dependency globally to satisfy the oldest target.

The Fabric API is pulled per module (`fapi("fabric-screen-api-v1")` in
`build.gradle.kts`) rather than whole, so each target only fetches what the mod
uses. Add modules there as they become necessary.

JUnit 5 is one line — `deps.junit` at the top of the TOML rather than in a per-target
table — because the same version runs on Java 21 and on Java 25 alike. It is pulled
through the JUnit BOM, so the engine, the parameterised runner and the launcher all
follow that one number.

## Adding a Minecraft version

1. Add the node in `settings.gradle.kts` (`versions(…)` or `version(alias, value)`).
2. Add its table to `stonecutter.properties.toml`: `mod.mc_compat`,
   `mod.mc_releases`, `deps.fabric_api`.
3. Extend `requiredJava` in `build.gradle.kts` if that version needs a different
   Java level.
4. Add the version to the CI matrix in `.github/workflows/build.yml`.
5. `./gradlew :<version>:build`, then fix what the compiler reports, keeping shared
   code shared.
6. `./gradlew build` — every target, not just the new one.

## CI

`.github/workflows/build.yml` runs **one job per Stonecutter target** on every pull
request and on pushes to `feature/**` and `fix/**`, with `fail-fast: false` so one
broken version does not mask another. Each job compiles that target and runs its
tests, uploads the test report when something failed, and uploads its jar as a
**workflow artifact** (`retention-days: 7`) so a PR can be test-installed before it
merges — this is not a release: no tag, no GitHub release,
`mod.version` unchanged. It deliberately does not run on `main` or `develop`, where
`release.yml` compiles the same commit anyway. No secret is declared.

## Branch model

Trimmed Git Flow. Git branches track **work**; Stonecutter tracks **Minecraft
compatibility**. The two are orthogonal, and a branch never represents a game
version.

```
feature/*  fix/*  ──►  develop  ──►  main
                          │            │
                     pre-release    release
                    v0.2.0-dev.7     v0.2.0
```

- `main` — stable only. The default branch. A push here can cut a release.
- `develop` — integration. Every push produces a pre-release build, except a
  doc-only one.
- `feature/*`, `fix/*` — where work happens. Pull requests target `develop`.
- `release/*` and `hotfix/*` do not exist yet. They stabilise a version while
  development continues elsewhere, which is not yet a problem this project has.

**Never commit or push directly to `main` or `develop`, and never merge a pull
request into either.** All work, including work done by an AI agent, happens on
a `feature/*` or `fix/*` branch and ends with an open pull request targeting
`develop` (`main` only for a hotfix). Opening the pull request is the end of the
task — merging is a human decision, made after review, every time, no matter how
small the change or how green the CI.

After a stable release, merge `main` back into `develop` so it picks up the version
bump commit.

## Versioning and releases

Semantic versioning, with one rule on top: **the Minecraft version never touches
the mod version.** Supporting a new game version is not a mod release; compatibility
is carried by the artifact name. Mod version and Minecraft version are independent
numbers that happen to appear next to each other.

### How the number is decided

`.github/scripts/next-version.sh` derives it from the conventional-commit subjects
since the last stable tag. Run it locally to see what the next release would be.

| Commits since the last stable tag | Result |
|---|---|
| `feat!:` or a `BREAKING CHANGE:` footer | minor while below 1.0.0, major above |
| `feat:` | minor |
| `fix:`, `perf:` | patch |
| only `docs`, `chore`, `ci`, `refactor`, `test`, `build` | **no release at all** |

Below `1.0.0` a breaking change bumps the minor: there is no major to bump yet.
**`1.0.0` is never computed.** It is cut by hand, through the release workflow's
`workflow_dispatch` input, when the mod actually edits and applies a skin — the V1
described in the issues. Everything before that stays `0.x`.

### What the workflows do

`.github/workflows/release.yml`, on every push except a doc-only one (`paths-ignore`
skips `**.md` and `LICENSE`):

| Branch | Version | Result |
|---|---|---|
| `main` | derived from the commits | tag `vX.Y.Z`, GitHub release, every target's jar attached, `mod.version` committed back to `main` |
| `develop` | next version + `-dev.<run number>` | GitHub **pre-release**, jars attached, nothing committed |

A merge into `main` whose commits earn nothing produces no release and no noise.
A merge into `develop` always produces a build, so there is always a permanent link
to the latest state — `-dev.` builds are previews and are not tested.

`-dev.7` sorts below the `X.Y.Z` it previews, which is what it is: a preview of the
next release, not a patch on the last one.

`.github/workflows/build.yml` is unrelated to releases: it is the per-target matrix
that checks pull requests and work branches. Its jars are short-lived workflow
artifacts for testing a PR, not a release artifact.

### Things worth knowing before touching this

- **`stonecutter.properties.toml` wins over `-P` properties.** `-Pmod.version=…`
  is silently ignored — verified, not assumed. The release workflow rewrites that
  one line in the runner's working copy before building. Do not replace that with a
  command-line property.
- The version commit is pushed with `GITHUB_TOKEN`, and GitHub does not start a new
  workflow run for such a push, so the release workflow cannot loop.
- Every jar of a release comes from **one commit**: one source tree, one version,
  one jar per supported Minecraft version.

### Artifact naming

```
mcskincreator-<mod version>+mc<minecraft version>.jar
```

e.g. `mcskincreator-0.1.0+mc1.21.11.jar`, `mcskincreator-0.1.0+mc26.2.jar`, and for
a development build `mcskincreator-0.2.0-dev.7+mc1.21.11.jar`.

Publishing to Modrinth/CurseForge is still not set up; building and publishing are
separate concerns (see issue #15).
