/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.preview;

import fr.clixmods.mcsc.mod.style.Palette;
import fr.clixmods.mcsc.mod.ui.Canvas;
import fr.clixmods.mcsc.mod.ui.Figure;

/**
 * The player's stand-in.
 *
 * <p>Rendering an actual player needs a running client, and what the preview is for is
 * the room around the figure rather than the figure: whether the view bar is clear of
 * it, whether the dock is sitting on its feet, whether it is being squeezed by a panel
 * that grew. So the stand-in is exactly the rectangle the real one would occupy, drawn
 * plainly enough that it cannot be mistaken for a rendering.
 */
public final class PreviewFigure implements Figure {
    private int x;
    private int y;
    private int width;
    private int height;

    @Override
    public void place(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public void reset() {
    }

    /** The box the player stands in: a hatch, a frame, and how big it came out. */
    @Override
    public void draw(Canvas canvas, int mouseX, int mouseY, float delta) {
        if (this.width <= 0 || this.height <= 0) {
            return;
        }
        for (int row = 0; row < this.height; row += 4) {
            canvas.fill(this.x, this.y + row, this.width, 1, 0x30FFFFFF);
        }
        canvas.fill(this.x, this.y, this.width, 1, Palette.RULE);
        canvas.fill(this.x, this.y + this.height - 1, this.width, 1, Palette.RULE);
        canvas.fill(this.x, this.y, 1, this.height, Palette.RULE);
        canvas.fill(this.x + this.width - 1, this.y, 1, this.height, Palette.RULE);
        canvas.textCentered(net.minecraft.network.chat.Component.literal(
                        this.width + "x" + this.height),
                this.x + this.width / 2, this.y + this.height / 2 - 4, Palette.INK_MUTED);
    }

    @Override
    public boolean press(double mouseX, double mouseY, int button) {
        return false;
    }

    @Override
    public void drag(double mouseX, double mouseY, double dragX, double dragY, int button) {
    }

    @Override
    public void release(double mouseX, double mouseY, int button) {
    }
}
