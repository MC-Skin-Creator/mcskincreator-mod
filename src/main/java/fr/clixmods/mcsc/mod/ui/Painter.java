/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui;

//? if >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?} else {
import net.minecraft.client.gui.GuiGraphics;
//?}
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Everything the editor's panels draw with, behind one name per operation.
 *
 * <p>26.x renamed the class that carries a screen's drawing calls, and renamed the
 * text calls on it. The type is the problem: a method taking it cannot be written
 * once, so any shared code that draws text would have to exist twice. Wrapping it
 * here pays that cost a single time - a field, a constructor and two text calls - and
 * leaves every panel written once, in plain shared code.
 *
 * <p>Only the text calls actually differ. Fills, blits and the scissor stack are the
 * same on both targets and are ordinary methods below; they are here for the panels'
 * convenience, not because they need a conditional.
 */
final class Painter {
    private final Font font;

    //? if >=26.1 {
    /*private final GuiGraphicsExtractor graphics;

    Painter(GuiGraphicsExtractor graphics, Font font) {
        this.graphics = graphics;
        this.font = font;
    }

    void text(Component text, int x, int y, int color) {
        this.graphics.text(this.font, text, x, y, color);
    }

    void centeredText(Component text, int centerX, int y, int color) {
        this.graphics.centeredText(this.font, text, centerX, y, color);
    }
    *///?} else {
    private final GuiGraphics graphics;

    Painter(GuiGraphics graphics, Font font) {
        this.graphics = graphics;
        this.font = font;
    }

    void text(Component text, int x, int y, int color) {
        this.graphics.drawString(this.font, text, x, y, color);
    }

    void centeredText(Component text, int centerX, int y, int color) {
        this.graphics.drawCenteredString(this.font, text, centerX, y, color);
    }
    //?}

    /** Fills the rectangle between the two corners, {@code x2} and {@code y2} excluded. */
    void fill(int x1, int y1, int x2, int y2, int argb) {
        this.graphics.fill(x1, y1, x2, y2, argb);
    }

    /** A one-pixel frame just inside the given rectangle. */
    void frame(int x, int y, int width, int height, int argb) {
        this.fill(x, y, x + width, y + 1, argb);
        this.fill(x, y + height - 1, x + width, y + height, argb);
        this.fill(x, y + 1, x + 1, y + height - 1, argb);
        this.fill(x + width - 1, y + 1, x + width, y + height - 1, argb);
    }

    /**
     * Draws a rectangle of a texture at its own scale.
     *
     * @param u          the left edge to read from, in the texture's pixels
     * @param v          the top edge to read from, in the texture's pixels
     * @param textureWidth  the whole texture's width, which is what turns {@code u} into
     *                      a coordinate
     * @param textureHeight the whole texture's height
     */
    void blit(Identifier texture, int x, int y, float u, float v, int width, int height,
            int textureWidth, int textureHeight) {
        this.graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height,
                textureWidth, textureHeight);
    }

    void pushScissor(int x, int y, int width, int height) {
        this.graphics.enableScissor(x, y, x + width, y + height);
    }

    void popScissor() {
        this.graphics.disableScissor();
    }

    void tooltip(Component text, int mouseX, int mouseY) {
        this.graphics.setTooltipForNextFrame(this.font, text, mouseX, mouseY);
    }
}
