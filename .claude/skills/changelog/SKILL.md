---
name: changelog
description: Write or update the mod's user-facing changelog (changelog/en.md, fr.md, es.md). Use whenever a task adds, fixes or improves something a player can see or feel in the game, before opening the pull request, or when asked to write the changelog, prepare a release or correct an entry.
---

# The changelog

It is written for **the players who use the mod**, not for developers and not for
the author. Three files, one per language: `changelog/en.md` (the reference, written
first), `fr.md` and `es.md`. They carry **the same versions, the same headings and
the same number of bullets, in the same order**. A pull request that touches one
touches all three. The website shows these files on its changelog page (the "Mod"
tab), so the format below is a contract: do not improvise it.

## What goes in, and what never does

**In:** what a player sees or feels — a feature, a setting, an export, a supported
loader or Minecraft version, a bug fixed, the editor getting faster or smoother
(said in one plain sentence).

**Never in:**

- CI, release workflows, build scripts, Gradle, Stonecutter, dependencies, Mixins;
- documentation, `CLAUDE.md`, skills, the README, licences;
- refactors, tests, renames, anything internal;
- developer tooling of any kind.

Conventional-commit types give the first cut: `feat`, `fix` and `perf` are
candidates; `docs`, `chore`, `ci`, `test`, `refactor` and `build` almost never are.
A `build` or `ci` change enters only when a player gains something from it (a new
loader, a new store page), and is then written as that gain. When in doubt: *would a
player notice?* If not, it does not go in. A pull request with nothing visible adds
no entry, and says so.

## Format

```
# MC Skin Creator — Mod changelog

## Unreleased

### New
- **Short title** — one or two sentences: what it lets you do.

## 0.4.1 — 2026-09-30
### Improved
### Fixed
```

- Newest version first; `## <version> — <YYYY-MM-DD>`. Work in progress goes under
  `## Unreleased` (`Próxima versión` / `Prochaine version` in the translations).
- Headings in this order: New, Improved, Fixed. Translations: `Nouveautés` /
  `Améliorations` / `Corrections`, and `Novedades` / `Mejoras` / `Correcciones`.
  **An empty heading is omitted.**
- One bullet is one change, understandable without context. Bold short title, an
  em dash, plain sentences. No class names, no file names, no pull-request numbers.
- No emoji. Use the game's own words for its screens and buttons.
- Screenshots live in `changelog/images/<version>/`, shared by the three languages,
  and are referenced by relative path.

## The version number

It is **not decided here, and not dated here either.** `.github/scripts/next-version.sh`
derives it from the commit types. Write under `## Unreleased` and stop: on a stable release,
`release.yml` renames that section to `## X.Y.Z — <date>` in the three files
(`changelog-release.js`) and announces it on Discord (`announce-discord.js`). Never invent a
number and never date a section by hand — a pull request that did would be announced under
the wrong version.

## Before opening the pull request

1. The three files have the same structure:
   `for f in en fr es; do echo $f $(grep -c '^## ' changelog/$f.md) $(grep -c '^### ' changelog/$f.md) $(grep -c '^- ' changelog/$f.md); done`
   — the three lines must print the same numbers.
2. Nothing internal slipped in: read each bullet and ask the question above.
3. Changelog files are `.md`, so they never trigger a release by themselves
   (`paths-ignore` in `release.yml`) — that is intended.

## What this skill learns

When the author corrects an entry or a writing habit, **the rule is filed here, in
the same change as the correction** — not only in the entry that was corrected.
