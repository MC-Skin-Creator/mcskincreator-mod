# Interface

The mod brings the [MC Skin Creator](https://mcskincreator.app/) editor into the
game. What it takes from the site is the **arrangement** — three zones, a fixed row
of category tabs, the stack grouped by region, the gestures. What it takes from
Minecraft is the **paint**: every surface on this screen is one of the game's own
interface sprites.

Nothing here is invented. That is the rule the rest of this file explains.

## Looking at it

**This screen can be rendered without the game**, and that is the single most useful
thing in this file. `./gradlew :1.21.11:test` writes `build/ui-preview/` — the whole
editor at the five sizes the game gives it, magnified to the size a player sees, plus
a sheet of every sprite it is made of at every height it is used at.

Nothing about it is a mock-up. The panels, the widgets and the arithmetic are the real
ones; the font is read out of the Minecraft jar, with advances computed the way
`BitmapProvider` computes them, so a label is exactly as wide as the game will draw
it; the sprites are the game's, nine-sliced from their own metadata. Only what needs a
running client is stood in for — the player figure, the thumbnail sheets, the mark.

That is what [`Canvas`](src/main/java/fr/clixmods/mcsc/mod/ui/Canvas.java) is an
interface for, and what [`EditorChrome`](src/main/java/fr/clixmods/mcsc/mod/ui/EditorChrome.java)
is separate from the screen for. Keep it that way: a panel that reaches for
`Minecraft.getInstance()` takes the whole screen out of the preview with it, and a
screen nobody can see is a screen nobody checks. The three mistakes that did the most
visible damage to this interface — content drawn under its own panel's frame, a strip
of tabs with nothing to stand on, settings that wanted more room than the column had —
were all found by looking at a picture, and none of them had been found by reading the
code.

`EditorGeometryTest` is the other half of it: it lays the editor out at every width
from 200 to 900 and asserts that nothing lands outside the screen, outside its own
panel, or with no size at all.

## Why the game's sprites and not the site's

The site hand-draws its panels, slots and buttons out of bevels and outlines,
because a web page has nothing else to build them from. A mod does. Using the game's
sprites buys three things a copy cannot:

- a player already knows what a Minecraft button, slot and tab mean;
- a resource pack that restyles the game restyles this screen with it;
- a sprite that changes in a future version changes here too, where an invented copy
  would quietly stop matching the game around it.

So the editor names no material colours of its own, ships no GUI textures, and draws
no icon the game does not already have.

## The surfaces

Every one of these is a vanilla sprite, listed in
[`Sprites`](src/main/java/fr/clixmods/mcsc/mod/style/Sprites.java) and drawn through
[`Surface`](src/main/java/fr/clixmods/mcsc/mod/style/Surface.java). They are all
present and identical on every supported Minecraft version — checked against both
jars, not assumed.

| What the site draws | What the mod draws | Sprite |
|---|---|---|
| Stone panel | the panel behind the game's own pop-ups | `popup/background` |
| Inventory slot | a carved slot, at any size | `container/bundle/slot_background` |
| Green "active" state | the game's selected tab | `widget/tab_selected` |
| Gold hover | the game's hovered widget, and its hover ink | `*_highlighted`, `#FFFFA0` |
| Button | the game's button | `widget/button` |
| Text field | the game's field | `widget/text_field` |
| Slider | the game's rail and handle | `widget/slider`, `widget/slider_handle` |
| Checkbox | the game's checkbox | `widget/checkbox` |
| Scrollbar | the game's scroller and track | `widget/scroller` |
| Close cross | the game's close button | `widget/cross_button` |
| Fold chevron, dropdown arrow | the game's page arrows | `widget/page_forward` |
| Background | whatever the game puts behind a screen | drawn by vanilla |

The ones the game has no sprite for are drawn from the game's own parts instead:

- **the mark** is `assets/mcskincreator/icon.png`, the icon the mod already ships and
  the game's mod list already shows — there is one MC Skin Creator logo, and a second
  one drawn for this screen would drift from it;
- **a category tab** shows the category's own first element, which is a truer picture
  of what is inside than an icon and costs nothing: those pixels are already on the
  graphics card for the grid below;
- **an element's provenance** is on the right mouse button, where the game puts a
  second action, rather than behind a badge that only appears on hover;
- **a layer's visibility** is a checkbox, because that is what it is;
- **a notification's outcome** is a coloured band down its flank. This is the one
  colour of the mod's own left in the interface, and it exists because the game has
  nothing that says "this worked" in a panel.

The only other colours named are ink — white, the game's hover yellow, its greys, its
failure red — taken from where the game uses them.

## Scale

The site is laid out around a 12 px font; the game's is 8 px. Every spacing and line
height is two thirds of its reference value — `Metrics.ui(int)`, and nothing does
that conversion anywhere else.

**A width is not a spacing, and converting one that way is the trap this interface
already fell into.** The site's library is 300 px wide, which `ui()` turns into 200 —
and 200 is wrong, because the site has some 1900 px to spend and the game, at a GUI
scale of 3, has about 640. Two columns of 200 ate two thirds of the screen and left
the model a strip. What carries over is the **share**: a side column is about a fifth
of the width, within bounds that keep it readable at one end and from sprawling at the
other. That is `Metrics.columnWidth(int)`.

What is **not** converted at all is anything the game already has an opinion about. A
button is 20 px tall because that is what a Minecraft button is. The insets in
`Metrics` are the borders of the sprites above, so a label sits clear of the frame the
sprite draws.

**There are two control heights and there is no third.** `BUTTON_HEIGHT` (20) and
`BUTTON_HEIGHT_COMPACT` (16), which is also `TAB_HEIGHT`. A row holding a 14 px tab
beside a 16 px button beside a 20 px one is the whole of why nothing lined up.

**A panel's contents start at its frame, not at its edge.** `popup/background` carries
a six pixel border, so anything laid out closer than that is drawn *under* the frame:
the last letter of a value, the count beside a heading, the flank of a tab.
`Panel.contentLeft()` and `contentRight()` name that box once, and nothing measures
from `x` and `width` itself.

**A tab is part of a tab bar.** The game's tab sprite has no bottom border, because it
is drawn to sit on the thing it opens. One floating in the middle of a column reads as
a bracket, and a row of them that wraps is not a tab bar at all — it is a grid of
boxes. So a tab strip ends flush on a rule, and a strip that will not fit on one row
becomes a dropdown instead of wrapping.

Images are drawn at whole scales only, and the nine-sliced sprites are scaled by the
game, which knows where their borders are — so the mod never stretches a corner.

Everything gives way before the column does: the element grid drops from three
thumbnails to two rather than splitting the width into ones too small to tell apart, a
button cuts its own label rather than drawing past its sprite, and a layer row too
narrow for both actions keeps the one that removes and drops the one that duplicates —
and drops its click target with it.

## Layout

```
┌──────────────────────────────────────────────────────────────┐
│ TOP BAR   mark · beta · tools      dimmed band, ruled off     │
├────────────┬──────────────────────────────┬──────────────────┤
│ LIBRARY    │ SCENE                        │ LAYERS           │
│ ~1/5       │ everything left over         │ ~1/5             │
└────────────┴──────────────────────────────┴──────────────────┘
```

The top bar is a band rather than a panel. `popup/background` at twenty pixels tall is
frame all the way through, and the strip came out as one light slab with the controls
sunk into it; what sits across the top of a screen in this game is a dimmed ground
with a rule under it.

Three columns while the width allows it; below that the side columns become drawers
driven by a tab bar at the bottom, and the drawer lands under the scene in portrait
and beside it in landscape. The GUI scale replaces the site's breakpoints, and the
threshold is measured in interface pixels rather than screen pixels.

**The scene never disappears.** The model has to stay visible while an element is
being chosen or a layer adjusted — that is what one is there to look at. The site
tried two other arrangements and dropped both: stacking the three panels left the
preview 21 px, and swapping them through a single slot made the model vanish the
moment the library opened.

Either side column folds through a ghost button in its header, and a folded header
keeps **only** that button. An earlier arrangement kept the title as well, the header
overflowed, and the button that would have brought the panel back was the part pushed
out of sight.

## Where the pictures come from

Nothing here renders a player. The model in the scene is the game's own
`PlayerSkinWidget` — it already draws a player, already turns under the mouse, and
already follows the classic or slim model of the skin it is handed. The mod only
supplies the skin.

The scene does not build it, though: it is handed a [`Figure`](src/main/java/fr/clixmods/mcsc/mod/ui/Figure.java)
and works out how much room it may have. `PlayerFigure` is the game's one; the preview
stands a labelled box in its place, which is the only reason the scene's own layout
can be looked at at all.

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

## Traps already paid for

- Don't lay a panel's contents out from its edge; lay them out from its frame.
- Don't float a tab. It is drawn to sit on what it opens, and it has no bottom edge.
- Don't wrap a row of tabs onto a second row. Collapse it to a dropdown.
- Don't pin a band whose height is fixed against one that has to fit in the rest: the
  settings wanted 137 px of a column that has 163 on a 720p window, and the list was
  left one row that drew over them. One scroll fits every screen the game has.
- Don't say the same thing twice in two empty states. With no layers there is nothing
  to select either, and both sentences landed in the same place.
- Don't put the preview behind anything.
- Don't fill an empty category — an empty category is not shown at all, and a control
  whose target is empty disappears instead of opening onto nothing.
- Don't make anything clickable that is not visible. Layer actions and scrolled-away
  tiles are out of reach at rest, not merely invisible: that is how people delete a
  layer by clicking a cross they never saw.
- Don't invent a sprite, an icon or a material colour. If the game has no way to say
  it, say it with the game's words instead — a checkbox for a yes, a tab for a
  choice, the right mouse button for a second action.
- Don't let a folded panel lose the button that unfolds it.
- Don't lay a backdrop after what sits on it. The scene's corner boxes were drawn
  after their own controls once, and the dock spent a release looking like an empty
  frame.
- Don't trust a layout you have only read. The two side panels can be laid out with no
  game running — `Font` and `Language` are both subclassable — and doing so found a
  button wider than its own panel in a single run.
- Never write to the display. The state changes and the display follows, through
  `SkinProject.revision()`; a mutation that forgot to bump it would leave the preview
  a step behind with no error anywhere.

## What is not built yet

The interface is complete; several of the things it is an interface *to* are not.
These are deliberate gaps, and each one follows the rule about empty targets — the
control is absent rather than dead. That is why there is no animation chooser in the
scene dock, no colour swatches in the inspector (the catalogue carries no colour keys
yet), no random-outfit button, and no starter templates.

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
that says what will change and, in a framed note, that other players wait on
Minecraft's profile servers — tens of seconds, sometimes longer. The note is the
feature; the button is the easy part.

That note used to say nothing changes on screen either, and it was right until the mod
started wearing the skin itself. **The player now sees the change on their own player
the moment Mojang accepts it**, which removes the worst of the confusion — a
successful upload no longer looks like a failure — but it does not remove the note. It
inverts it: what is instant is what *you* see, and the gap that remains is what
everyone else sees. Someone who reads "applied", looks down at their new skin and then
asks a friend who still sees the old one needs that sentence more than before, not
less.

**A card that cannot work is absent, and the absence is explained.** With no
Microsoft-signed-in session there is no token, so the apply card is not drawn at all —
a control whose target is empty disappears rather than opening onto nothing. But a
choice that silently vanishes is one nobody can ask about, so the export window carries
a note under its cards saying why. That is what `CardWindow`'s note is for.

**A locked button counts down.** Between two uploads the confirm button is disabled
and its label says how many seconds are left, refreshed as they pass. A button that
says "wait 20 seconds" and still says it a minute later is a button people conclude is
broken, so `SkinCreatorScreen.tick()` re-lays the window out as the count changes.
