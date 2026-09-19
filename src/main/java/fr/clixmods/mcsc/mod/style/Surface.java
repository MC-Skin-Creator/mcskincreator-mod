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
 * The four materials every surface of the interface is made of, and nothing else.
 *
 * <p>Stone panel, inventory slot, button, dark surface. There is no fifth: an element
 * that fits none of them is an element that was designed wrong, so this class offers
 * no escape hatch for one.
 *
 * <p>Three rules hold for all four and are enforced here rather than left to each
 * caller. Nothing is rounded. Every framed element carries exactly one pure black
 * outline, two pixels thick. The bevel is drawn <em>inside</em> that outline, over two
 * pixels, never on the border itself — light above and to the left, dark below and to
 * the right, and the other way round for a slot, which is what makes a slot read as
 * carved into the panel instead of sitting on it.
 *
 * <p>These are the site's materials and not the game's. Drawing the editor with
 * Minecraft's own sprites was tried and abandoned: a nine-sliced pop-up plate is built
 * for a panel the size of a dialogue box, and stretched down a column it is a broad
 * light frame around a dark well, with no accent anywhere and nothing to tell a chosen
 * thing from an unchosen one. What the game is right about — its font, its GUI scale,
 * its widget mechanics — this interface still takes. Its paint it does not.
 */
public final class Surface {
    private Surface() {
    }

    /** How a control is being looked at, which decides how its material is painted. */
    public enum State {
        NORMAL, HOVERED, DISABLED
    }

    /** Which family of colours a button or an active state is painted in. */
    public enum Tone {
        /** The default button material. */
        NEUTRAL,
        /** Active state and primary action. One primary per bar, per window footer. */
        GREEN,
        /** Failure and destruction. */
        RED;

        int fill(boolean hovered) {
            return switch (this) {
                case NEUTRAL -> hovered ? Palette.BUTTON_HOVER : Palette.BUTTON;
                case GREEN -> hovered ? Palette.GREEN_HOVER : Palette.GREEN;
                case RED -> hovered ? Palette.RED_HOVER : Palette.RED;
            };
        }

        int top(boolean hovered) {
            return switch (this) {
                case NEUTRAL -> hovered ? Palette.BUTTON_HOVER_TOP : Palette.BUTTON_TOP;
                case GREEN -> hovered ? Palette.GREEN_HOVER_TOP : Palette.GREEN_TOP;
                case RED -> Palette.RED_TOP;
            };
        }

        int bottom(boolean hovered) {
            return switch (this) {
                case NEUTRAL -> hovered ? Palette.BUTTON_HOVER_BOTTOM : Palette.BUTTON_BOTTOM;
                case GREEN -> Palette.GREEN_BOTTOM;
                case RED -> Palette.RED_BOTTOM;
            };
        }
    }

    /** A pure black frame, two pixels thick, drawn on the outer edge of the element. */
    public static void outline(Canvas canvas, int x, int y, int width, int height) {
        int thickness = Metrics.OUTLINE;
        canvas.fill(x, y, width, thickness, Palette.OUTLINE);
        canvas.fill(x, y + height - thickness, width, thickness, Palette.OUTLINE);
        canvas.fill(x, y + thickness, thickness, height - thickness * 2, Palette.OUTLINE);
        canvas.fill(x + width - thickness, y + thickness, thickness, height - thickness * 2,
                Palette.OUTLINE);
    }

    /**
     * The bevel, drawn just inside the outline.
     *
     * <p>{@code light} goes on top and on the left, {@code dark} below and on the
     * right; swapping the two is the whole difference between a raised button and a
     * carved slot. The two opposite corners take {@code mid} so the light and dark
     * bands do not meet in a hard notch.
     */
    public static void bevel(Canvas canvas, int x, int y, int width, int height,
                             int light, int dark, int mid, int thickness) {
        int inset = Metrics.OUTLINE;
        int innerX = x + inset;
        int innerY = y + inset;
        int innerWidth = width - inset * 2;
        int innerHeight = height - inset * 2;
        if (innerWidth <= 0 || innerHeight <= 0) {
            return;
        }
        canvas.fill(innerX, innerY + innerHeight - thickness, innerWidth, thickness, dark);
        canvas.fill(innerX + innerWidth - thickness, innerY, thickness, innerHeight, dark);
        canvas.fill(innerX, innerY, innerWidth, thickness, light);
        canvas.fill(innerX, innerY, thickness, innerHeight, light);
        canvas.fill(innerX + innerWidth - thickness, innerY, thickness, thickness, mid);
        canvas.fill(innerX, innerY + innerHeight - thickness, thickness, thickness, mid);
    }

    /** Stone panel: side columns, notifications, dropdowns. */
    public static void panel(Canvas canvas, int x, int y, int width, int height) {
        framed(canvas, x, y, width, height, Palette.PANEL);
        bevel(canvas, x, y, width, height,
                Palette.PANEL_TOP, Palette.PANEL_BOTTOM, Palette.PANEL_MID, Metrics.BEVEL);
    }

    /**
     * A window: the same stone, a three pixel bevel instead of two, and a black rim
     * around the outside. Those two differences are what tell a window from a panel at
     * a glance, without either one needing a colour of its own.
     */
    public static void window(Canvas canvas, int x, int y, int width, int height) {
        int rim = Metrics.WINDOW_RIM;
        canvas.fill(x - rim, y - rim, width + rim * 2, height + rim * 2, Palette.OUTLINE);
        framed(canvas, x, y, width, height, Palette.PANEL);
        bevel(canvas, x, y, width, height,
                Palette.PANEL_TOP, Palette.PANEL_BOTTOM, Palette.PANEL_MID, Metrics.WINDOW_BEVEL);
    }

    /** Inventory slot: thumbnails, layer rows, category tabs, framed areas. */
    public static void slot(Canvas canvas, int x, int y, int width, int height, int fill) {
        framed(canvas, x, y, width, height, fill);
        // Inverted: dark above and to the left, light below and to the right.
        bevel(canvas, x, y, width, height,
                Palette.SLOT_BOTTOM, Palette.SLOT_TOP, Palette.PANEL_MID, Metrics.BEVEL);
    }

    public static void slot(Canvas canvas, int x, int y, int width, int height) {
        slot(canvas, x, y, width, height, Palette.SLOT);
    }

    /** The same slot, lit: the pointer is on it, or it holds the focus. */
    public static void slotHighlight(Canvas canvas, int x, int y, int width, int height) {
        slot(canvas, x, y, width, height, Palette.SLOT_HOVER);
    }

    /**
     * Button material.
     *
     * <p>Pressing inverts the bevel; the caller shifts the content down by one pixel to
     * match. That pair is the entire press feedback — there is no animation, on the site
     * or here.
     */
    public static void button(Canvas canvas, int x, int y, int width, int height,
                              Tone tone, boolean hovered, boolean pressed) {
        framed(canvas, x, y, width, height, tone.fill(hovered));
        int light = tone.top(hovered);
        int dark = tone.bottom(hovered);
        bevel(canvas, x, y, width, height,
                pressed ? dark : light, pressed ? light : dark, Palette.PANEL_MID, Metrics.BEVEL);
    }

    public static void button(Canvas canvas, int x, int y, int width, int height, State state) {
        button(canvas, x, y, width, height, Tone.NEUTRAL, state == State.HOVERED, false);
    }

    /**
     * One of a group, where the chosen one is green.
     *
     * <p>Green is the site's active state and it is the one thing the vanilla pass had
     * no answer for: with every control the same grey, a row of five regions said
     * nothing about which of them was open.
     */
    public static void tab(Canvas canvas, int x, int y, int width, int height,
                           boolean selected, boolean hovered) {
        button(canvas, x, y, width, height, selected ? Tone.GREEN : Tone.NEUTRAL, hovered, false);
    }

    /** A text field: near-black, and a light border that brightens when it has the caret. */
    public static void field(Canvas canvas, int x, int y, int width, int height, boolean focused) {
        canvas.fill(x, y, width, height, Palette.FIELD);
        int border = focused ? Palette.FIELD_BORDER_FOCUSED : Palette.FIELD_BORDER;
        canvas.fill(x, y, width, 1, border);
        canvas.fill(x, y + height - 1, width, 1, border);
        canvas.fill(x, y, 1, height, border);
        canvas.fill(x + width - 1, y, 1, height, border);
    }

    /** A slider rail: a carved groove, like every other track in the interface. */
    public static void sliderRail(Canvas canvas, int x, int y, int width, int height,
                                  boolean hovered) {
        slot(canvas, x, y, width, height, hovered ? Palette.SLOT_HOVER : Palette.SLOT);
    }

    public static void sliderHandle(Canvas canvas, int x, int y, int height, boolean hovered) {
        button(canvas, x, y, Metrics.SLIDER_HANDLE, height, Tone.NEUTRAL, hovered, false);
    }

    /**
     * A tick box, which is a slot that goes green when it is on.
     *
     * <p>A core of green rather than a drawn tick: at the size a layer row can spare a
     * tick is four pixels of noise, and the colour is legible across the column. The
     * slot stays, so an unticked box is still a box rather than a hole.
     */
    public static void checkbox(Canvas canvas, int x, int y, int size,
                                boolean ticked, boolean hovered) {
        slot(canvas, x, y, size, size, hovered ? Palette.SLOT_HOVER : Palette.SLOT);
        if (ticked) {
            int inset = Metrics.OUTLINE + Metrics.BEVEL;
            canvas.fill(x + inset, y + inset, size - inset * 2, size - inset * 2,
                    hovered ? Palette.GREEN_HOVER : Palette.GREEN);
        }
    }

    /**
     * A caret: the little triangle that says a control opens onto a list.
     *
     * <p>Drawn from the palette rather than blitted, because the game's only arrow is a
     * page-turn arrow — 23 by 13 pixels of near-white, which swamps a control this size
     * and says "next" rather than "more".
     */
    public static void checkbox(Canvas canvas, int x, int y, boolean ticked, boolean hovered) {
        checkbox(canvas, x, y, Metrics.CHECKBOX, ticked, hovered);
    }

    public static void caret(Canvas canvas, int x, int y, int ink) {
        for (int row = 0; row < Metrics.CARET_HEIGHT; row++) {
            int width = Metrics.CARET_WIDTH - row * 2;
            if (width > 0) {
                canvas.fill(x + row, y + row, width, 1, ink);
            }
        }
    }

    /**
     * The transparency checker a thumbnail sits on, so an element with holes in it does
     * not read as an element with black in it.
     */
    public static void checker(Canvas canvas, int x, int y, int width, int height) {
        checker(canvas, x, y, width, height, Metrics.CHECKER);
    }

    /**
     * The same checker at a chosen square size.
     *
     * <p>Every square is a quad, so the count is the area over the square — which is
     * fine behind a thumbnail and ruinous behind the 64x64 sheet, where a fixed square
     * meant five and a half thousand quads a frame and the editor crawled. Behind
     * something drawn at a whole zoom the square belongs in <em>texture</em> pixels:
     * it then scales with the picture, stays put against it, and costs the same
     * sixty-four squares however far in the view is zoomed.
     */
    public static void checker(Canvas canvas, int x, int y, int width, int height, int cell) {
        if (cell < 1) {
            cell = 1;
        }
        for (int row = 0; row * cell < height; row++) {
            for (int column = 0; column * cell < width; column++) {
                int shade = ((row + column) % 2 == 0) ? Palette.CHECKER_DARK : Palette.CHECKER_LIGHT;
                canvas.fill(x + column * cell, y + row * cell,
                        Math.min(cell, width - column * cell),
                        Math.min(cell, height - row * cell), shade);
            }
        }
    }

    /** A flat fill with no grain and no outline — headers, tool strips, the top bar. */
    public static void flat(Canvas canvas, int x, int y, int width, int height, int fill) {
        canvas.fill(x, y, width, height, fill);
    }

    /** A hairline rule, drawn under a heading and between two bands of a panel. */
    public static void rule(Canvas canvas, int x, int y, int width) {
        canvas.fill(x, y, width, 1, Palette.RULE);
    }

    private static void framed(Canvas canvas, int x, int y, int width, int height, int fill) {
        if (width <= 0 || height <= 0) {
            return;
        }
        canvas.fill(x, y, width, height, fill);
        Tiles.lay(canvas, x, y, width, height, fill);
        outline(canvas, x, y, width, height);
    }
}
