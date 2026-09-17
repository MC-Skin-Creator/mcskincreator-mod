/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui.widget;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import fr.clixmods.mcsc.mod.style.Metrics;
import fr.clixmods.mcsc.mod.style.Palette;
import fr.clixmods.mcsc.mod.style.Surface;
import fr.clixmods.mcsc.mod.ui.Canvas;
import fr.clixmods.mcsc.mod.ui.Element;
import fr.clixmods.mcsc.mod.ui.Paint;
import net.minecraft.network.chat.Component;

/**
 * A carved square and its label, clickable as one piece.
 *
 * <p>The box alone is a nine pixel target; together with its label it is the width of
 * the row. The label is part of the control, not a caption next to it, which is also
 * why the two light up together.
 */
public class Checkbox extends Element {
    /** The tick, drawn pixel by pixel rather than borrowed from a font. */
    private static final int[][] TICK = {
        {0, 2}, {1, 3}, {2, 4}, {3, 3}, {4, 2}, {5, 1}, {6, 0},
    };

    private final Component label;
    private final BooleanSupplier read;
    private final Consumer<Boolean> write;

    public Checkbox(Component label, BooleanSupplier read, Consumer<Boolean> write) {
        this.label = label;
        this.read = read;
        this.write = write;
        this.height = Metrics.CHECKBOX;
    }

    @Override
    public void draw(Paint paint) {
        if (!visible()) {
            return;
        }
        Canvas canvas = paint.canvas();
        boolean hot = paint.hot(this);
        boolean checked = this.read.getAsBoolean();
        int box = Metrics.CHECKBOX;

        Surface.slot(canvas, this.x, this.y, box, box, checked ? Palette.GREEN : Palette.SLOT);
        if (checked) {
            int originX = this.x + Metrics.OUTLINE + 1;
            int originY = this.y + Metrics.OUTLINE + 1;
            for (int[] pixel : TICK) {
                canvas.fill(originX + pixel[0], originY + pixel[1], 1, 1, Palette.INK);
            }
        }

        canvas.text(this.label, this.x + box + Metrics.PAD_TIGHT,
                this.y + (box - canvas.lineHeight()) / 2,
                hot ? Palette.GOLD : Palette.INK_MUTED);
    }

    /** The label is part of the target, so the row is as wide as both together. */
    public Checkbox fit(Canvas canvas) {
        this.width = Metrics.CHECKBOX + Metrics.PAD_TIGHT + canvas.textWidth(this.label);
        return this;
    }

    @Override
    public boolean mouseDown(double mouseX, double mouseY, int button) {
        return button == 0 && contains(mouseX, mouseY) && activate();
    }

    @Override
    public boolean activate() {
        this.write.accept(!this.read.getAsBoolean());
        return true;
    }
}
