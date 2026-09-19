# Interface

The mod brings the [MC Skin Creator](https://mcskincreator.app/) editor into the game.
What it takes from the site is both the **arrangement** — three zones, a fixed row of
category tabs, the stack grouped by region, the gestures — and the **paint**: four
materials, one accent colour, a black outline on everything.

What it takes from Minecraft is what Minecraft is right about: its font, its GUI scale,
its widget mechanics, and the player model in the scene.

Drawing the editor with the game's own interface sprites instead was tried, and is the
one thing in here that was reverted rather than refined. A nine-sliced pop-up plate is
built for a dialogue box; stretched down a full-height column it is a broad light frame
around a dark well. The tab sprite has no bottom edge, because it is drawn to sit on
whatever it opens. And with every control the same grey there was nothing on the screen
to tell a chosen thing from an unchosen one. The site's own look is already a Minecraft
look — it is where these materials come from — and it is built for a workbench.

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

## The four materials

Every surface is one of four, and there is no fifth. An element that fits none of them
is an element that was designed wrong, so
[`Surface`](src/main/java/fr/clixmods/mcsc/mod/style/Surface.java) offers no escape
hatch for one.

| Material | Used for | Fill | Bevel |
|---|---|---|---|
| Stone panel | columns, windows, notifications, dropdowns | `#2e2e34` + grain | light top-left |
| Slot | thumbnails, layer rows, category tabs, rails | `#1b1b20` | inverted — carved in |
| Button | every action | `#63636b` | light top-left |
| Dark surface | the header strip, tool strips | `#26262b` / `#18181c` / `#101014` | none |

Green `#3c8527` is the active state and the primary action. Red `#a03b31` is failure
and destruction. Gold `#fcfc54` is hover ink, on every material.

Every one of those values is a custom property in the site's own stylesheet, transcribed
into [`Palette`](src/main/java/fr/clixmods/mcsc/mod/style/Palette.java) and derived from
nothing. Nothing else in the source tree names a colour.

### Drawing rules

- Nothing is rounded, nothing is a gradient, nothing animates.
- One pure black outline, 2 px, on every framed element.
- The bevel is drawn **inside** that outline, over 2 px, never on the border.
- Pressing inverts the bevel and drops the content one pixel. That is the whole of the
  press feedback.
- The grain is one 64 px greyscale tile, generated from seeded noise and multiplied by
  the fill, so every material speckles without a second asset and without ever going
  out of step with the palette.
- State never travels by border. The border is black; colour goes in the fill, the
  bevel, or a band down the left flank.

## What the game still supplies

The font, the GUI scale, the player model, and the mechanics of focus and input. Not a
surface.

Where the game has an idiom worth borrowing, it is borrowed — as an idea rather than
as a picture:

- **the mark** is `assets/mcskincreator/icon.png`, the icon the mod already ships and
  the game's mod list already shows — there is one MC Skin Creator logo, and a second
  one drawn for this screen would drift from it;
- **a category tab** shows the category's own first element, which is a truer picture
  of what is inside than an icon and costs nothing: those pixels are already on the
  graphics card for the grid below;
- **an element's provenance** is on the right mouse button, where the game puts a
  second action, rather than behind a badge that only appears on hover;
- **a layer's visibility** is a tick box, because that is what it is: a slot with a
  green core, since at the size a layer row can spare a drawn tick is four pixels of
  noise and the colour is legible across the column;
- **a caret** — the triangle on a dropdown — is drawn from the palette, because the
  game's only arrow is a page-turn arrow, 23 by 13 pixels of near-white that swamps a
  control this size and says "next" rather than "more";
- **a notification's outcome** is a coloured band down its flank. This is the one
  colour of the mod's own left in the interface, and it exists because the game has
  nothing that says "this worked" in a panel.



## Scale

**The editor takes its own GUI scale.** It is a workbench, not a menu: three columns,
a grid of thumbnails, a stack of layers and four settings at once, and at the scale
most people play at it would get 640 by 360 pixels to do that in. The game's font is 8
of those — a ninth of the height of a layer row — so everything around it has to be
big too, and the result reads as an interface designed for a phone.

The font is the floor and it cannot be lowered: it is a bitmap, and half a pixel of it
is a broken letter rather than a smaller one. Drawing the editor two thirds the size
inside the player's scale would mangle every glyph on screen. So
[`EditorScale`](src/main/java/fr/clixmods/mcsc/mod/ui/EditorScale.java) asks the window
for the whole scale nearest to **960 by 540** — what a 1080p window gives at a scale of
2, and the size this interface is laid out for — and gives the player's own back the
moment the screen closes. It is their setting, so it is restored in `removed()`, which
the game calls however the screen ends, and re-applied on every layout, because a
window resize makes the game recompute it from the options.

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

**Three gaps and one control height.** The gaps are the site's 8, 6 and 3 converted:
6, 4 and 2. The height is 20, which is its `padding: 5px 10px` over an 11 px line. A
row holding a 14 px tab beside a 16 px button beside a 20 px one is the whole of why
nothing used to line up.

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

Nothing here renders a player. The figure in the scene is drawn by the game, through
the same picture-in-picture path the inventory portrait goes through: the mod fills in
an `AvatarRenderState` — a skin and a situation — and hands it over
(`Canvas.entity`, `scene/PosedPlayer`). What comes back is the game's own player
rendering, with its overlay layer, its animation and its classic or slim proportions.

That path is what gives the scene a camera and an animation chooser, and it replaced
vanilla's `PlayerSkinWidget`, which turns under the mouse and does nothing else. The
widget is still what the panel on the vanilla menus uses (`SkinPanel`), where turning
is all that is wanted.

**The animations are named in the game's terms, not the site's.** The site had to
write its ten out by hand — limb angles, cycle lengths, a coordinate change and a
test measuring which arm ends up inside the skull. Here walking is a walk speed and
crouching is a boolean, and the game animates it: `scene/ScenePose` sets fields the
player model already reads. Which is also why the list is shorter. The site's wave
and T-pose are not in the game's player model, and reaching them would mean driving
the model's parts directly — the one route that is not the same call on both
Minecraft targets — so they are absent rather than dead, like everything else whose
target is empty.

**Under a camera that shows the world, the editor is a layer over the game.** The
screen paints no backdrop — and overrides vanilla's, which blurs what is behind and
then covers it with the opaque menu background — and both side panels fold themselves
away, because the game draws the first-person arm exactly where the layers panel sat.
The player's own fold choice comes back with them. The game's HUD goes too, and not
through the game's own flag: that flag takes the held hand with it, so the hotbar is
left undrawn for the frame instead (`scene/HiddenHud`, `mixin/GuiMixin`) and the
setting the player owns is not touched.

**Both world cameras borrow the game's own** (`scene/GameCamera`, public API, given
back on the way out), and both leave it pointed at the player,
because the level renderer draws your own character only while they *are* the camera
entity. Third person works because the camera is detached, not because it is somewhere
else; going round the character is therefore done by turning them, and their body is
pinned so they keep facing the way they were. That is real game state, saved and put
back. See `DECISIONS.md`, "The local player is only drawn when they are the camera".

**The backdrop is a separate question from the camera.** `scene/SceneBackdrop` puts
either the editor's own dark or the live world behind the workshop figure — which is
how the animations and the zoom stay available with a landscape behind them, since the
in-game view shows the real character and a real character cannot be posed. It is a
preference, never serialised.

A backdrop is **behind**, not **around**: the figure is a GUI element drawn after the
world, so it takes no world lighting and no shader pack. The view that puts the
character in the world is the in-game one — and it animates there too, because
`AvatarRendererMixin` poses the real character as the game extracts them. So the
animation chooser belongs to both views, and only the first-person one is without it.
The world backdrop also borrows the camera, for the opposite reason to the other two —
first person plus the game's own HUD flag is how the game is asked to draw the world and
nothing else, neither the character nor their hand. It is the one place that flag is
still the right tool.

The scene does not build the figure, though: it is handed a [`Figure`](src/main/java/fr/clixmods/mcsc/mod/ui/Figure.java)
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

The ready-made stacks — the starter models and the outfits — are the one place that
composes locally, because there are two hundred of them on screen at once and a
request each is not a thing to ask for. Their pictures are what choosing one will put
on the model, which is not always what the site shows for the same entry: the
catalogue gives some pieces a colour override, and a layer here cannot hold one yet.

An outfit is clothes and nothing else, so its picture is stood on a body taken from
the catalogue's own skin category. The site names one outright and the catalogue no
longer carries it, which is exactly what a hardcoded id gets you — a silently empty
body.

## Traps already paid for

- Don't lay a panel's contents out from its edge; lay them out from its frame.
- Don't float a tab. It is drawn to sit on what it opens, and it has no bottom edge.
- Don't wrap a row of tabs onto a second row. Collapse it to a dropdown.
- Don't let the settings scroll. The list moves, they stay: a slider that can scroll
  out from under a drag is a slider nobody can use. What makes that safe is the
  editor's own scale, and a band clipped to itself so it can never reach into the list
  the way it once did.
- Don't make a tab of something that opens nothing. The model chooser was a pair of
  tabs with no body under them — one drawn as a black box, the other as an open frame,
  and neither making sense. It is one control that says what the model is.
- Don't say the same thing twice in two empty states. With no layers there is nothing
  to select either, and both sentences landed in the same place.
- Don't put the preview behind anything.
- Don't show a layer by hiding the others. Pointing at a layer row makes that layer
  **pulse over the composed stack** (`skin/Highlight`); it used to put the layer on the
  model on its own, which showed what the layer was and hid where it was — and turned a
  covered or switched-off layer into the figure disappearing.
- Don't fill an empty category — an empty category is not shown at all, and a control
  whose target is empty disappears instead of opening onto nothing.
- Don't make anything clickable that is not visible. Layer actions and scrolled-away
  tiles are out of reach at rest, not merely invisible: that is how people delete a
  layer by clicking a cross they never saw.
- Don't name a colour outside `Palette`, and don't add one that is not a line of the
  site's stylesheet. Four materials and three tones is the whole system.
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
These are deliberate gaps, and each one follows the rule above about empty targets —
the control is absent rather than dead. That is why there is no animation chooser in
the scene dock, no colour swatches in the inspector (the catalogue carries no colour
keys yet), and no random-outfit button. The starter models the site offers are
there — that is what the **Models** button in the top bar opens — and the button
itself disappears when the catalogue turns out to carry none.

**Outfits are a region of the library, not a window.** The site gives them a shelf
between the body and the head, and they are picked exactly the way an element is, so
they are one more region here too. The catalogue does not carry that region — outfits
are a list beside the categories rather than a category — so `LibraryPanel` adds the
tab, and stands the outfits on a shelf that never leaves the panel. That shelf is a
category in shape only: it lets an outfit be drawn and picked by the same `ItemTile`
as everything else instead of a second widget to keep looking the same, and the
project only ever sees a real `CatalogModel`.

| Missing | Where it lands |
|---|---|
| The pixel drawing tools and their shortcuts | issues #7, #8 |
| Per-element colours, and the swatch row that goes with them | issue #7 |
| Composing locally instead of over the network | issue #8 |
| Importing a texture | issue #8 |
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
