/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.style;

/**
 * The site's palette, transcribed from its stylesheet and derived from nothing.
 *
 * <p>Every value here is one of the custom properties in the site's own CSS — the
 * names in the comments are those properties — so the mod and the site cannot drift
 * apart by taste. If a colour on the site changes, it changes here, and nowhere else
 * in this source tree names a colour at all.
 *
 * <p>The palette is dark from end to end and it is the identity of the product. An
 * attempt was made to replace all of it with Minecraft's own interface sprites, on the
 * reasoning that a mod should look like the game it is in; what that produced was a
 * light grey plate with a heavy bevel around every column, no accent colour anywhere,
 * and nothing on the screen to tell a chosen thing from an unchosen one. The game's
 * sprites are right for a menu of six buttons and wrong for a workbench.
 */
public final class Palette {
    private Palette() {
    }

    /** Opaque black. The one and only border colour — state never travels by border. */
    public static final int OUTLINE = 0xFF000000;

    // Stone panel: windows, side panels, notifications, dropdowns. --gui
    public static final int PANEL = 0xFF2E2E34;
    public static final int PANEL_TOP = 0xFF53535E;
    public static final int PANEL_BOTTOM = 0xFF131317;
    public static final int PANEL_MID = 0xFF4A4A54;
    /** --gui-head: the strip a panel's title sits on, and the top bar. */
    public static final int PANEL_HEADER = 0xFF26262B;
    /** --gui-sub: a band inside a panel that is neither stone nor slot. */
    public static final int PANEL_SUB = 0xFF34343B;

    // Inventory slot: thumbnails, layer rows, category tabs, framed areas. Carved in.
    public static final int SLOT = 0xFF1B1B20;
    public static final int SLOT_TOP = 0xFF4A4A54;
    public static final int SLOT_BOTTOM = 0xFF0B0B0E;
    public static final int SLOT_HOVER = 0xFF272730;

    // Button: every action. --btn
    public static final int BUTTON = 0xFF63636B;
    public static final int BUTTON_TOP = 0xFF95959F;
    public static final int BUTTON_BOTTOM = 0xFF33333A;
    public static final int BUTTON_HOVER = 0xFF767688;
    public static final int BUTTON_HOVER_TOP = 0xFFA9A9BD;
    public static final int BUTTON_HOVER_BOTTOM = 0xFF3D3D4A;

    /** Green marks the active state and the primary action. --green */
    public static final int GREEN = 0xFF3C8527;
    public static final int GREEN_TOP = 0xFF5AA838;
    public static final int GREEN_BOTTOM = 0xFF23511A;
    public static final int GREEN_HOVER = 0xFF4A9C30;
    public static final int GREEN_HOVER_TOP = 0xFF6BBF47;
    /** The green a selected row is tinted with: the fill, not a border. */
    public static final int GREEN_FILL = 0xFF1F3320;

    /** Red marks failure and destruction. --red */
    public static final int RED = 0xFFA03B31;
    public static final int RED_TOP = 0xFFC45A4D;
    public static final int RED_BOTTOM = 0xFF5E1F19;
    public static final int RED_HOVER = 0xFFB8473B;

    // Dark surfaces: tool strips, scene background. --dark, --dark2, --void
    public static final int DARK = 0xFF26262B;
    public static final int DARKER = 0xFF18181C;
    public static final int VOID = 0xFF101014;

    // Inks. --txt and its three dim steps.
    public static final int INK = 0xFFFFFFFF;
    /** --gold: hover ink, everywhere, on every material. */
    public static final int INK_HOVERED = 0xFFFCFC54;
    public static final int INK_MUTED = 0xFFC9C9C9;
    /** --txt-dim2: a subtitle, a count, a heading that is not the point of its row. */
    public static final int INK_FAINT = 0xFF9A9A9A;
    /** --txt-dim3: a placeholder, and what a disabled label greys out to. */
    public static final int INK_DISABLED = 0xFF7C7C84;
    /** Failure ink, readable on stone. */
    public static final int INK_FAILURE = 0xFFFF9D8F;
    /** --gold, by its own name: the colour, where a caller means the colour. */
    public static final int GOLD = INK_HOVERED;
    /** --txt-dim2, by its own name. */
    public static final int INK_DIM = INK_FAINT;
    /** --lime: brighter than the green, for a mark that has to read at seven pixels. */
    public static final int LIME = 0xFF7EC850;

    // Text field: a light border is what tells a field apart from a carved slot.
    public static final int FIELD = 0xFF0A0A0C;
    public static final int FIELD_BORDER = 0xFF6A6A72;
    public static final int FIELD_BORDER_FOCUSED = 0xFFA0A0A0;

    /** The hairline drawn under a heading, and between two bands of a panel. */
    public static final int RULE = 0xFF4A4A54;
    /** Black at 74 %, behind a window. */
    public static final int BACKDROP = 0xBD000000;
    /** A hard drop shadow, for a menu that floats over the screen. */
    public static final int SHADOW = 0x8C000000;

    // The transparency checker, so an element with holes does not read as one with
    // black in it.
    public static final int CHECKER_DARK = DARKER;
    public static final int CHECKER_LIGHT = VOID;

    /** The band down the flank of a notification: it went well, or it did not. */
    public static final int BAND_SUCCESS = GREEN;
    public static final int BAND_FAILURE = RED;

    /** Applies an alpha in 0..255 to an opaque colour. */
    public static int withAlpha(int argb, int alpha) {
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }
}
