/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.style;

import com.mojang.blaze3d.platform.NativeImage;

import fr.clixmods.mcsc.mod.MCSkinCreatorClient;
import fr.clixmods.mcsc.mod.ui.Canvas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * The grain under the stone panels and the dirt background.
 *
 * <p>The site draws its stone and its dirt from deterministic noise rather than
 * shipping a texture, and the mod does the same for one practical reason: a single
 * greyscale tile, tinted at draw time, gives every material its own grain without a
 * second asset and without the tile ever going out of step with the palette.
 *
 * <p>The noise is seeded from the pixel coordinates, so the tile is identical on
 * every machine and every launch — two players comparing screenshots see the same
 * stone.
 */
public final class Tiles {
    /** Big enough that the repeat does not read as a pattern, small enough to stay cheap. */
    public static final int SIZE = 64;

    private static final Identifier GRAIN =
            Identifier.fromNamespaceAndPath(MCSkinCreatorClient.MOD_ID, "generated/grain");

    private static boolean registered;

    private Tiles() {
    }

    /** The tile's name, which the preview answers with a grain of its own. */
    public static Identifier texture() {
        return GRAIN;
    }

    /** Builds and registers the tile on first use. Safe to call every frame. */
    public static void ensureRegistered(Minecraft client) {
        if (registered) {
            return;
        }
        registered = true;

        NativeImage image = new NativeImage(SIZE, SIZE, false);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                image.setPixel(x, y, grey(shade(x, y)));
            }
        }
        client.getTextureManager().register(GRAIN, new DynamicTexture(() -> "mcsc grain", image));
    }

    /**
     * Lays the grain over a rectangle, multiplied by {@code tint}.
     *
     * <p>The tile is drawn at 1:1 and the last row and column are cut short rather
     * than squashed: a stretched grain reads as a gradient, and the interface has no
     * gradients.
     */
    public static void lay(Canvas canvas, int x, int y, int width, int height, int tint) {
        for (int offsetY = 0; offsetY < height; offsetY += SIZE) {
            int slice = Math.min(SIZE, height - offsetY);
            for (int offsetX = 0; offsetX < width; offsetX += SIZE) {
                int column = Math.min(SIZE, width - offsetX);
                canvas.blitTinted(GRAIN, x + offsetX, y + offsetY, column, slice,
                        0.0F, 0.0F, column, slice, SIZE, SIZE, tint);
            }
        }
    }

    /**
     * Lightness of one grain pixel, from 226 to 255.
     *
     * <p>Kept near white on purpose: the tile is a multiplier, so anything darker
     * would drag every material away from the palette it is supposed to be.
     */
    public static int shade(int x, int y) {
        int hash = x * 374761393 + y * 668265263;
        hash = (hash ^ (hash >>> 13)) * 1274126177;
        hash ^= hash >>> 16;
        // Three bands rather than smooth noise: the speckle has to read as pixels.
        return switch (Math.floorMod(hash, 8)) {
            case 0, 1 -> 226;
            case 2, 3, 4 -> 240;
            default -> 255;
        };
    }

    public static int grey(int level) {
        return 0xFF000000 | (level << 16) | (level << 8) | level;
    }
}
