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
 * <p>Stone panel, inventory slot, button, dark surface. There is no fifth material:
 * an element that fits none of them is an element that was designed wrong, so this
 * class offers no escape hatch for one.
 *
 * <p>Three rules hold for all four and are enforced here rather than left to each
 * caller. Nothing is rounded. Every framed element carries exactly one pure black
 * outline, two pixels thick. The bevel is drawn <em>inside</em> that outline, over
 * two pixels, never on the border itself — light above and to the left, dark below
 * and to the right, and the other way round for a slot, which is what makes a slot
 * read as carved into the panel instead of sitting on it.
 */
public final class Surface {
    private Surface() {
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

        /** Ink that stays readable on this fill, hovered or not. */
        public int ink(boolean hovered) {
            if (this == RED && hovered) {
                return Palette.RED_INK;
            }
            return hovered ? Palette.GOLD : Palette.INK;
        }
    }

    /** A pure black frame, two pixels thick, drawn on the outer edge of the element. */
    public static void outline(Canvas canvas, int x, int y, int width, int height) {
        int t = Metrics.OUTLINE;
        canvas.fill(x, y, width, t, Palette.OUTLINE);
        canvas.fill(x, y + height - t, width, t, Palette.OUTLINE);
        canvas.fill(x, y + t, t, height - t * 2, Palette.OUTLINE);
        canvas.fill(x + width - t, y + t, t, height - t * 2, Palette.OUTLINE);
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

    /** Stone panel: windows, side panels, notifications, dropdowns. */
    public static void panel(Canvas canvas, int x, int y, int width, int height) {
        fillFramed(canvas, x, y, width, height, Palette.PANEL);
        bevel(canvas, x, y, width, height,
                Palette.PANEL_TOP, Palette.PANEL_BOTTOM, Palette.PANEL_MID, Metrics.BEVEL);
    }

    /**
     * A window: the same stone, a three pixel bevel instead of two, and a black rim
     * around the outside. Those two differences are what tell a window from a panel
     * at a glance, without either one needing a colour of its own.
     */
    public static void window(Canvas canvas, int x, int y, int width, int height) {
        int rim = Metrics.WINDOW_RIM;
        canvas.fill(x - rim, y - rim, width + rim * 2, height + rim * 2, Palette.OUTLINE);
        fillFramed(canvas, x, y, width, height, Palette.PANEL);
        bevel(canvas, x, y, width, height,
                Palette.PANEL_TOP, Palette.PANEL_BOTTOM, Palette.PANEL_MID, Metrics.WINDOW_BEVEL);
    }

    /** Inventory slot: thumbnails, layer rows, category tabs, framed areas. */
    public static void slot(Canvas canvas, int x, int y, int width, int height, int fill) {
        fillFramed(canvas, x, y, width, height, fill);
        // Inverted: dark above and to the left, light below and to the right.
        bevel(canvas, x, y, width, height,
                Palette.SLOT_BOTTOM, Palette.SLOT_TOP, Palette.PANEL_MID, Metrics.BEVEL);
    }

    public static void slot(Canvas canvas, int x, int y, int width, int height) {
        slot(canvas, x, y, width, height, Palette.SLOT);
    }

    /**
     * Button material.
     *
     * <p>Pressing inverts the bevel; the caller shifts the content down by one pixel
     * to match. That pair is the entire press feedback — there is no animation, on
     * the site or here.
     */
    public static void button(Canvas canvas, int x, int y, int width, int height,
                              Tone tone, boolean hovered, boolean pressed) {
        fillFramed(canvas, x, y, width, height, tone.fill(hovered));
        int light = tone.top(hovered);
        int dark = tone.bottom(hovered);
        bevel(canvas, x, y, width, height,
                pressed ? dark : light, pressed ? light : dark, Palette.PANEL_MID, Metrics.BEVEL);
    }

    /** Dark surface: tool strips and scene background. Outlined, never bevelled. */
    public static void dark(Canvas canvas, int x, int y, int width, int height, int fill) {
        fillFramed(canvas, x, y, width, height, fill);
    }

    /** A flat fill with no grain and no outline — headers, sub-panels, rails. */
    public static void flat(Canvas canvas, int x, int y, int width, int height, int fill) {
        canvas.fill(x, y, width, height, fill);
    }

    /**
     * The transparency checker a thumbnail sits on: two greys in squares, so an
     * element with holes in it does not read as an element with black in it.
     */
    public static void checker(Canvas canvas, int x, int y, int width, int height) {
        int cell = Metrics.CHECKER;
        for (int row = 0; row * cell < height; row++) {
            for (int column = 0; column * cell < width; column++) {
                int shade = ((row + column) % 2 == 0) ? Palette.DARKER : Palette.EMPTY;
                canvas.fill(x + column * cell, y + row * cell,
                        Math.min(cell, width - column * cell),
                        Math.min(cell, height - row * cell), shade);
            }
        }
    }

    private static void fillFramed(Canvas canvas, int x, int y, int width, int height, int fill) {
        canvas.fill(x, y, width, height, fill);
        Tiles.lay(canvas, x, y, width, height, fill);
        outline(canvas, x, y, width, height);
    }
}
