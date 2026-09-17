/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.style;

/**
 * The colours of the MC Skin Creator interface, transcribed from the site.
 *
 * <p>The palette is deliberately dark from end to end. On the site that started as
 * a workaround — browsers in forced dark mode re-invert light backgrounds — but it
 * has become the identity of the product, so the mod keeps it rather than falling
 * back on the light vanilla inventory panel.
 *
 * <p>Every value is opaque ARGB, which is what the game's drawing calls expect.
 * Nothing here is computed from anything else: a palette that derives its shades
 * drifts from the reference the moment one constant is touched.
 */
public final class Palette {
    private Palette() {
    }

    /** Opaque black. The one and only border colour — state never travels by border. */
    public static final int OUTLINE = 0xFF000000;

    // Stone panel: windows, side panels, notifications, dropdowns.
    public static final int PANEL = 0xFF2E2E34;
    public static final int PANEL_TOP = 0xFF53535E;
    public static final int PANEL_BOTTOM = 0xFF131317;
    public static final int PANEL_MID = 0xFF4A4A54;
    public static final int PANEL_HEADER = 0xFF26262B;
    public static final int PANEL_SUB = 0xFF34343B;

    // Inventory slot: thumbnails, layer rows, category tabs, framed areas. Carved in.
    public static final int SLOT = 0xFF1B1B20;
    public static final int SLOT_TOP = 0xFF4A4A54;
    public static final int SLOT_BOTTOM = 0xFF0B0B0E;
    public static final int SLOT_HOVER = 0xFF272730;

    // Button: every action.
    public static final int BUTTON = 0xFF63636B;
    public static final int BUTTON_TOP = 0xFF95959F;
    public static final int BUTTON_BOTTOM = 0xFF33333A;
    public static final int BUTTON_HOVER = 0xFF767688;
    public static final int BUTTON_HOVER_TOP = 0xFFA9A9BD;
    public static final int BUTTON_HOVER_BOTTOM = 0xFF3D3D4A;

    /** Green marks the active state and the primary action. */
    public static final int GREEN = 0xFF3C8527;
    public static final int GREEN_TOP = 0xFF5AA838;
    public static final int GREEN_BOTTOM = 0xFF23511A;
    public static final int GREEN_HOVER = 0xFF4A9C30;
    public static final int GREEN_HOVER_TOP = 0xFF6BBF47;

    /** Red marks failure and destruction. */
    public static final int RED = 0xFFA03B31;
    public static final int RED_TOP = 0xFFC45A4D;
    public static final int RED_BOTTOM = 0xFF5E1F19;
    public static final int RED_HOVER = 0xFFB8473B;
    public static final int RED_INK = 0xFFFFD0C8;

    // Dark surfaces: tool strips, scene background.
    public static final int DARK = 0xFF26262B;
    public static final int DARKER = 0xFF18181C;
    public static final int EMPTY = 0xFF101014;

    // Inks.
    public static final int INK = 0xFFFFFFFF;
    public static final int INK_MUTED = 0xFFC9C9C9;
    public static final int INK_DIM = 0xFF9A9A9A;
    public static final int INK_FAINT = 0xFF7C7C84;
    /** Hover ink, everywhere. */
    public static final int GOLD = 0xFFFCFC54;
    public static final int GREEN_LIGHT = 0xFF7EC850;

    // Field: a light border is what tells a text field apart from a carved slot.
    public static final int FIELD = 0xFF0A0A0C;
    public static final int FIELD_BORDER = 0xFFA0A0A0;
    public static final int FIELD_PLACEHOLDER = 0xFF6A6A6A;

    /** Background of the selected layer row. State travels by fill, never by border. */
    public static final int SELECTED_FILL = 0xFF1F3320;
    /** Failure ink on a notification. */
    public static final int FAIL_INK = 0xFFFF9D8F;
    /** Backdrop behind a window: black at 74 %. */
    public static final int BACKDROP = 0xBD000000;

    /** Applies an alpha in 0..255 to an opaque colour. */
    public static int withAlpha(int argb, int alpha) {
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }
}
