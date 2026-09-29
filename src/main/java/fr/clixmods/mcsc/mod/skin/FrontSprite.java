/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.skin;

import fr.clixmods.mcsc.engine.Composition;

/**
 * Projects a 64x64 skin to the 16x32 front view the library shows in its slots.
 *
 * <p>The projection is {@code mcsc-engine}'s, the same call the site makes for its own
 * thumbnails, so a shelf of elements is framed and blended here exactly as it is in the
 * browser. This class used to hold its own copy of it — twelve rectangles and an
 * integer blend — which came out a shade different on every semi-transparent texel of
 * an outer layer, for no gain anyone asked for.
 *
 * <p>What is left is the conversion between the two ways of writing a buffer: the
 * atlases carry RGBA bytes and the library draws into an ARGB sheet. Nothing here
 * touches the game, which keeps it usable from the thread that decompressed the atlas.
 */
public final class FrontSprite {
    public static final int WIDTH = 16;
    public static final int HEIGHT = 32;
    /** The side of a skin, in pixels; both dimensions, and the only size the API serves. */
    public static final int SKIN_SIZE = 64;

    private FrontSprite() {
    }

    /**
     * Writes one front view into an ARGB buffer.
     *
     * @param skin   the skin's pixels, {@value #SKIN_SIZE} squared, four bytes per
     *               pixel in RGBA order - exactly what an atlas buffer holds
     * @param slim   whether to read the three-pixel-wide arms of the slim model
     * @param target the buffer written into, ARGB, laid out row by row
     * @param stride the width of {@code target}, in pixels
     * @param left   where in {@code target} the sprite's left edge goes
     * @param top    where in {@code target} the sprite's top edge goes
     */
    public static void draw(byte[] skin, boolean slim, int[] target, int stride, int left, int top) {
        int[] sprite = Composition.frontSprite(toInts(skin), slim);
        for (int row = 0; row < HEIGHT; row++) {
            for (int column = 0; column < WIDTH; column++) {
                int offset = (row * WIDTH + column) * 4;
                target[(top + row) * stride + left + column] = argb(sprite, offset);
            }
        }
    }

    /**
     * The library reads a buffer as ints of 0 to 255, the way JavaScript hands it a
     * {@code Uint8ClampedArray}; atlases arrive as bytes, which are signed.
     */
    private static int[] toInts(byte[] skin) {
        int[] values = new int[Composition.BYTES];
        for (int index = 0; index < values.length; index++) {
            values[index] = skin[index] & 0xFF;
        }
        return values;
    }

    /** One RGBA pixel of the sprite, as the ARGB a texture is uploaded from. */
    private static int argb(int[] sprite, int offset) {
        return sprite[offset + 3] << 24
                | sprite[offset] << 16
                | sprite[offset + 1] << 8
                | sprite[offset + 2];
    }
}
