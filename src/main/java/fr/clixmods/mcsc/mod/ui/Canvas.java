/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui;

import java.util.List;

//? if >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?} else {
import net.minecraft.client.gui.GuiGraphics;
//?}
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * The one drawing surface the whole interface is painted through.
 *
 * <p>26.x replaced immediate-mode GUI drawing with a render-state extraction pass:
 * the object changed name, and so did the text calls on it. Everything else the mod
 * draws with — fills, blits, scissors, tooltips — is byte for byte the same call on
 * both targets. Wrapping the object once, here, is what keeps the rest of the
 * interface free of version conditionals: no panel, no widget and no window
 * mentions a Minecraft version.
 *
 * <p>Rectangles are given as {@code x, y, width, height} throughout. The game takes
 * two corners, which reads fine in one call and badly in a hundred — every widget
 * in this package sizes itself, so a width is what it actually has to hand.
 */
public final class Canvas {
    //? if >=26.1 {
    /*private final GuiGraphicsExtractor graphics;
    *///?} else {
    private final GuiGraphics graphics;
    //?}

    private final Font font;
    private int scissorDepth;

    //? if >=26.1 {
    /*public Canvas(GuiGraphicsExtractor graphics, Font font) {
    *///?} else {
    public Canvas(GuiGraphics graphics, Font font) {
    //?}
        this.graphics = graphics;
        this.font = font;
    }

    public Font font() {
        return this.font;
    }

    public int lineHeight() {
        return this.font.lineHeight;
    }

    public int textWidth(Component text) {
        return this.font.width(text.getVisualOrderText());
    }

    public int textWidth(String text) {
        return this.font.width(text);
    }

    /** Fills a rectangle. Alpha is honoured, which is what draws the window backdrop. */
    public void fill(int x, int y, int width, int height, int argb) {
        if (width <= 0 || height <= 0) {
            return;
        }
        this.graphics.fill(x, y, x + width, y + height, argb);
    }

    /** Text with the game's own one pixel shadow — not a copy of the site's two. */
    public void text(Component text, int x, int y, int argb) {
        //? if >=26.1 {
        /*this.graphics.text(this.font, text, x, y, argb, true);
        *///?} else {
        this.graphics.drawString(this.font, text, x, y, argb, true);
        //?}
    }

    /** Text without a shadow, for placeholders and anything already on a flat fill. */
    public void textFlat(Component text, int x, int y, int argb) {
        //? if >=26.1 {
        /*this.graphics.text(this.font, text, x, y, argb, false);
        *///?} else {
        this.graphics.drawString(this.font, text, x, y, argb, false);
        //?}
    }

    /**
     * A panel title: capitals, letter-spaced.
     *
     * <p>The game's font has no tracking, so the string is drawn a character at a
     * time with a pixel of air between them. It is the one place in the interface
     * that does this, and it is what makes a title read as a title without needing a
     * second font size the game does not have.
     */
    public void textTracked(String text, int x, int y, int argb, int tracking) {
        int cursor = x;
        for (int index = 0; index < text.length(); index++) {
            String glyph = text.substring(index, index + 1);
            textFlat(Component.literal(glyph), cursor, y, argb);
            cursor += width(glyph) + tracking;
        }
    }

    /** Width {@link #textTracked} will take. */
    public int trackedWidth(String text, int tracking) {
        int total = 0;
        for (int index = 0; index < text.length(); index++) {
            total += width(text.substring(index, index + 1)) + tracking;
        }
        return Math.max(0, total - tracking);
    }

    private int width(String glyph) {
        return this.font.width(glyph);
    }

    public void textCentered(Component text, int centerX, int y, int argb) {
        //? if >=26.1 {
        /*this.graphics.centeredText(this.font, text, centerX, y, argb);
        *///?} else {
        this.graphics.drawCenteredString(this.font, text, centerX, y, argb);
        //?}
    }

    /**
     * Text that has to survive being laid over the scene.
     *
     * <p>A shadow is enough over a panel because a panel is one flat dark colour.
     * Over the preview the background is sky and landscape, so the letter is ringed
     * with black on all four sides instead — the site does the same, for the same
     * reason.
     */
    public void textRinged(Component text, int x, int y, int argb) {
        textFlat(text, x - 1, y, 0xFF000000);
        textFlat(text, x + 1, y, 0xFF000000);
        textFlat(text, x, y - 1, 0xFF000000);
        textFlat(text, x, y + 1, 0xFF000000);
        textFlat(text, x, y, argb);
    }

    /**
     * Draws part of a texture, stretching the source rectangle to the target one.
     *
     * <p>Callers scale by whole factors only. Nothing here enforces that, because a
     * fractional scale is a design mistake rather than a runtime one — the rule
     * lives where the sizes are chosen.
     */
    public void blit(Identifier texture, int x, int y, int width, int height,
                     float u, float v, int sourceWidth, int sourceHeight,
                     int textureWidth, int textureHeight) {
        this.graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v,
                width, height, sourceWidth, sourceHeight, textureWidth, textureHeight);
    }

    /**
     * Draws one of the game's own interface sprites at the size asked for.
     *
     * <p>The game knows which sprites are nine-sliced and which tile, and scales their
     * borders itself, so this never stretches a corner — which is the whole reason the
     * editor draws with vanilla sprites rather than with rectangles of its own.
     */
    public void sprite(Identifier sprite, int x, int y, int width, int height) {
        if (width > 0 && height > 0) {
            this.graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height);
        }
    }

    /** The same, tinted — which is how a sprite is dimmed when its control is off. */
    public void spriteTinted(Identifier sprite, int x, int y, int width, int height, int tint) {
        if (width > 0 && height > 0) {
            this.graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height, tint);
        }
    }

    /** The same, tinted — which is also how an icon is dimmed when it is disabled. */
    public void blitTinted(Identifier texture, int x, int y, int width, int height,
                           float u, float v, int sourceWidth, int sourceHeight,
                           int textureWidth, int textureHeight, int tint) {
        this.graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v,
                width, height, sourceWidth, sourceHeight, textureWidth, textureHeight, tint);
    }

    /** Text broken onto as many lines as it needs to stay within {@code lineWidth}. */
    public void textWrapped(Component text, int x, int y, int lineWidth, int argb) {
        //? if >=26.1 {
        /*this.graphics.textWithWordWrap(this.font, text, x, y, lineWidth, argb);
        *///?} else {
        this.graphics.drawWordWrap(this.font, text, x, y, lineWidth, argb);
        //?}
    }

    /** How tall {@link #textWrapped} will be, so a caller can place it from its foot. */
    public int wrappedHeight(Component text, int lineWidth) {
        return this.font.split(text, Math.max(1, lineWidth)).size() * this.font.lineHeight;
    }

    /**
     * Draws one of the game's own widgets.
     *
     * <p>The editor draws its panels itself, in an order it decides, so a vanilla
     * widget added to the screen the usual way would be painted over by whatever came
     * after it. Calling it from here instead puts it exactly where it belongs in that
     * order - which is what lets the player model sit on the scene with the tool
     * strips over it and the panels beside it.
     */
    public void widget(Renderable widget, int mouseX, int mouseY, float delta) {
        //? if >=26.1 {
        /*widget.extractRenderState(this.graphics, mouseX, mouseY, delta);
        *///?} else {
        widget.render(this.graphics, mouseX, mouseY, delta);
        //?}
    }

    /**
     * Clips to a rectangle until the matching {@link #popScissor()}.
     *
     * <p>The game intersects with whatever is already clipped, so a scrolling list
     * inside a collapsed panel cannot paint outside the panel.
     */
    public void pushScissor(int x, int y, int width, int height) {
        this.graphics.enableScissor(x, y, x + width, y + height);
        this.scissorDepth++;
    }

    public void popScissor() {
        if (this.scissorDepth > 0) {
            this.scissorDepth--;
            this.graphics.disableScissor();
        }
    }

    /** Queues a tooltip for this frame; the game draws it above everything else. */
    public void tooltip(List<Component> lines, int mouseX, int mouseY) {
        if (!lines.isEmpty()) {
            this.graphics.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
        }
    }
}
