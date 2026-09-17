/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui.window;

import java.util.List;

import fr.clixmods.mcsc.mod.style.Metrics;
import fr.clixmods.mcsc.mod.style.Palette;
import fr.clixmods.mcsc.mod.style.Surface;
import fr.clixmods.mcsc.mod.ui.Canvas;
import fr.clixmods.mcsc.mod.ui.Paint;
import net.minecraft.network.chat.Component;

/**
 * A window that is only prose: About, the beta notice, where an element came from,
 * the credits of a skin.
 *
 * <p>A line can be marked a warning, and then it is a slot with a gold band down its
 * left flank and a gold heading on its own line — never a coloured border, which is
 * the one way this interface never says anything.
 */
public class TextWindow extends ModalWindow {
    /**
     * One paragraph.
     *
     * @param warning true to set the paragraph in a framed, gold-banded note
     */
    public record Line(Component text, boolean warning) {
        public static Line of(String key) {
            return new Line(Component.translatable(key), false);
        }

        public static Line warning(String key) {
            return new Line(Component.translatable(key), true);
        }
    }

    private final List<Line> lines;

    public TextWindow(String titleKey, List<Line> lines, ModalWindow returnsTo) {
        super(titleKey, returnsTo);
        this.lines = lines;
    }

    @Override
    protected int contentHeight(Canvas canvas) {
        int height = 0;
        for (Line line : this.lines) {
            height += heightOf(canvas, line);
        }
        return height;
    }

    private int heightOf(Canvas canvas, Line line) {
        int rows = wrapped(canvas, line).size();
        int text = rows * (canvas.lineHeight() + 1) + Metrics.PAD_TIGHT;
        return line.warning() ? text + canvas.lineHeight() + Metrics.PAD_TIGHT * 3 : text;
    }

    /**
     * Wraps a paragraph to the body width by hand.
     *
     * <p>The game can wrap text itself, but only straight onto the screen; the window
     * needs the line count first to know how tall it is before it draws anything.
     */
    private List<String> wrapped(Canvas canvas, Line line) {
        int room = width() - Metrics.PAD * 2 - (line.warning() ? Metrics.BAND + Metrics.PAD : 0);
        List<String> rows = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : line.text().getString().split(" ")) {
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

    @Override
    protected void layoutBody(Canvas canvas, int left, int top, int width) {
    }

    @Override
    protected void drawBody(Paint paint, int left, int top, int width) {
        Canvas canvas = paint.canvas();
        int cursorY = top;
        for (Line line : this.lines) {
            List<String> rows = wrapped(canvas, line);
            if (line.warning()) {
                int boxHeight = heightOf(canvas, line) - Metrics.PAD_TIGHT;
                Surface.slot(canvas, left, cursorY, width, boxHeight, Palette.SLOT);
                canvas.fill(left + Metrics.OUTLINE, cursorY + Metrics.OUTLINE,
                        Metrics.BAND, boxHeight - Metrics.OUTLINE * 2, Palette.GOLD);
                int textX = left + Metrics.BAND + Metrics.PAD;
                canvas.text(Component.translatable("gui.mcskincreator.warning"),
                        textX, cursorY + Metrics.PAD_TIGHT, Palette.GOLD);
                int rowY = cursorY + Metrics.PAD_TIGHT + canvas.lineHeight() + Metrics.PAD_TIGHT;
                for (String row : rows) {
                    canvas.text(Component.literal(row), textX, rowY, Palette.INK_MUTED);
                    rowY += canvas.lineHeight() + 1;
                }
            } else {
                for (String row : rows) {
                    canvas.text(Component.literal(row), left, cursorY, Palette.INK_MUTED);
                    cursorY += canvas.lineHeight() + 1;
                }
                cursorY += Metrics.PAD_TIGHT;
                continue;
            }
            cursorY += heightOf(canvas, line);
        }
    }
}
