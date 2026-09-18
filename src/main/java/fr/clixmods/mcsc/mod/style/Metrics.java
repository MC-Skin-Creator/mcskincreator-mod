/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.style;

/**
 * The site's measurements, converted once to the units the game draws in.
 *
 * <p>The site is laid out around a 12 px font; the game's font is 8 px tall. Every
 * spacing and every line height is therefore two thirds of its reference value —
 * {@link #ui(int)} does that conversion and nothing else does it ad hoc.
 *
 * <p>What is <em>not</em> converted is anything the game already has an opinion
 * about. A button is 20 px tall because that is what a Minecraft button is; the
 * insets below are the borders of the game's own sprites, so a label sits clear of
 * the frame the sprite draws rather than clear of one the mod invented.
 */
public final class Metrics {
    private Metrics() {
    }

    /**
     * The two gaps this interface uses, and there is no third.
     *
     * <p>Not converted from the site any more. The site's spacings ran through
     * {@link #ui(int)} and came out 5 and 3, which is a third of a letter of air
     * between controls — and the whole interface read as cramped for it. These are the
     * game's own rhythm: a Minecraft screen is laid out on fours and eights.
     */
    public static final int PAD = 8;
    public static final int PAD_TIGHT = 4;

    /**
     * The black frame around every material, and the bevel drawn just inside it.
     *
     * <p>Neither shrinks with the scale conversion below. They are the graphic identity
     * itself and stop reading as anything at one pixel.
     */
    public static final int OUTLINE = 2;
    public static final int BEVEL = 2;
    /** A window is told from a panel by a deeper bevel and a black rim, not by a colour. */
    public static final int WINDOW_BEVEL = 3;
    public static final int WINDOW_RIM = 2;

    // Where a material's content starts, so a label sits clear of the frame around it.
    /** A panel's frame and bevel, and a pixel of air past them. */
    public static final int PANEL_INSET = OUTLINE + BEVEL + 2;
    /** A slot's frame and bevel. */
    public static final int SLOT_INSET = OUTLINE + BEVEL;
    /** A button's frame and bevel. */
    public static final int BUTTON_INSET = OUTLINE + BEVEL;
    /** A field's one pixel border, and a second pixel that keeps the caret clear. */
    public static final int FIELD_INSET = 2;

    /** A tick box, at the size a layer row can spare. */
    public static final int CHECKBOX = 14;
    /** The handle that runs along a slider rail. */
    public static final int SLIDER_HANDLE = 8;
    /** The close cross of a window, and the one that removes a layer. */
    public static final int CROSS = 14;
    /** A scroll rail, narrow enough to be a gutter rather than a column. */
    public static final int RAIL_WIDTH = 6;
    /** The triangle that says a control opens onto a list. */
    public static final int CARET_WIDTH = 7;
    public static final int CARET_HEIGHT = 4;

    /** A Minecraft button is 20 px tall. The game wins on widget mechanics. */
    public static final int BUTTON_HEIGHT = 20;
    /** The same sprite in a crowded strip: its border plus a line of the game's font. */
    public static final int BUTTON_HEIGHT_COMPACT = 16;
    /**
     * A tab, which is the compact control height and not a third one.
     *
     * <p>The interface has exactly two control heights: this and {@link #BUTTON_HEIGHT}.
     * It used to have three, and a row holding a 14 px tab beside a 16 px button beside
     * a 20 px one is the whole of why nothing lined up.
     */
    public static final int TAB_HEIGHT = BUTTON_HEIGHT_COMPACT;
    public static final int BUTTON_PAD_X = ui(10);

    // Panels.
    /**
     * A side column takes about a fifth of the width, as it does on the site.
     *
     * <p>Converting the site's 300 px by the font ratio gives 200, and that is the
     * wrong answer: the site has some 1900 px to spend and the game, at a GUI scale of
     * 3, has about 640. Two columns of 200 would eat two thirds of the screen and
     * leave the model a strip. The share is what has to carry over, within bounds that
     * keep a column readable at one end and from sprawling at the other.
     */
    public static int columnWidth(int screenWidth) {
        return Math.max(MIN_COLUMN_WIDTH, Math.min(MAX_COLUMN_WIDTH, screenWidth * 17 / 100));
    }

    /** Narrow enough to still hold two thumbnails side by side. */
    public static final int MIN_COLUMN_WIDTH = 120;
    /**
     * Wide enough on a big screen without turning into a second scene.
     *
     * <p>The cap used to be 170 and it was reached everywhere, which is why the columns
     * looked stranded in a wide window: the share stopped applying and the scene took
     * everything the column did not. The site's own library is a little over a seventh
     * of its width, and this is that share with room to go on growing before it stops.
     */
    public static final int MAX_COLUMN_WIDTH = 210;

    /** A folded column keeps just enough room for the button that unfolds it. */
    public static final int COLLAPSED_WIDTH = ui(40);
    /** Top bar: one compact button and the air around it. */
    public static final int TOP_BAR_HEIGHT = BUTTON_HEIGHT_COMPACT + PAD_TIGHT * 2;

    /** The scene below this width is not worth showing, so the columns become drawers. */
    public static final int MIN_SCENE_WIDTH = 160;

    /** A category tab: a slot big enough for an element's face and its frame. */
    public static final int CATEGORY_TAB = 24;

    /**
     * The model is given most of the scene but not all of it: the strips and the
     * reminder in the corners need somewhere to sit that is not on top of the player.
     */
    public static final double SCENE_MODEL_SHARE = 0.78;

    /** Buttons of a segmented group are 3 site px apart. */
    public static final int SEGMENT_GAP = ui(3);

    /**
     * A layer row holds a name and a subtitle under it, plus the slot's own frame.
     *
     * <p>Two lines of the game's font is 18 px, and the slot sprite's border is 4 px at
     * each end. At 22 the subtitle's last pixel row was under the frame, which is not a
     * clipped letter so much as a row that looks cut in half.
     */
    public static final int LAYER_ROW = 26;
    /** The layer's own thumbnail, the size of an inventory icon. */
    public static final int LAYER_PREVIEW = 16;

    /** Thumbnails sit in a three column grid, 5 site px apart. */
    public static final int GRID_GAP = ui(5);
    public static final int GRID_COLUMNS = 3;
    /** Render area of a thumbnail, 58 site px. */
    public static final int THUMB_RENDER = ui(58);
    /**
     * The tallest a thumbnail may be: two whole front views.
     *
     * <p>A whole multiple of the 32 px view on purpose. Anything between 32 and 64
     * would draw the figure at one, leaving it a third of the size of the tile it sits
     * in; at 64 a full-body element is drawn at two and fills its slot.
     */
    public static final int MAX_THUMB_RENDER = 64;
    /** Transparency checker squares. Small, so a thumbnail reads before its backdrop. */
    public static final int CHECKER = 4;

    /**
     * The rail of a settings slider. The game's handle is 20 px tall natively but
     * nine-sliced, so it scales to this without smearing — and four rails at 20 would
     * take a third of the panel.
     */
    public static final int SLIDER_RAIL = 10;

    /** The narrowest a thumbnail may be before the grid drops a column. */
    public static final int MIN_TILE_WIDTH = 44;

    /** A window is 600 site px wide at most and 88 % of the screen tall. */
    public static final int WINDOW_MAX_WIDTH = ui(600);
    public static final double WINDOW_MAX_HEIGHT_RATIO = 0.88;

    /** A notification stays up for 2.4 s. */
    public static final long TOAST_MILLIS = 2400L;
    /** Coloured band on the left flank of a notification or a warning, 6 site px. */
    public static final int BAND = ui(6);

    /** A label too long to fit scrolls at 45 site px per second... */
    public static final double MARQUEE_SPEED = ui(45);
    /** ...and rests 450 ms at each end. */
    public static final long MARQUEE_PAUSE = 450L;

    /** Letter spacing of a panel title, 1.5 site px rounded to a whole pixel. */
    public static final int TITLE_TRACKING = 1;

    /** The history keeps 60 states. */
    public static final int HISTORY_DEPTH = 60;

    /** An element name is never shown beyond 24 characters, in any language. */
    public static final int MAX_NAME_CHARS = 24;

    /**
     * Converts a site measurement to a game one: two thirds, rounded, never below 2.
     *
     * <p>The floor of 2 is not cosmetic. A 1 px gap between two dark surfaces is
     * invisible, so anything that rounded down to 1 would silently merge the two
     * elements it was meant to separate.
     */
    public static int ui(int sitePixels) {
        return Math.max(2, Math.round(sitePixels * 2.0f / 3.0f));
    }
}
