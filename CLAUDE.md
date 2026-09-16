# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Language rule

Everything in this repository is in **English**: code, comments, documentation,
commit messages, pull request descriptions. Do not write French into the
repository. User-facing strings are the exception and are translated through
`assets/mcskincreator/lang/` — never hardcode a display string in the code.

Commit messages are short and imperative. Do not add AI attribution lines
(`Co-Authored-By`, session links) to commits or pull requests.

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
└── ui/
    ├── MenuButtons.java       adds the button to TitleScreen and PauseScreen via ScreenEvents.AFTER_INIT
    ├── SkinCreatorScreen.java the editor screen (placeholder content for now)
    └── ScreenCompat.java      the only home for cross-version call renames
```

One shared source tree serves every Minecraft version. There is no `src-1.21.11/`
and there never should be.

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
./gradlew build                        # builds EVERY target
./gradlew :1.21.11:build               # builds one target
./gradlew :26.2.x:build
./gradlew buildAndCollect              # all targets, jars collected in build/libs/0.1.0/
./gradlew tasks                        # lists the generated Stonecutter tasks
```

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
| Centered text | `drawCenteredString` | `centeredText` | `SkinCreatorScreen` |

26.x replaced immediate-mode GUI drawing with a render-state extraction pass, so
any new drawing code will need the same treatment.

## Before changing Minecraft-facing code

1. Check whether the API differs across the supported versions — the Minecraft jar
   for each target is unobfuscated or Mojang-mapped, so `javap` on the jar in
   `~/.gradle/caches/fabric-loom/` answers the question definitively.
2. Read the existing conditionals around the code you are touching.
3. Prefer a shared implementation.
4. Add version-specific code only for a real incompatibility.
5. **Build every target** (`./gradlew build`). A change is not done because the
   active target compiles.

## Dependencies

Per-version dependency versions live in `stonecutter.properties.toml`, never
hardcoded in the build scripts. Before adding a dependency, check it exists for
every target and give each target its own version if they differ. Do not downgrade
a dependency globally to satisfy the oldest target.

The Fabric API is pulled per module (`fapi("fabric-screen-api-v1")` in
`build.gradle.kts`) rather than whole, so each target only fetches what the mod
uses. Add modules there as they become necessary.

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

`.github/workflows/build.yml` runs **one job per Stonecutter target** on every push
to `main`, `develop`, `feature/**` and on every pull request, with `fail-fast:
false` so one broken version does not mask another. Each job uploads its own jar.
No secret is declared.

## Releases

Mod version and Minecraft version are separate. The mod version lives in
`stonecutter.properties.toml` (`mod.version`); the Minecraft version comes from the
target. Artifacts are named:

```
mcskincreator-<mod version>+mc<minecraft version>.jar
```

e.g. `mcskincreator-0.1.0+mc1.21.11.jar` and `mcskincreator-0.1.0+mc26.2.jar`. A
Minecraft compatibility update is not a mod version bump. One commit produces the
jars for every supported version.

Publishing to Modrinth/CurseForge is not set up; building and publishing are
separate concerns (see issue #15).
