# Interface

The mod reproduces the interface of [MC Skin Creator](https://mcskincreator.app/)
inside the game. The site already imitates Minecraft, so nothing here is being
"adapted to the Minecraft style" — the work runs the other way, bringing an
interface that came from the game back into it.

Where a site rule and a vanilla widget disagree, the widget wins on mechanics
(font size, GUI scale, focus) and the site wins on identity (palette, bevels,
the green of an active state, layout).

## The four materials

Every surface is one of four, and there is no fifth. An element that fits none of
them is an element that was designed wrong, so [`Surface`](src/main/java/fr/clixmods/mcsc/mod/style/Surface.java)
offers no escape hatch for one.

| Material | Used for | Fill | Bevel |
|---|---|---|---|
| Stone panel | windows, side panels, notifications, dropdowns | `#2e2e34` + grain | light top-left |
| Slot | thumbnails, layer rows, category tabs, framed areas | `#1b1b20` | inverted — carved in |
| Button | every action | `#63636b` | light top-left |
| Dark surface | tool strips, scene background | `#26262b` / `#18181c` / `#101014` | none, or the panel's |

Green `#3c8527` is the active state and the primary action. Red `#a03b31` is
failure and destruction. Gold `#fcfc54` is hover. The whole palette is in
[`Palette`](src/main/java/fr/clixmods/mcsc/mod/style/Palette.java), transcribed
from the site and derived from nothing.

The palette stays dark from end to end. On the site that began as a workaround —
browsers in forced dark mode re-invert light backgrounds — and it no longer has
that reason here, but it has become the identity of the product. A light,
inventory-coloured panel would be more faithful to the game and less faithful to
the site.

## Drawing rules

- Nothing is rounded, nothing is gradient, nothing animates.
- One pure black outline, 2 px, on every framed element.
- The bevel is drawn **inside** that outline, over 2 px, never on the border.
- Pressing inverts the bevel and drops the content one pixel. That is the entire
  press feedback.
- Text over a panel carries the game's own 1 px shadow. Text laid on the scene is
  ringed in black on all four sides instead — a shadow is enough over a flat
  colour and not enough over a figure.
- Images and icons are drawn at whole scales only. An icon designed at 16 px is
  shown at 16 or at 32, never at 24.
- **No emoji, anywhere, not even as a stop-gap.** An emoji is drawn by whatever
  font the machine has: its shape, colour and size change from device to device,
  and it is round and smoothed in the middle of an interface that is square.
  Icons are 16x16 pixel drawings with an outline laid on automatically around the
  silhouette — see [`Icons`](src/main/java/fr/clixmods/mcsc/mod/ui/Icons.java),
  which writes them as rows of characters so a drawing can be reviewed in a diff.
- State never travels by border. The border is black; colour goes in the fill, the
  bevel, or a band down the left flank.

## Scale

The site is laid out around a 12 px font; the game's is 8 px. Every spacing and
every line height is therefore two thirds of its reference value, rounded, with a
floor of 2 — `Metrics.ui(int)`, and nothing does that conversion anywhere else.

Two things deliberately do not shrink:

- the 2 px bevel and the 2 px outline, which are the graphic identity itself and
  stop reading as anything at 1 px;
- boxes sized by an icon. A category tab holds a 16 px icon at exactly x2, so it
  stays 38 px wide whatever the font does.

The floor of 2 is not cosmetic either: a 1 px gap between two dark surfaces is
invisible, so anything that rounded to 1 would silently merge the two elements it
was meant to separate.

## Layout

```
┌──────────────────────────────────────────────────────────────┐
│ TOP BAR   mark · beta · tools                                │
├────────────┬──────────────────────────────┬──────────────────┤
│ LIBRARY    │ SCENE                        │ LAYERS           │
│ 200 px     │ everything left over         │ 212 px           │
└────────────┴──────────────────────────────┴──────────────────┘
```

Three columns while the width allows it; below that the side columns become
drawers driven by a tab bar at the bottom, and the drawer lands under the scene in
portrait and beside it in landscape. The GUI scale replaces the site's breakpoints,
and the threshold is measured in interface pixels rather than screen pixels.

**The scene never disappears.** The model has to stay visible while an element is
being chosen or a layer adjusted — that is what one is there to look at. The site
tried two other arrangements and dropped both: stacking the three panels left the
preview 21 px, and swapping them through a single slot made the model vanish the
moment the library opened.

Either side column folds to 27 px through a ghost button in its header, and a
folded header keeps **only** that button. An earlier arrangement kept the title as
well, the header overflowed, and the button that would have brought the panel back
was the part pushed out of sight.

## Traps already paid for

- Don't put the preview behind anything.
- Don't fill an empty category — an empty category is not shown at all, and a
  control whose target is empty disappears instead of opening onto nothing.
- Don't stack two marks in the same corner of a thumbnail.
- Don't make anything clickable that is not visible. Layer actions are out of
  reach at rest, not merely invisible: that is how people delete a layer by
  clicking a cross they never saw.
- Don't announce a state with a coloured border.
- Don't let a folded panel lose the button that unfolds it.
- Never write to the display. The state changes and the display follows, through
  `SkinProject.revision()`; a mutation that forgot to bump it would leave the
  preview a step behind with no error anywhere.

## Where the pictures come from

Nothing here renders a player. The model in the scene is the game's own
`PlayerSkinWidget` — it already draws a player, already turns under the mouse, and
already follows the classic or slim model of the skin it is handed. The mod only
supplies the skin.

Thumbnails are cheaper still: a category's atlas arrives as raw 64x64 skins, and
`FrontSprite` folds each one into the 16x32 front view the site's slots show. One
sheet per category rather than one texture per element, cropped per category to the
part of the body the element actually covers — a shelf of hairstyles drawn on whole
bodies is unreadable, which is what `ThumbCrop` exists to prevent.

The stack is composed by the server: the project goes to `POST /textures` and the
sheet comes back. Picking never waits on that. The element goes on the model
immediately from the atlas buffer already in memory, and the composition replaces it
when it lands, so a server that cannot compose costs a notification rather than the
preview.

## What is not built yet

The interface is complete; several of the things it is an interface *to* are not.
These are deliberate gaps, and each one follows the rule above about empty targets —
the control is absent rather than dead. That is why there is no animation chooser in
the scene dock, no colour swatches in the inspector (the catalogue carries no colour
keys yet), no random-outfit button, and no starter templates.

| Missing | Where it lands |
|---|---|
| The pixel drawing tools and their shortcuts | issues #7, #8 |
| Per-element colours, and the swatch row that goes with them | issue #7 |
| Composing locally instead of over the network | issue #8 |
| Importing a texture, and the starter templates | issues #8, #10 |
| The fitting room: the skin on your own client only | issue #11 |

The saved-skin list is built: **My skins** in the top bar lists what the server
keeps for this installation, each row drawn from the picture the server composed
when it stored that skin. Opening one replaces the stack, and there is no skin that
follows the editing the way the site's open skin does — saving always makes a new
entry, because silently replacing one is the single thing nobody could undo.

Export writes into `<game>/mcskincreator/`: the composed sheet as a 64x64 PNG, or
the character seen from the front, which the server draws. There is no language
picker: in the game the language is the game's, and the mod follows it.

## Applying to the account

The export window is where the skin leaves the editor, and its cards are not alike.
Two of them write a file onto this machine. The third changes the player's real skin,
for everyone, until they change it back — and it is last in the list for that reason.
Three rules follow from it, and none of them is decoration.

**The cost is stated before the button, not after.** Applying opens a confirmation
that says what will change and, in a framed note, that Minecraft's profile servers
take tens of seconds to hand the new skin out. Without that sentence a successful
upload is indistinguishable from a failed one, and the player presses again — which is
exactly what gets a session rate-limited. The note is the feature; the button is the
easy part.

**A card that cannot work is absent, and the absence is explained.** With no
Microsoft-signed-in session there is no token, so the apply card is not drawn at all —
a control whose target is empty disappears rather than opening onto nothing. But a
choice that silently vanishes is one nobody can ask about, so the export window carries
a note under its cards saying why. That is what `CardWindow`'s note is for.

**A locked button counts down.** Between two uploads the confirm button is disabled
and its label says how many seconds are left, refreshed as they pass. A button that
says "wait 20 seconds" and still says it a minute later is a button people conclude is
broken, so `SkinCreatorScreen.tick()` re-lays the window out as the count changes.
