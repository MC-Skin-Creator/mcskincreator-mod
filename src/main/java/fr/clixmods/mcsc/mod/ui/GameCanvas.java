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
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;

/**
 * The {@link Canvas} the game paints on, and the one file in the interface that knows
 * which Minecraft version it is compiled against.
 *
 * <p>26.x replaced immediate-mode GUI drawing with a render-state extraction pass: the
 * graphics object changed name, and so did the text calls on it. Everything else the
 * mod draws with — fills, blits, scissors, tooltips — is byte for byte the same call
 * on both targets. Wrapping the object once, here, is what keeps the rest of the
 * interface free of version conditionals: no panel, no widget and no window mentions a
 * Minecraft version.
 */
public final class GameCanvas implements Canvas {
    //? if >=26.1 {
    /*private final GuiGraphicsExtractor graphics;
    *///?} else {
    private final GuiGraphics graphics;
    //?}

    private final Font font;
    private int scissorDepth;

    //? if >=26.1 {
    /*public GameCanvas(GuiGraphicsExtractor graphics, Font font) {
    *///?} else {
    public GameCanvas(GuiGraphics graphics, Font font) {
    //?}
        this.graphics = graphics;
        this.font = font;
    }

    @Override
    public String trimToWidth(String text, int room) {
        return this.font.plainSubstrByWidth(text, room);
    }

    @Override
    public int lineHeight() {
        return this.font.lineHeight;
    }

    @Override
    public int textWidth(Component text) {
        return this.font.width(text.getVisualOrderText());
    }

    @Override
    public int textWidth(String text) {
        return this.font.width(text);
    }

    /**
     * The fraction the small text is drawn at.
     *
     * <p>A half and nothing else. At the even GUI scales this editor takes, half a
     * game pixel is a whole screen pixel, so the bitmap lands on the grid exactly;
     * two thirds or three quarters would land between pixels and smear every glyph.
     */
    private static final float SMALL = 0.5F;

    @Override
    public int smallLineHeight() {
        return (lineHeight() + 1) / 2;
    }

    @Override
    public int smallTextWidth(Component text) {
        return (textWidth(text) + 1) / 2;
    }

    @Override
    public int smallTextWidth(String text) {
        return (textWidth(text) + 1) / 2;
    }

    @Override
    public String trimToSmallWidth(String text, int room) {
        return trimToWidth(text, room * 2);
    }

    @Override
    public void textSmall(Component text, int x, int y, int argb) {
        // Halve the matrix and double the coordinates: the glyphs land where they were
        // asked for, at half the size. The scissor in force was set in screen pixels
        // and is unaffected, because it is state and not part of the matrix.
        Matrix3x2fStack pose = this.graphics.pose();
        pose.pushMatrix();
        pose.scale(SMALL, SMALL);
        //? if >=26.1 {
        /*this.graphics.text(this.font, text, x * 2, y * 2, argb, false);
        *///?} else {
        this.graphics.drawString(this.font, text, x * 2, y * 2, argb, false);
        //?}
        pose.popMatrix();
    }

    /** Fills a rectangle. Alpha is honoured, which is what draws the window backdrop. */
    @Override
    public void fill(int x, int y, int width, int height, int argb) {
        if (width <= 0 || height <= 0) {
            return;
        }
        this.graphics.fill(x, y, x + width, y + height, argb);
    }

    /** Text with the game's own one pixel shadow — not a copy of the site's two. */
    @Override
    public void text(Component text, int x, int y, int argb) {
        //? if >=26.1 {
        /*this.graphics.text(this.font, text, x, y, argb, true);
        *///?} else {
        this.graphics.drawString(this.font, text, x, y, argb, true);
        //?}
    }

    /** Text without a shadow, for placeholders and anything already on a flat fill. */
    @Override
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
    @Override
    public void textTracked(String text, int x, int y, int argb, int tracking) {
        int cursor = x;
        for (int index = 0; index < text.length(); index++) {
            String glyph = text.substring(index, index + 1);
            textFlat(Component.literal(glyph), cursor, y, argb);
            cursor += width(glyph) + tracking;
        }
    }

    /** Width {@link #textTracked} will take. */
    @Override
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

    @Override
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
    @Override
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
    @Override
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
    @Override
    public void sprite(Identifier sprite, int x, int y, int width, int height) {
        if (width > 0 && height > 0) {
            this.graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height);
        }
    }

    /** The same, tinted — which is how a sprite is dimmed when its control is off. */
    @Override
    public void spriteTinted(Identifier sprite, int x, int y, int width, int height, int tint) {
        if (width > 0 && height > 0) {
            this.graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height, tint);
        }
    }

    /** The same, tinted — which is also how an icon is dimmed when it is disabled. */
    @Override
    public void blitTinted(Identifier texture, int x, int y, int width, int height,
                           float u, float v, int sourceWidth, int sourceHeight,
                           int textureWidth, int textureHeight, int tint) {
        this.graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v,
                width, height, sourceWidth, sourceHeight, textureWidth, textureHeight, tint);
    }

    /** Text broken onto as many lines as it needs to stay within {@code lineWidth}. */
    @Override
    public void textWrapped(Component text, int x, int y, int lineWidth, int argb) {
        //? if >=26.1 {
        /*this.graphics.textWithWordWrap(this.font, text, x, y, lineWidth, argb);
        *///?} else {
        this.graphics.drawWordWrap(this.font, text, x, y, lineWidth, argb);
        //?}
    }

    /** How tall {@link #textWrapped} will be, so a caller can place it from its foot. */
    @Override
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
    @Override
    public void widget(Renderable widget, int mouseX, int mouseY, float delta) {
        //? if >=26.1 {
        /*widget.extractRenderState(this.graphics, mouseX, mouseY, delta);
        *///?} else {
        widget.render(this.graphics, mouseX, mouseY, delta);
        //?}
    }

    /**
     * The game's own picture-in-picture path, the one the inventory portrait goes
     * through. 26.x renamed it along with everything else that draws.
     */
    @Override
    public void entity(EntityRenderState state, float scale, Vector3f translation,
                       Quaternionf rotation, Quaternionf overrideCameraAngle,
                       int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        //? if >=26.1 {
        /*this.graphics.entity(state, scale, translation, rotation, overrideCameraAngle,
                x, y, x + width, y + height);
        *///?} else {
        this.graphics.submitEntityRenderState(state, scale, translation, rotation,
                overrideCameraAngle, x, y, x + width, y + height);
        //?}
    }

    /**
     * Clips to a rectangle until the matching {@link #popScissor()}.
     *
     * <p>The game intersects with whatever is already clipped, so a scrolling list
     * inside a collapsed panel cannot paint outside the panel.
     */
    @Override
    public void pushScissor(int x, int y, int width, int height) {
        this.graphics.enableScissor(x, y, x + width, y + height);
        this.scissorDepth++;
    }

    @Override
    public void popScissor() {
        if (this.scissorDepth > 0) {
            this.scissorDepth--;
            this.graphics.disableScissor();
        }
    }

    /** Queues a tooltip for this frame; the game draws it above everything else. */
    @Override
    public void tooltip(List<Component> lines, int mouseX, int mouseY) {
        if (!lines.isEmpty()) {
            this.graphics.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
        }
    }
}
