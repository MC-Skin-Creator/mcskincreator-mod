/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui.window;

import java.util.List;
import java.util.function.Supplier;

import fr.clixmods.mcsc.mod.style.Metrics;
import fr.clixmods.mcsc.mod.style.Palette;
import fr.clixmods.mcsc.mod.ui.Canvas;
import fr.clixmods.mcsc.mod.ui.Paint;
import fr.clixmods.mcsc.mod.ui.Prose;
import net.minecraft.network.chat.Component;

/**
 * A window that is only prose: About, the beta notice, where an element came from,
 * the credits of a skin.
 *
 * <p>A line can be marked a warning, and then it is a slot with a gold band down its
 * left flank and a gold heading on its own line — never a coloured border, which is
 * the one way this interface never says anything.
 *
 * <p>The lines are read back every frame rather than kept, so a window can be opened
 * on what is already known and fill in as an answer lands. That is what the provenance
 * sheet does: the catalogue names the work at once, and the server adds the models the
 * element is a piece of a moment later, without the window being closed and reopened.
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

    private final Supplier<List<Line>> lines;

    /** Prose that does not change while the window is open. */
    public TextWindow(String titleKey, List<Line> lines, ModalWindow returnsTo) {
        this(titleKey, () -> lines, returnsTo);
    }

    /** Prose that is read again on every frame. */
    public TextWindow(String titleKey, Supplier<List<Line>> lines, ModalWindow returnsTo) {
        super(titleKey, returnsTo);
        this.lines = lines;
    }

    @Override
    protected int contentHeight(Canvas canvas) {
        int height = 0;
        for (Line line : this.lines.get()) {
            height += heightOf(canvas, line);
        }
        return height;
    }

    private int heightOf(Canvas canvas, Line line) {
        int rows = wrapped(canvas, line).size();
        if (line.warning()) {
            return Prose.noteHeight(canvas, rows) + Metrics.PAD_TIGHT;
        }
        return rows * (canvas.lineHeight() + 1) + Metrics.PAD_TIGHT;
    }

    /** Wraps a paragraph to the body width, a note's band and gutter taken off first. */
    private List<String> wrapped(Canvas canvas, Line line) {
        int body = bodyWidth();
        return Prose.wrap(canvas, line.text(), line.warning() ? Prose.noteRoom(body) : body);
    }

    @Override
    protected void layoutBody(Canvas canvas, int left, int top, int width) {
    }

    @Override
    protected void drawBody(Paint paint, int left, int top, int width) {
        Canvas canvas = paint.canvas();
        int cursorY = top;
        for (Line line : this.lines.get()) {
            List<String> rows = wrapped(canvas, line);
            if (line.warning()) {
                Prose.drawNote(canvas, left, cursorY, width, rows);
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
