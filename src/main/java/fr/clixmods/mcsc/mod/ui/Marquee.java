/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui;

import fr.clixmods.mcsc.mod.style.Metrics;
import net.minecraft.network.chat.Component;

/**
 * A label that is too long for its box, and what happens when you point at it.
 *
 * <p>At rest it is cut with an ellipsis. Pointing at it, or giving it the focus,
 * slides it along and back so the whole name can be read — and the ellipsis comes off
 * while it moves, because the end of the name is exactly what tells two similar
 * elements apart. It rests at each end rather than turning round on the spot, which
 * is what makes it readable rather than merely animated.
 */
public final class Marquee {
    private static final String ELLIPSIS = "...";

    private long startedAt = -1L;

    /**
     * Draws the label inside {@code maxWidth}.
     *
     * @param active true while the owner is hovered or focused
     */
    public void draw(Paint paint, Component text, int x, int y, int maxWidth, int color, boolean active) {
        Canvas canvas = paint.canvas();
        int full = canvas.textWidth(text);
        if (full <= maxWidth) {
            this.startedAt = -1L;
            canvas.text(text, x, y, color);
            return;
        }

        if (!active) {
            this.startedAt = -1L;
            canvas.text(Component.literal(cut(canvas, text.getString(), maxWidth)), x, y, color);
            return;
        }

        if (this.startedAt < 0L) {
            this.startedAt = paint.time();
        }
        int overflow = full - maxWidth;
        int offset = offset(paint.time() - this.startedAt, overflow);

        // Clipped rather than cut: while it moves, the whole name goes past.
        canvas.pushScissor(x, y - 1, maxWidth, canvas.lineHeight() + 2);
        canvas.text(text, x - offset, y, color);
        canvas.popScissor();
    }

    /** How far along the label has slid, given how long the pointer has been on it. */
    private static int offset(long elapsed, int overflow) {
        long travel = Math.max(1L, Math.round(overflow * 1000.0 / Metrics.MARQUEE_SPEED));
        long pause = Metrics.MARQUEE_PAUSE;
        long cycle = pause + travel + pause + travel;
        long phase = elapsed % cycle;

        if (phase < pause) {
            return 0;
        }
        phase -= pause;
        if (phase < travel) {
            return (int) (overflow * phase / travel);
        }
        phase -= travel;
        if (phase < pause) {
            return overflow;
        }
        phase -= pause;
        return (int) (overflow - overflow * phase / travel);
    }

    /** The resting form: as much of the name as fits, then an ellipsis. */
    public static String cut(Canvas canvas, String text, int maxWidth) {
        if (canvas.textWidth(text) <= maxWidth) {
            return text;
        }
        int room = maxWidth - canvas.textWidth(ELLIPSIS);
        if (room <= 0) {
            return ELLIPSIS;
        }
        return canvas.font().plainSubstrByWidth(text, room) + ELLIPSIS;
    }
}
