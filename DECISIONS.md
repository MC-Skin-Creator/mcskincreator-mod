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

## 3. Game version: 1.21.11

Pinned to a single version.

| Piece | Version | Why |
|---|---|---|
| Minecraft | `1.21.11` | Pinned target |
| Yarn mappings | `1.21.11+build.6` | Last Yarn build available (see the warning below) |
| Fabric Loader | `0.19.3` | Lowest version the mod is built and declared against |
| Fabric API | `0.141.6+1.21.11` | Latest build for this game version |
| Fabric Loom | `1.17.21` | Loom `1.18.x` requires a Java 25 JVM, because it targets the 26.x game versions |
| Gradle | `9.7.1` | Loom `1.17.21` declares a Gradle 9 plugin API; Gradle 8.x cannot resolve it |
| Java | `21` | What Minecraft 1.21.11 runs on |

**Yarn is a dead end past 1.21.11.** Mappings stop there: from game version 26.1
onwards there is no Yarn build at all, and those versions require Java 25. Moving
to a 26.x version therefore means three changes at once — official Mojang mappings
instead of Yarn (every `net.minecraft.*` import gets renamed), Java 25, and Loom
1.18.x. Worth planning for, not worth paying for today.

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

The entry point is **client only** (`"environment": "client"` in `fabric.mod.json`):
there is no server side to this mod, and declaring one would only make it refuse to
load on servers for no reason.
