/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.style;

import fr.clixmods.mcsc.mod.ui.Canvas;

/**
 * The surfaces the editor is made of, each one drawn with the game's own sprite.
 *
 * <p>The site builds its panels, slots and buttons out of hand-drawn bevels because a
 * web page has nothing else to build them from. A mod does: the game ships every one
 * of these, the player already knows what they mean, and a resource pack that
 * restyles them restyles this screen along with the rest of the game. Nothing here
 * invents a material.
 *
 * <p>The arrangement is still the site's — three columns, a fixed row of category
 * tabs, the stack grouped by region — and so are the states: one choice of a group is
 * the selected tab, hovering lights a control, and a disabled control is dimmed. What
 * changed is the paint, not the plan.
 */
public final class Surface {
    private Surface() {
    }

    /** How a control is being looked at, which decides which sprite it gets. */
    public enum State {
        NORMAL, HOVERED, DISABLED
    }

    /** The panel behind a column, a window or a strip of tools. */
    public static void panel(Canvas canvas, int x, int y, int width, int height) {
        canvas.sprite(Sprites.PANEL, x, y, width, height);
    }

    /**
     * A window.
     *
     * <p>The same panel the game puts behind its own pop-ups. It is told from a column
     * by the dimmed screen behind it rather than by a heavier border, which is how the
     * game itself distinguishes one.
     */
    public static void window(Canvas canvas, int x, int y, int width, int height) {
        canvas.sprite(Sprites.PANEL, x, y, width, height);
    }

    /** A carved slot: thumbnails, layer rows, framed areas. */
    public static void slot(Canvas canvas, int x, int y, int width, int height) {
        canvas.sprite(Sprites.SLOT, x, y, width, height);
    }

    /** The frame the game lays over a slot the pointer is on. */
    public static void slotHighlight(Canvas canvas, int x, int y, int width, int height) {
        canvas.sprite(Sprites.SLOT_HOVERED, x, y, width, height);
    }

    /** Button material. */
    public static void button(Canvas canvas, int x, int y, int width, int height, State state) {
        canvas.sprite(switch (state) {
            case HOVERED -> Sprites.BUTTON_HOVERED;
            case DISABLED -> Sprites.BUTTON_DISABLED;
            case NORMAL -> Sprites.BUTTON;
        }, x, y, width, height);
    }

    /**
     * A tab, which is how the game shows the chosen one of a group.
     *
     * <p>The site marks it with green. Green means nothing in a Minecraft menu, and a
     * selected tab means exactly this, so the tab is the honest translation.
     */
    public static void tab(Canvas canvas, int x, int y, int width, int height,
                           boolean selected, boolean hovered) {
        canvas.sprite(selected
                ? (hovered ? Sprites.TAB_SELECTED_HOVERED : Sprites.TAB_SELECTED)
                : (hovered ? Sprites.TAB_HOVERED : Sprites.TAB),
                x, y, width, height);
    }

    /** A text field, lit when it holds the cursor. */
    public static void field(Canvas canvas, int x, int y, int width, int height, boolean focused) {
        canvas.sprite(focused ? Sprites.FIELD_FOCUSED : Sprites.FIELD, x, y, width, height);
    }

    /** A slider rail and the handle that runs along it. */
    public static void sliderRail(Canvas canvas, int x, int y, int width, int height, boolean hovered) {
        canvas.sprite(hovered ? Sprites.SLIDER_HOVERED : Sprites.SLIDER, x, y, width, height);
    }

    public static void sliderHandle(Canvas canvas, int x, int y, int height, boolean hovered) {
        canvas.sprite(hovered ? Sprites.SLIDER_HANDLE_HOVERED : Sprites.SLIDER_HANDLE,
                x, y, Sprites.SLIDER_HANDLE_WIDTH, height);
    }

    public static void checkbox(Canvas canvas, int x, int y, boolean ticked, boolean hovered) {
        canvas.sprite(ticked
                ? (hovered ? Sprites.CHECKBOX_TICKED_HOVERED : Sprites.CHECKBOX_TICKED)
                : (hovered ? Sprites.CHECKBOX_HOVERED : Sprites.CHECKBOX),
                x, y, Sprites.CHECKBOX_SIZE, Sprites.CHECKBOX_SIZE);
    }

    /**
     * The transparency checker a thumbnail sits on, so an element with holes in it does
     * not read as an element with black in it.
     */
    public static void checker(Canvas canvas, int x, int y, int width, int height) {
        int cell = Metrics.CHECKER;
        for (int row = 0; row * cell < height; row++) {
            for (int column = 0; column * cell < width; column++) {
                int shade = ((row + column) % 2 == 0) ? Palette.CHECKER_DARK : Palette.CHECKER_LIGHT;
                canvas.fill(x + column * cell, y + row * cell,
                        Math.min(cell, width - column * cell),
                        Math.min(cell, height - row * cell), shade);
            }
        }
    }

    /** A hairline rule, the one the game draws between a title and what follows it. */
    public static void rule(Canvas canvas, int x, int y, int width) {
        canvas.fill(x, y, width, 1, Palette.RULE);
    }
}
