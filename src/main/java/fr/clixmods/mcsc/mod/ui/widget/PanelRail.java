/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui.widget;

import java.util.Locale;

import fr.clixmods.mcsc.mod.style.Metrics;
import fr.clixmods.mcsc.mod.style.Palette;
import fr.clixmods.mcsc.mod.ui.Canvas;
import fr.clixmods.mcsc.mod.ui.Paint;
import net.minecraft.network.chat.Component;

/**
 * A folded panel's column, under its unfold button: the panel's name written
 * downwards, and the whole strip a way to unfold it.
 *
 * <p>A folded column used to keep the button and nothing else, a square of twenty-two
 * pixels at the top of an empty strip that said nothing of what it would bring back.
 * The site turned its folded panels into this rail, and the mod follows: the name says
 * which panel it is, and the strip is a target the height of the screen.
 *
 * <p>A ghost button underneath — the press, the sound, the focus and the tooltip are a
 * button's — with no material of its own: the column's stone shows through, and the
 * name takes the gold hover ink, as every label does.
 */
public class PanelRail extends PixelButton {
    public PanelRail(Component name, Runnable unfold) {
        super(name, Style.GHOST, unfold);
    }

    @Override
    public void draw(Paint paint) {
        if (!visible()) {
            return;
        }
        Canvas canvas = paint.canvas();
        String name = label().getString().toUpperCase(Locale.ROOT);
        int ink = paint.hot(this) ? Palette.GOLD : Palette.INK_DIM;
        // Clipped to the strip: on a window too short for the whole name, the end of it
        // goes rather than running down over the frame.
        canvas.pushScissor(this.x, this.y, this.width, this.height);
        canvas.textTrackedDown(name, this.x + (this.width - canvas.lineHeight()) / 2,
                this.y + Metrics.PAD_TIGHT, ink, Metrics.TITLE_TRACKING);
        canvas.popScissor();
    }
}
