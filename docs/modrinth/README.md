# Modrinth listing

Everything needed to fill in the Modrinth project, in the order the site asks for it.
The texts live in this folder so they are reviewed and versioned like the code:

| File | Goes into |
|---|---|
| [`description.md`](description.md) | Settings → Description (Markdown) |
| [`changelog-0.1.0.md`](changelog-0.1.0.md) | The changelog of the first version upload |

Nothing here is read by the build, and `**.md` is ignored by `release.yml`, so editing
these files never cuts a release.

## 1. Creation form

| Field | Value |
|---|---|
| Type | Project |
| Name | `MC Skin Creator` |
| URL | `mc-skin-creator` |
| Owner | `clixmods` |
| Visibility | **Unlisted** while filling everything in, then switch to Public before submitting for review |
| Summary | see below |

**Summary** (256 characters maximum, this one is 152):

```
Build and edit your Minecraft skin from inside the game: layers, a live player preview, and one button to put it on your account.
```

## 2. Settings

| Field | Value |
|---|---|
| Categories | `Utility` (main). `Decoration` is a fair second one if you want it |
| Client / server side | Client: **Required** · Server: **Unsupported** |
| Loaders | Fabric |
| License | *Custom* — name `MC Skin Creator - Proprietary, source-available`, link `https://github.com/MC-Skin-Creator/mcskincreator-mod/blob/main/LICENSE` |
| Issues | `https://github.com/MC-Skin-Creator/mcskincreator-mod/issues` |
| Source | `https://github.com/MC-Skin-Creator/mcskincreator-mod` |
| Wiki | empty for now |
| Discord | empty for now |
| Donation links | empty for now |

The license matters: Modrinth's own rules allow a restrictive license as long as it is
stated honestly, and this one is. It is source-available, not open source, so do
**not** pick an OSI license from the dropdown and do not tick "open source" anywhere.

## 3. Description

Paste [`description.md`](description.md). It sticks to what the mod does today, states
that the 26.2 jar has not been run yet, and says where the session token goes, because
that is the first thing a Modrinth reviewer and a cautious player look for.

## 4. Icon and gallery

- **Icon**: `src/main/resources/assets/mcskincreator/icon.png` (128×128). Modrinth
  accepts it as is.
- **Gallery** — nothing exists yet. `./gradlew :1.21.11:test` writes
  `build/ui-preview/`, which is the editor rendered without the game, but a gallery is
  better served by real in-game screenshots. Suggested set, in this order (the first
  one, featured, is the picture shown in search results):
  1. The editor at a wide window: three columns, a skin on the model. *(featured)*
  2. The library with an element being tried on the model.
  3. The layers column with the inspector open.
  4. The **Skin Creator** panel on the title screen.
  5. The apply confirmation dialog.

  Give each a title and a one-line description; Modrinth shows them.

## 5. Versions

Modrinth takes one upload per Minecraft version, and the jar name already carries it.
Upload both jars of the same release, each as its own version:

| File | Version number | Name | Game version | Loader | Channel |
|---|---|---|---|---|---|
| `mcskincreator-0.1.0+mc1.21.11.jar` | `0.1.0+mc1.21.11` | `MC Skin Creator 0.1.0 (1.21.11)` | 1.21.11 | Fabric | Beta |
| `mcskincreator-0.1.0+mc26.2.jar` | `0.1.0+mc26.2` | `MC Skin Creator 0.1.0 (26.2)` | 26.2 | Fabric | Alpha |

- **Dependency** on both: `Fabric API`, *required*.
- The 26.2 jar goes out as **Alpha** until someone has run it in a real game — the
  README says the same thing, and the listing should not promise more.
- Changelog: [`changelog-0.1.0.md`](changelog-0.1.0.md), the same text on both.
- The `-dev.N` pre-releases from `develop` are **not** uploaded: they are untested
  previews and belong on GitHub only.

## 6. Before pressing "Submit for review"

- [ ] A stable release exists (`v0.x.y` from `main`) — Modrinth needs a version to review.
- [ ] Description, icon, license and links filled in as above.
- [ ] At least the first gallery picture.
- [ ] Project switched from Unlisted to Public (or leave Unlisted if a first look is wanted).
- [ ] `SECURITY.md` ([issue #13](https://github.com/MC-Skin-Creator/mcskincreator-mod/issues/13))
      if it is done by then: the description links to it.

## Afterwards

Automating the upload is [issue #15](https://github.com/MC-Skin-Creator/mcskincreator-mod/issues/15).
It needs the project id from the Modrinth settings page and a `MODRINTH_TOKEN` repository
secret with the *Create versions* scope, and it belongs in `release.yml`'s stable path
only. It is deliberately not part of this change.
