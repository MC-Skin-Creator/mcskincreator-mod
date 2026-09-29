# Security

This mod touches your Minecraft session token, in exactly one place. A mod that does
that starts out presumed guilty, and the way to answer is not to ask for trust but to
make the code short enough to read. This file says where to look. Every claim below
points at a file, and if a claim and its file ever disagree, the file is right and
that is a bug to report.

## What the mod reads

The session's **access token**, and nothing else of the session. It is read when you
press **Apply** in the export window, and at no other moment: not at startup, not on
a timer, not in the background.

- [`GameSession`](src/main/java/fr/clixmods/mcsc/mod/account/GameSession.java) is the
  only code that reads it. It is package-private, so nothing outside
  `fr.clixmods.mcsc.mod.account` can reach it, and the only public way into that package
  is [`AccountSkin`](src/main/java/fr/clixmods/mcsc/mod/account/AccountSkin.java).
- The token is passed as a method argument to `MojangSkins.upload` and is never stored
  in a field, a file, or a cache.

## Where the token goes

To **one address**, and it is a Mojang one:

```
https://api.minecraftservices.com/minecraft/profile/skins
```

That is the call the official launcher makes to change a skin. It is a single constant,
[`MojangSkins.ENDPOINT`](src/main/java/fr/clixmods/mcsc/mod/account/MojangSkins.java),
and `Authorization` is set in one place in the whole code base, the request built in
that same file. Uploading happens once per press of the button, and the button locks
between two uploads.

## What the mod never does with it

- **It never sends the token to the MC Skin Creator back-end.** The back-end client,
  [`McscApi`](src/main/java/fr/clixmods/mcsc/mod/remote/McscApi.java), sets no
  `Authorization` header and has no access to `GameSession`.
- **It never logs it.** Nothing in `MojangSkins` logs a header, a body or a request.
  A failed upload raises an exception that carries an HTTP status and nothing else,
  and the log line in `AccountSkin` receives that exception, never the request.
- **It never writes it to disk**, and so it cannot end up in a crash report through
  a file the mod wrote.
- **It never uses it to read anything.** The check that the account is wearing the
  skin you applied,
  [`WornSkin`](src/main/java/fr/clixmods/mcsc/mod/account/WornSkin.java), asks
  Mojang's public session server for the public profile, with no token and no
  `Authorization` header.
- **It stays silent when the session cannot upload.** A session without an Xbox user
  id and a token (an offline one, for instance) gets no Apply button and a message
  saying why.

You can check the first ones yourself: search the repository for `getAccessToken` and
`Authorization`. `getAccessToken` appears only in `GameSession`, and `Authorization` only in `MojangSkins`.

## Every address the mod talks to

| Address | What for | Carries |
|---|---|---|
| `https://mcskincreator.app/api/v1` | the catalogue, the atlases, search, credits, the composed texture, the saved-skin library | the skin project, and a random client identifier (below). **No token.** |
| `https://api.minecraftservices.com/minecraft/profile/skins` | applying a skin to your account | the session token and the PNG, **only when you press Apply** |
| `https://sessionserver.mojang.com/session/minecraft/profile/` and `textures.minecraft.net` | reading the skin an account is wearing | nothing of the session |

The back-end address can be replaced with `-Dmcskincreator.api=…` or the
`MCSKINCREATOR_API` environment variable, which is a developer setting; nothing in
the mod sets it. The token goes to the Mojang endpoint above whatever that address is.

## What is stored on your disk

| File | Where | What it holds |
|---|---|---|
| `mcskincreator-client.txt` | the game's `config/` folder | a random identifier the mod draws once, and sends with the saved-skin calls. It is not derived from your account, your name or your machine. |
| `mcskincreator-project.json` | the game's `config/` folder | the skin you are editing, so it survives a restart |
| exported PNGs | the game's `mcskincreator/` folder | the skins you chose to export |

Nothing else is written. In particular there is nothing that resembles a credential
in any of them. Deleting `mcskincreator-client.txt` does not delete your saved skins
on the server, but it does lose the way back to them.

## Reporting a vulnerability

Please **do not open a public issue** for something exploitable. Use GitHub's private
report instead:
[report a vulnerability](https://github.com/MC-Skin-Creator/mcskincreator-mod/security/advisories/new).

You will get an answer within **7 days**, and a fix or a stated plan within
**30 days** of a confirmed report. Reports are credited unless you ask otherwise.

The most useful report is a path by which the session token reaches anything other
than the Mojang endpoint above — that is the one promise this mod is built around.
Anything that contradicts a claim in this file counts, even if it is not exploitable.

## Supported versions

Only the latest release, and the latest pre-release build of `develop`, receive fixes.
