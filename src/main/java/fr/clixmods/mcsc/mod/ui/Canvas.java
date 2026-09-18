/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui;

import java.util.List;

import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * The one drawing surface the whole interface is painted through.
 *
 * <p>Every panel, widget and window in the mod draws through this and through nothing
 * else. That is what lets the same interface be painted somewhere that is not the
 * game: {@link GameCanvas} paints onto the game's own graphics object, and the
 * preview tool in the tests paints the very same calls into an image, so a layout can
 * be looked at without a window, a GPU or a login.
 *
 * <p>It is an interface rather than a class for that reason alone. A second
 * implementation is not a hypothetical extension point: it is the only way to see
 * this screen in an environment that cannot run Minecraft, and a screen nobody can
 * see is a screen nobody checks.
 *
 * <p>Rectangles are given as {@code x, y, width, height} throughout. The game takes
 * two corners, which reads fine in one call and badly in a hundred — every widget in
 * this package sizes itself, so a width is what it actually has to hand.
 */
public interface Canvas {
    /** How tall one line of the game's font is. */
    int lineHeight();

    int textWidth(Component text);

    int textWidth(String text);

    /** As much of {@code text} as fits in {@code room} pixels, cut on a whole glyph. */
    String trimToWidth(String text, int room);

    /** Fills a rectangle. Alpha is honoured, which is what draws the window backdrop. */
    void fill(int x, int y, int width, int height, int argb);

    /** Text with the game's own one pixel shadow — not a copy of the site's two. */
    void text(Component text, int x, int y, int argb);

    /** Text without a shadow, for placeholders and anything already on a flat fill. */
    void textFlat(Component text, int x, int y, int argb);

    /**
     * A panel title: capitals, letter-spaced.
     *
     * <p>The game's font has no tracking, so the string is drawn a character at a time
     * with a pixel of air between them. It is the one place in the interface that does
     * this, and it is what makes a title read as a title without needing a second font
     * size the game does not have.
     */
    void textTracked(String text, int x, int y, int argb, int tracking);

    /** Width {@link #textTracked} will take. */
    int trackedWidth(String text, int tracking);

    void textCentered(Component text, int centerX, int y, int argb);

    /**
     * Text that has to survive being laid over the scene.
     *
     * <p>A shadow is enough over a panel because a panel is one flat dark colour. Over
     * the preview the background is sky and landscape, so the letter is ringed with
     * black on all four sides instead — the site does the same, for the same reason.
     */
    void textRinged(Component text, int x, int y, int argb);

    /** Text broken onto as many lines as it needs to stay within {@code lineWidth}. */
    void textWrapped(Component text, int x, int y, int lineWidth, int argb);

    /** How tall {@link #textWrapped} will be, so a caller can place it from its foot. */
    int wrappedHeight(Component text, int lineWidth);

    /**
     * Draws part of a texture, stretching the source rectangle to the target one.
     *
     * <p>Callers scale by whole factors only. Nothing here enforces that, because a
     * fractional scale is a design mistake rather than a runtime one — the rule lives
     * where the sizes are chosen.
     */
    void blit(Identifier texture, int x, int y, int width, int height,
              float u, float v, int sourceWidth, int sourceHeight,
              int textureWidth, int textureHeight);

    /** The same, tinted — which is how an icon is dimmed when it is disabled. */
    void blitTinted(Identifier texture, int x, int y, int width, int height,
                    float u, float v, int sourceWidth, int sourceHeight,
                    int textureWidth, int textureHeight, int tint);

    /**
     * Draws one of the game's own interface sprites at the size asked for.
     *
     * <p>The game knows which sprites are nine-sliced and which tile, and scales their
     * borders itself, so this never stretches a corner — which is the whole reason the
     * editor draws with vanilla sprites rather than with rectangles of its own.
     */
    void sprite(Identifier sprite, int x, int y, int width, int height);

    /** The same, tinted — which is how a sprite is dimmed when its control is off. */
    void spriteTinted(Identifier sprite, int x, int y, int width, int height, int tint);

    /**
     * Draws one of the game's own widgets.
     *
     * <p>The editor draws its panels itself, in an order it decides, so a vanilla
     * widget added to the screen the usual way would be painted over by whatever came
     * after it. Calling it from here instead puts it exactly where it belongs in that
     * order — which is what lets the player model sit on the scene with the tool
     * strips over it and the panels beside it.
     */
    void widget(Renderable widget, int mouseX, int mouseY, float delta);

    /**
     * Clips to a rectangle until the matching {@link #popScissor()}.
     *
     * <p>The game intersects with whatever is already clipped, so a scrolling list
     * inside a collapsed panel cannot paint outside the panel.
     */
    void pushScissor(int x, int y, int width, int height);

    void popScissor();

    /** Queues a tooltip for this frame; the game draws it above everything else. */
    void tooltip(List<Component> lines, int mouseX, int mouseY);
}
