/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui.widget;

import fr.clixmods.mcsc.mod.catalog.ThumbCrop;
import fr.clixmods.mcsc.mod.skin.CategorySprites;
import fr.clixmods.mcsc.mod.skin.FrontSprite;
import fr.clixmods.mcsc.mod.ui.Canvas;

/**
 * Draws one element's front view out of its category's sheet.
 *
 * <p>Shared by the library's tiles and the layer rows, because they show the same
 * thing at two sizes and there is no reason for the crop arithmetic to exist twice.
 *
 * <p>Whole scales only. A skin drawn at one and a half times its size is a blurred
 * skin, and the whole point of a pixel editor is that it is not — so the thumbnail is
 * centred in its box at the largest whole multiple that fits rather than stretched to
 * fill it.
 *
 * <p>And it never draws outside that box: a crop too big for its box even at a scale of
 * one is narrowed at the source rather than clipped, so no caller needs a scissor. That
 * matters more than it sounds — a scissor flushes the interface's draw batch, so one per
 * thumbnail is one flush per thumbnail, and a grid of them crawls.
 */
public final class Thumbnail {
    private Thumbnail() {
    }

    /**
     * @param crop  the part of the body this element's category asks to show
     * @param index the element's buffer in the sheet
     * @return true when something was drawn; false while the atlas is still on its way
     */
    public static boolean draw(Canvas canvas, CategorySprites sprites, int index, ThumbCrop crop,
                               int boxX, int boxY, int boxWidth, int boxHeight) {
        if (sprites == null || index < 0 || index >= sprites.count()) {
            return false;
        }

        // A few elements are drawn outside the part of the body their category claims.
        // Widening the crop rescues those; the ones drawn only on faces a front view
        // cannot show are past rescuing, and come out empty either way.
        ThumbCrop shown = sprites.covers(index, crop) ? crop : ThumbCrop.ALL;
        int scale = Math.max(1, Math.min(boxWidth / shown.width(), boxHeight / shown.height()));

        // A crop too big for its box at the smallest whole scale is narrowed at the
        // source instead of being drawn past the edges. Clipping it with a scissor
        // would work and would cost a batch flush per thumbnail, which on a grid of
        // forty is forty — the models window was unusable for exactly that reason.
        int sourceWidth = Math.min(shown.width(), Math.max(1, boxWidth / scale));
        int sourceHeight = Math.min(shown.height(), Math.max(1, boxHeight / scale));
        int sourceX = shown.x() + (shown.width() - sourceWidth) / 2;
        int sourceY = shown.y() + (shown.height() - sourceHeight) / 2;
        int drawnWidth = sourceWidth * scale;
        int drawnHeight = sourceHeight * scale;

        canvas.blit(sprites.texture(),
                boxX + (boxWidth - drawnWidth) / 2, boxY + (boxHeight - drawnHeight) / 2,
                drawnWidth, drawnHeight,
                sprites.spriteU(index) + sourceX, sprites.spriteV(index) + sourceY,
                sourceWidth, sourceHeight,
                sprites.sheetWidth(), sprites.sheetHeight());
        return true;
    }

    /** The proportions a thumbnail box wants, so a tile can be sized before it draws. */
    public static int heightFor(int width, ThumbCrop crop) {
        return Math.max(1, width * crop.height() / Math.max(1, crop.width()));
    }

    /** The front view's own proportions, for a box that has to hold any crop. */
    public static int wholeHeightFor(int width) {
        return Math.max(1, width * FrontSprite.HEIGHT / FrontSprite.WIDTH);
    }
}
