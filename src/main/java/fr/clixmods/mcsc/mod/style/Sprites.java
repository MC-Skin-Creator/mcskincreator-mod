/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.style;

import net.minecraft.resources.Identifier;

/**
 * The game's own interface sprites, named once.
 *
 * <p>Every surface in this editor is drawn with these rather than with pixels the mod
 * made up. That is not only a matter of fitting in: a resource pack that restyles
 * Minecraft's buttons restyles this screen's buttons with it, and a sprite that moves
 * in a future version moves here too, where an invented copy would quietly stop
 * matching the game around it.
 *
 * <p>All of them are present and identical on every supported version — checked
 * against both jars, not assumed. The ones used at sizes of the mod's choosing are
 * nine-sliced, so the game scales their borders and the mod never stretches a corner.
 */
public final class Sprites {
    private Sprites() {
    }

    // Buttons. Nine-sliced from 200x20 with a 3 px border.
    public static final Identifier BUTTON = vanilla("widget/button");
    public static final Identifier BUTTON_HOVERED = vanilla("widget/button_highlighted");
    public static final Identifier BUTTON_DISABLED = vanilla("widget/button_disabled");

    /**
     * Tabs, which is how the game shows one choice of several — the region tabs, the
     * category tabs and the model chooser all use them rather than a colour of the
     * mod's own devising.
     */
    public static final Identifier TAB = vanilla("widget/tab");
    public static final Identifier TAB_HOVERED = vanilla("widget/tab_highlighted");
    public static final Identifier TAB_SELECTED = vanilla("widget/tab_selected");
    public static final Identifier TAB_SELECTED_HOVERED = vanilla("widget/tab_selected_highlighted");

    /** The panel behind a window or a column. Nine-sliced from 236x34. */
    public static final Identifier PANEL = vanilla("popup/background");

    /** A carved slot, at whatever size it is given. Nine-sliced from 24x24. */
    public static final Identifier SLOT = vanilla("container/bundle/slot_background");
    /** The frame the game draws over a slot the pointer is on. */
    public static final Identifier SLOT_HOVERED = vanilla("container/bundle/slot_highlight_front");

    // Text fields.
    public static final Identifier FIELD = vanilla("widget/text_field");
    public static final Identifier FIELD_FOCUSED = vanilla("widget/text_field_highlighted");

    // Sliders.
    public static final Identifier SLIDER = vanilla("widget/slider");
    public static final Identifier SLIDER_HOVERED = vanilla("widget/slider_highlighted");
    public static final Identifier SLIDER_HANDLE = vanilla("widget/slider_handle");
    public static final Identifier SLIDER_HANDLE_HOVERED = vanilla("widget/slider_handle_highlighted");

    // Checkboxes, at their own 20x20.
    public static final Identifier CHECKBOX = vanilla("widget/checkbox");
    public static final Identifier CHECKBOX_HOVERED = vanilla("widget/checkbox_highlighted");
    public static final Identifier CHECKBOX_TICKED = vanilla("widget/checkbox_selected");
    public static final Identifier CHECKBOX_TICKED_HOVERED = vanilla("widget/checkbox_selected_highlighted");

    // Scroll rails.
    public static final Identifier SCROLLER = vanilla("widget/scroller");
    public static final Identifier SCROLLER_TRACK = vanilla("widget/scroller_background");

    /** The close cross of a window, at its own 14x14. */
    public static final Identifier CROSS = vanilla("widget/cross_button");
    public static final Identifier CROSS_HOVERED = vanilla("widget/cross_button_highlighted");

    /** The page arrows, at their own 23x13. Used for a dropdown and for folding a panel. */
    public static final Identifier ARROW_RIGHT = vanilla("widget/page_forward");
    public static final Identifier ARROW_RIGHT_HOVERED = vanilla("widget/page_forward_highlighted");
    public static final Identifier ARROW_LEFT = vanilla("widget/page_backward");
    public static final Identifier ARROW_LEFT_HOVERED = vanilla("widget/page_backward_highlighted");

    // Sizes the game fixes, for callers that have to lay out around them.
    public static final int CHECKBOX_SIZE = 20;
    public static final int CROSS_SIZE = 14;
    public static final int ARROW_WIDTH = 23;
    public static final int ARROW_HEIGHT = 13;
    public static final int SLIDER_HANDLE_WIDTH = 8;
    public static final int SCROLLER_WIDTH = 6;

    private static Identifier vanilla(String path) {
        return Identifier.withDefaultNamespace(path);
    }
}
