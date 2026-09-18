/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui;

import java.util.ArrayList;
import java.util.List;

import fr.clixmods.mcsc.mod.style.Metrics;
import fr.clixmods.mcsc.mod.style.Palette;
import fr.clixmods.mcsc.mod.style.Surface;
import net.minecraft.network.chat.Component;

/**
 * Paragraphs, and the framed note a paragraph becomes when it is a warning.
 *
 * <p>Here rather than inside one window because two of them need it: the prose windows
 * are all text, and the export window has to say, under its cards, why a card it is not
 * showing is not there. A second copy of the wrapping would be a second place for the
 * height and the drawing to disagree, and they disagree invisibly — as a paragraph
 * clipped by one row.
 *
 * <p>A note is a slot with a gold band down its left flank and a gold heading, never a
 * coloured border: state never travels by border in this interface.
 */
public final class Prose {
    private Prose() {
    }

    /**
     * Wraps a paragraph to {@code room} pixels.
     *
     * <p>The game can wrap text itself, but only straight onto the screen; a window
     * needs the row count first, to know how tall it is before it draws anything.
     */
    public static List<String> wrap(Canvas canvas, Component text, int room) {
        List<String> rows = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.getString().split(" ")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (canvas.textWidth(candidate) > room && !current.isEmpty()) {
                rows.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(candidate);
            }
        }
        rows.add(current.toString());
        return rows;
    }

    /** How much of a body's width a note's text gets, the band and its gutter aside. */
    public static int noteRoom(int width) {
        return width - Metrics.BAND - Metrics.PAD;
    }

    /** How tall the framed note holding {@code rows} rows is. */
    public static int noteHeight(Canvas canvas, int rows) {
        return rows * (canvas.lineHeight() + 1) + canvas.lineHeight() + Metrics.PAD_TIGHT * 3;
    }

    /** Draws the framed note: the band, the heading, then the rows. */
    public static void drawNote(Canvas canvas, int x, int y, int width, List<String> rows) {
        int height = noteHeight(canvas, rows.size());
        Surface.slot(canvas, x, y, width, height, Palette.SLOT);
        canvas.fill(x + Metrics.OUTLINE, y + Metrics.OUTLINE,
                Metrics.BAND, height - Metrics.OUTLINE * 2, Palette.GOLD);

        int textX = x + Metrics.BAND + Metrics.PAD;
        canvas.text(Component.translatable("gui.mcskincreator.warning"),
                textX, y + Metrics.PAD_TIGHT, Palette.GOLD);

        int rowY = y + Metrics.PAD_TIGHT + canvas.lineHeight() + Metrics.PAD_TIGHT;
        for (String row : rows) {
            canvas.text(Component.literal(row), textX, rowY, Palette.INK_MUTED);
            rowY += canvas.lineHeight() + 1;
        }
    }
}
