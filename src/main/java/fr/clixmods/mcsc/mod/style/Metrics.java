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
 * <p>Two values deliberately do <em>not</em> shrink. The 2 px bevel and the 2 px
 * black outline are the graphic identity itself: at 1 px they stop reading as a
 * bevel and the interface turns into flat rectangles. Boxes sized by an icon do not
 * shrink either, because an icon may only be drawn at a whole scale — a category
 * tab holds a 16 px icon at exactly x2, so it stays 38 px wide whatever the font is.
 */
public final class Metrics {
    private Metrics() {
    }

    /** Never scaled: the bevel is drawn inside the element, over 2 px. */
    public static final int BEVEL = 2;
    /** Never scaled: a single pure black outline, always the same thickness. */
    public static final int OUTLINE = 2;
    /** A window is told from a panel by a 3 px bevel instead of 2. */
    public static final int WINDOW_BEVEL = 3;
    /** Black rim drawn around a window, outside its outline. */
    public static final int WINDOW_RIM = 4;

    /** Pressing offsets the content by one pixel. That is the whole press feedback. */
    public static final int PRESS_OFFSET = 1;

    // Panels.
    /** Library column, 300 site px. */
    public static final int LIBRARY_WIDTH = ui(300);
    /** Layers column, 318 site px. */
    public static final int LAYERS_WIDTH = ui(318);
    /** A collapsed column keeps just enough room for the button that unfolds it. */
    public static final int COLLAPSED_WIDTH = ui(40);
    /** Top bar, 46 site px. */
    public static final int TOP_BAR_HEIGHT = ui(46);

    /** The scene below this width is not worth showing, so the columns become drawers. */
    public static final int MIN_SCENE_WIDTH = 180;

    // Components.
    /** A category tab holds a 16 px icon at x2 plus its frame; icon-sized, so not scaled. */
    public static final int CATEGORY_TAB = 38;
    /** The category icon, drawn at exactly x2 in the tab row. */
    public static final int CATEGORY_ICON = 32;

    /** Standard vanilla button height; the game wins on widget mechanics. */
    public static final int BUTTON_HEIGHT = 20;
    /** The same button in a crowded strip: the game's font plus padding and outline. */
    public static final int BUTTON_HEIGHT_COMPACT = 18;
    public static final int BUTTON_PAD_X = ui(10);
    public static final int BUTTON_PAD_Y = ui(5);
    /** Buttons of a segmented group are 3 site px apart. */
    public static final int SEGMENT_GAP = ui(3);

    /**
     * A layer row holds a name and a subtitle under it, plus its frame — which is what
     * the site's 26 px row holds too, at its own font size.
     */
    public static final int LAYER_ROW = 22;
    /** The layer's own thumbnail, the size of an inventory icon. */
    public static final int LAYER_PREVIEW = 16;
    /** Letter spacing of a panel title, 1.5 site px rounded to a whole pixel. */
    public static final int TITLE_TRACKING = 1;
    /** Thumbnails sit in a three column grid, 5 site px apart. */
    public static final int GRID_GAP = ui(5);
    public static final int GRID_COLUMNS = 3;
    /** Render area of a thumbnail, 58 site px. */
    public static final int THUMB_RENDER = ui(58);
    /** Transparency checker squares, 8 site px. */
    public static final int CHECKER = ui(8);

    /** Slider rail, 14 site px. */
    public static final int SLIDER_HEIGHT = ui(14);
    public static final int SLIDER_KNOB_WIDTH = ui(10);
    public static final int SLIDER_KNOB_HEIGHT = ui(18);
    /** Slider values are right aligned in a fixed 38 site px column. */
    public static final int SLIDER_VALUE_WIDTH = ui(38);

    /** Checkbox, 16 site px. */
    public static final int CHECKBOX = ui(16);

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
