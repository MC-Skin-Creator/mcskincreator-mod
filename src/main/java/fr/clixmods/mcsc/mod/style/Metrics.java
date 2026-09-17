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

    // The borders of the sprites in Sprites, so content is laid inside them.
    /** {@code popup/background} carries a 6 px border. */
    public static final int PANEL_INSET = 6;
    /** {@code slot_background} carries a 4 px border. */
    public static final int SLOT_INSET = 4;
    /** {@code widget/button} carries a 3 px border. */
    public static final int BUTTON_INSET = 3;
    /** {@code widget/text_field} carries a 1 px border; a second pixel keeps the caret clear. */
    public static final int FIELD_INSET = 2;

    /** A Minecraft button is 20 px tall. The game wins on widget mechanics. */
    public static final int BUTTON_HEIGHT = 20;
    /** The same sprite in a crowded strip: its border plus a line of the game's font. */
    public static final int BUTTON_HEIGHT_COMPACT = 16;
    public static final int BUTTON_PAD_X = ui(10);

    // Panels.
    /** Library column, 300 site px. */
    public static final int LIBRARY_WIDTH = ui(300);
    /** Layers column, 318 site px. */
    public static final int LAYERS_WIDTH = ui(318);
    /** A folded column keeps just enough room for the button that unfolds it. */
    public static final int COLLAPSED_WIDTH = ui(40);
    /** Top bar, 46 site px. */
    public static final int TOP_BAR_HEIGHT = ui(46);

    /** The scene below this width is not worth showing, so the columns become drawers. */
    public static final int MIN_SCENE_WIDTH = 180;

    /** A category tab: a slot big enough for an element's face and its frame. */
    public static final int CATEGORY_TAB = 28;

    /** Buttons of a segmented group are 3 site px apart. */
    public static final int SEGMENT_GAP = ui(3);

    /**
     * A layer row holds a name and a subtitle under it, plus its frame — which is what
     * the site's 26 px row holds too, at its own font size.
     */
    public static final int LAYER_ROW = 24;
    /** The layer's own thumbnail, the size of an inventory icon. */
    public static final int LAYER_PREVIEW = 16;

    /** Thumbnails sit in a three column grid, 5 site px apart. */
    public static final int GRID_GAP = ui(5);
    public static final int GRID_COLUMNS = 3;
    /** Render area of a thumbnail, 58 site px. */
    public static final int THUMB_RENDER = ui(58);
    /** Transparency checker squares, 8 site px. */
    public static final int CHECKER = ui(8);

    /** A slider is a Minecraft slider: the game's handle is 20 tall and sets the row. */
    public static final int SLIDER_ROW = BUTTON_HEIGHT;
    /** Slider values are right aligned in a fixed 38 site px column. */
    public static final int SLIDER_VALUE_WIDTH = ui(38);

    /** Generic inner padding, 8 site px. */
    public static final int PAD = ui(8);
    /** Tight inner padding, 4 site px. */
    public static final int PAD_TIGHT = ui(4);

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
