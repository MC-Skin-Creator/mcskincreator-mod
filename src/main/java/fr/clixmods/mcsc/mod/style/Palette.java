/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.style;

/**
 * The few colours the editor names.
 *
 * <p>Materials are not among them. Panels, slots, buttons, tabs, fields and sliders
 * are the game's own sprites, so their colours belong to the game and to whatever
 * resource pack the player is using — naming them here would mean an invented copy
 * that stops matching the moment either changes.
 *
 * <p>What is left is ink and the two states the game has no sprite for: the coloured
 * band on a notification, and the checker under a transparent thumbnail. The ink
 * values are vanilla's own, taken from where the game uses them rather than picked to
 * taste.
 */
public final class Palette {
    private Palette() {
    }

    /** What the game writes a widget's label in. */
    public static final int INK = 0xFFFFFFFF;
    /** What the game writes a hovered widget's label in. */
    public static final int INK_HOVERED = 0xFFFFFFA0;
    /** Vanilla's grey for secondary text, as used by its own screens. */
    public static final int INK_MUTED = 0xFFA0A0A0;
    /** Fainter still, for a subtitle or an empty state. */
    public static final int INK_FAINT = 0xFF808080;
    /** The grey the game greys a disabled label out to. */
    public static final int INK_DISABLED = 0xFFA0A0A0;
    /** Vanilla's failure red, as used by its own error lines. */
    public static final int INK_FAILURE = 0xFFFF5555;

    /** The rule the game draws under a heading. */
    public static final int RULE = 0xFF5A5A5A;
    /** Black at 74 %, the game's own dimming behind a pop-up. */
    public static final int BACKDROP = 0xBD000000;
    /** A hard drop shadow, for a menu that floats over the screen. */
    public static final int SHADOW = 0x8C000000;

    // The transparency checker. Two greys, and nothing else in the interface uses them.
    public static final int CHECKER_DARK = 0xFF2B2B2B;
    public static final int CHECKER_LIGHT = 0xFF383838;

    /** The band down the flank of a notification: it went well, or it did not. */
    public static final int BAND_SUCCESS = 0xFF3C8527;
    public static final int BAND_FAILURE = 0xFFA03B31;

    /** Applies an alpha in 0..255 to an opaque colour. */
    public static int withAlpha(int argb, int alpha) {
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }
}
