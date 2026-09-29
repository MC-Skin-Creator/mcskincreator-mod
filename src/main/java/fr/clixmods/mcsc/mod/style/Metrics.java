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
 * <p>The site is laid out around an <strong>11 px</strong> font; the game's is 8 px
 * tall. Every spacing, every line height and every control height is therefore
 * {@code 8/11} of its reference value — {@link #ui(int)} does that conversion and
 * nothing else does it ad hoc. The reference values are not guesses: they are the
 * numbers in the site's own stylesheet, and each one is named in the comment beside
 * its constant.
 *
 * <p><strong>A column is the exception, and it is not converted that way.</strong> A
 * spacing is a length and scales with the font; a column is a share of the screen and
 * scales with the screen. {@link #share(int)} is that second conversion, and mixing
 * the two up is what walled this editor in: {@code ui(300)} gave the library 218 px of
 * the 960 it has, where the site gives it 300 of about 1900.
 *
 * <p>One thing is deliberately <em>not</em> converted: a whole multiple stays whole —
 * a thumbnail is drawn at x1 or x2, never at x1.4.
 *
 * <p>These numbers assume the room {@link fr.clixmods.mcsc.mod.ui.EditorScale} gets
 * them: 960 by 540, which is a 1080p window at the scale that screen takes for itself.
 * At the scale most people play at the same window is 480 by 270, and no amount of
 * tightening fits three columns and a model into that.
 */
public final class Metrics {
    private Metrics() {
    }

    /**
     * The three gaps this interface uses, and there is no fourth.
     *
     * <p>The site's are 8, 5–6 and 3. A gap is a gap: a screen with five of them is a
     * screen where nothing lines up with anything.
     */
    public static final int PAD = ui(8);
    public static final int PAD_TIGHT = ui(6);
    public static final int PAD_HAIR = ui(3);

    /**
     * The black frame around every material, and the bevel drawn just inside it.
     *
     * <p>{@code border: 2px solid var(--edge)} appears 36 times in the site's
     * stylesheet, and these were 2 here as well on the reasoning that the frame is the
     * graphic identity and would stop reading at one pixel. Seen in a client, it was
     * the opposite: two and two is four pixels of frame around a control sixteen
     * across, which leaves eight for the glyph and makes a button look like a hole
     * with something at the bottom of it.
     *
     * <p>The frame is a length like any other, so it converts like one. The site sets
     * it in a layout whose font is 11; at {@code 8/11} that is one and a half, and a
     * frame is either a pixel or two, never one and a half. One — and at the even GUI
     * scales this editor takes, one interface pixel is two on the screen, which is the
     * thickness the site's frame has there too.
     */
    public static final int OUTLINE = 1;
    public static final int BEVEL = 1;
    /** A window is told from a panel by a deeper bevel and a black rim, not by a colour. */
    public static final int WINDOW_BEVEL = 2;
    public static final int WINDOW_RIM = 1;

    // Where a material's content starts, so a label sits clear of the frame around it.
    /** A panel's frame and bevel, and a pixel of air past them. */
    public static final int PANEL_INSET = OUTLINE + BEVEL + 2;
    /** A slot's frame and bevel. */
    public static final int SLOT_INSET = OUTLINE + BEVEL;
    /** A button's frame and bevel. */
    public static final int BUTTON_INSET = OUTLINE + BEVEL;
    /** A field's border, and a pixel that keeps the caret clear of it. */
    public static final int FIELD_INSET = OUTLINE + 1;

    /**
     * One control height, and there is no second.
     *
     * <p>The site draws every button, every segment of a group and every dropdown at
     * {@code padding: 5px 10px} over an 11 px line, which is 27 px tall. Converted, 20.
     * Three heights on adjacent rows was the whole of why nothing used to line up.
     */
    public static final int BUTTON_HEIGHT = ui(27);
    public static final int TAB_HEIGHT = BUTTON_HEIGHT;
    public static final int BUTTON_HEIGHT_COMPACT = BUTTON_HEIGHT;
    /** {@code .btn { padding: 5px 10px }} — the horizontal half of it. */
    public static final int BUTTON_PAD_X = ui(10);
    /** {@code .seg { gap: 3px }} between the segments of one group. */
    public static final int SEGMENT_GAP = ui(3);

    /**
     * A button in a panel's header: the fold chevron, and the plus that adds a layer.
     *
     * <p>{@code .head-actions .btn.add-layer { padding: 2px 7px }} — the site's header
     * buttons are shorter than its ordinary ones, and they are buttons. Drawing them as
     * bare glyphs, as this did, left two of the interface's most used controls with no
     * edge to aim at.
     */
    public static final int HEADER_BUTTON = ui(22);

    /** {@code input[type=checkbox] { width: 16px }}. */
    public static final int CHECKBOX = ui(16);
    /** {@code .layer .eye { width: 26px }} — the toggle on a layer row is bigger. */
    public static final int LAYER_TOGGLE = ui(26);
    /** {@code input[type=range]::-webkit-slider-thumb { width: 10px }}. */
    public static final int SLIDER_HANDLE = ui(10);
    /** {@code ::-webkit-slider-runnable-track { height: 14px }}. */
    public static final int SLIDER_RAIL = ui(14);
    /** {@code .layer .ops button { width: 26px }} and the window's close cross. */
    public static final int CROSS = ui(22);
    /** A scroll rail, narrow enough to be a gutter rather than a column. */
    public static final int RAIL_WIDTH = ui(9);
    /** The triangle that says a control opens onto a list, and the room kept for it. */
    public static final int CARET_WIDTH = 7;
    public static final int CARET_HEIGHT = 4;
    /** {@code select { padding-right: 22px }} — the room a dropdown keeps for its caret. */
    public static final int CARET_ROOM = ui(22);

    // The thumbnail grid comes first because the library column is measured from
    // it: three tiles is the floor a column may not go under.
    /** {@code .cat-body { grid-template-columns: repeat(3, …); gap: 5px; padding: 7px }}. */
    public static final int GRID_GAP = ui(5);
    public static final int GRID_COLUMNS = 3;
    /**
     * {@code .thumb canvas { height: 58px }} — and it is a <em>fixed</em> height.
     *
     * <p>The site letterboxes every thumbnail into the same box, whatever the crop, so
     * the grid is a grid. Sizing each tile to its own crop instead gave a column of
     * ragged rows, and full-body elements tiles twice as tall as head ones.
     */
    public static final int THUMB_RENDER = ui(58);
    /** {@code .thumb .lbl { margin-top: 3px }} and {@code padding: 3px 2px 2px}. */
    public static final int THUMB_PAD = ui(3);
    /** {@code .thumb.used:after { width: 7px }} — the mark on an element already in play. */
    public static final int THUMB_MARK = ui(7);
    /** {@code .thumb .info { width: 15px }} — the "i" that says where an element came from. */
    public static final int INFO_MARK = ui(15);
    /** Transparency checker squares: {@code background-size: 8px 8px}. */
    public static final int CHECKER = ui(8);
    /**
     * The narrowest a tile may be while its label still says anything.
     *
     * <p>Not the site's {@code minmax(94px, 1fr)} — that is its mobile grid. Its desktop
     * grid is {@code repeat(3, minmax(0, 1fr))}, three columns whatever the width. What
     * this is is the floor underneath them: a tile is the thumbnail plus a name, and a
     * name cut to four letters and an ellipsis is not a name. At this width six or seven
     * letters survive, which with the picture above them is enough to tell two elements
     * apart; below it the grid drops to two columns rather than print nonsense.
     *
     * <p>It is also what sets the library column's width, through {@code threeTiles()}:
     * the column is measured from the tile, not the other way round.
     */
    public static final int MIN_TILE_WIDTH = ui(72);

    // Panels. The site's columns are a SHARE of its width, not a count of pixels:
    // .workspace { grid-template-columns: 300px minmax(0,1fr) 318px } sits in a layout
    // about 1900 px across, so they are a sixth and a sixth of it. Putting those two
    // numbers through ui() was the mistake that made this screen feel walled in —
    // ui() converts a length against the font, and a column is not a length, it is a
    // fraction of the room there is. 300 of 1900 is 16 % of the screen; the 218 that
    // came out of ui(300) is 23 % of the 960 this editor gets, and the two columns
    // together took very nearly half of it.
    /** {@code 300px} of about 1900, or three thumbnails, whichever is the wider. */
    public static final int LIBRARY_WIDTH = Math.max(share(300), threeTiles());
    /** {@code 318px} of about 1900 — wider than the library, because a layer row holds more. */
    public static final int LAYERS_WIDTH = share(318);

    /** {@code .workspace.lib-collapsed { grid-template-columns: 40px … }}. */
    public static final int COLLAPSED_WIDTH = ui(40);
    /** {@code .topbar { min-height: 46px }}. */
    public static final int TOP_BAR_HEIGHT = ui(46);

    /** The scene below this width is not worth showing, so the columns become drawers. */
    public static final int MIN_SCENE_WIDTH = ui(320);

    /** {@code .cat-tab { width: 38px }}, holding a 32 px icon. */
    public static final int CATEGORY_TAB = ui(38);

    /**
     * The model is given most of the scene but not all of it: the strips and the
     * reminder in the corners need somewhere to sit that is not on top of the player.
     */
    public static final double SCENE_MODEL_SHARE = 0.78;

    /** {@code .layer { min-height: 46px }} — name, subtitle and the controls beside them. */
    public static final int LAYER_ROW = ui(46);
    /** {@code .layer .mini-prev { width: 26px }}. */
    public static final int LAYER_PREVIEW = ui(26);

    /** A window is 600 site px wide at most and 88 % of the screen tall. */
    public static final int WINDOW_MAX_WIDTH = ui(600);
    public static final double WINDOW_MAX_HEIGHT_RATIO = 0.88;

    /** A notification stays up for 2.4 s. */
    public static final long TOAST_MILLIS = 2400L;
    /** Coloured band on the left flank of a notification, a warning or a chosen row. */
    public static final int BAND = ui(6);

    /** A label too long to fit scrolls at 45 site px per second... */
    public static final double MARQUEE_SPEED = ui(45);
    /** ...and rests 450 ms at each end. */
    public static final long MARQUEE_PAUSE = 450L;

    /** {@code .panel-head h2 { letter-spacing: 1.5px }}, rounded to a whole pixel. */
    public static final int TITLE_TRACKING = 1;

    /** The history keeps 60 states. */
    public static final int HISTORY_DEPTH = 60;

    /** An element name is never shown beyond 24 characters, in any language. */
    public static final int MAX_NAME_CHARS = 24;

    /**
     * The width the site lays out in, and the width this editor is laid out for.
     *
     * <p>960 is a 1080p window at the scale {@link fr.clixmods.mcsc.mod.ui.EditorScale}
     * takes; 1900 is where the site's own layout stops growing. The ratio between them
     * is what a column is converted by.
     */
    private static final int SITE_WIDTH = 1900;
    private static final int DESIGN_WIDTH = 960;

    /**
     * Converts a site width to the share of this screen it was a share of over there.
     *
     * <p>For anything that is a column rather than a length: what matters is that a
     * column takes the same <em>portion</em> of the screen it takes on the site, not
     * that it holds the same number of pixels.
     */
    public static int share(int sitePixels) {
        return Math.round(sitePixels * (float) DESIGN_WIDTH / SITE_WIDTH);
    }

    /**
     * The narrowest a library column can be and still show three thumbnails.
     *
     * <p>The site's desktop grid is {@code repeat(3, minmax(0, 1fr))} — three columns
     * whatever the width — so three is not negotiable, and a column narrower than this
     * would quietly become two.
     */
    private static int threeTiles() {
        return GRID_COLUMNS * MIN_TILE_WIDTH + (GRID_COLUMNS - 1) * GRID_GAP
                + PANEL_INSET * 2 + RAIL_WIDTH;
    }

    /**
     * Converts a site measurement to a game one: {@code 8/11}, rounded, never below 2.
     *
     * <p>The floor of 2 is not cosmetic. A 1 px gap between two dark surfaces is
     * invisible, so anything that rounded down to 1 would silently merge the two
     * elements it was meant to separate.
     */
    public static int ui(int sitePixels) {
        return Math.max(2, Math.round(sitePixels * 8.0f / 11.0f));
    }
}
