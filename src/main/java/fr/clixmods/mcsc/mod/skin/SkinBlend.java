/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.skin;

import com.mojang.blaze3d.platform.NativeImage;

/**
 * One element shown against the whole skin, composed here rather than asked of the
 * server.
 *
 * <p>Pointing at a tile in the library used to <em>replace</em> the previewed skin with
 * that one element: the character vanished and a pair of floating eyes took its place,
 * which answers "what does this look like" by removing everything it would look like
 * against. {@link #over} lays it on top instead, which is what picking it would do — so
 * the preview is honest, being the same composition.
 *
 * <p>The other half of the question, where a layer already in the stack <em>sits</em>,
 * is {@link fr.clixmods.mcsc.mod.skin.Highlight}: that one pulses, chooses its flash
 * colour per texel, and shows a layer nothing of which is visible. This is only the
 * straight lay-over.
 */
public final class SkinBlend {
    private SkinBlend() {
    }

    /** Copies an image, because every blend leaves the one it was given untouched. */
    public static NativeImage copy(NativeImage source) {
        NativeImage copy = new NativeImage(source.getWidth(), source.getHeight(), false);
        copy.copyFrom(source);
        return copy;
    }

    /**
     * The element laid over the skin, exactly as stacking it would.
     *
     * @param element a raw RGBA buffer the size of a skin, or null for nothing to do
     */
    public static NativeImage over(NativeImage base, byte[] element) {
        NativeImage result = copy(base);
        if (element == null) {
            return result;
        }
        forEachPixel(element, result, (image, x, y, argb) -> {
            int alpha = (argb >>> 24) & 0xFF;
            if (alpha == 0) {
                return;
            }
            if (alpha == 0xFF) {
                ImagePixels.set(image, x, y, argb);
                return;
            }
            ImagePixels.set(image, x, y, blend(ImagePixels.get(image, x, y), argb, alpha));
        });
        return result;
    }

    private interface Pixel {
        void set(NativeImage image, int x, int y, int argb);
    }

    /**
     * Walks a raw RGBA buffer against an image of the same size.
     *
     * <p>{@link ImagePixels} speaks packed ARGB on every target, and the buffers the API
     * sends are RGBA in byte order, so the channels are rearranged here once rather than
     * at each of the two call sites.
     */
    private static void forEachPixel(byte[] rgba, NativeImage image, Pixel action) {
        int size = FrontSprite.SKIN_SIZE;
        if (rgba.length != size * size * 4
                || image.getWidth() != size || image.getHeight() != size) {
            return;
        }
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int offset = (y * size + x) * 4;
                int argb = (rgba[offset + 3] & 0xFF) << 24
                        | (rgba[offset] & 0xFF) << 16
                        | (rgba[offset + 1] & 0xFF) << 8
                        | rgba[offset + 2] & 0xFF;
                action.set(image, x, y, argb);
            }
        }
    }

    /** Blends one packed colour over another by an alpha in 0..255, channel by channel. */
    private static int blend(int under, int over, int alpha) {
        return (Math.max((under >>> 24) & 0xFF, alpha) << 24)
                | mix(under, over, alpha, 16) << 16
                | mix(under, over, alpha, 8) << 8
                | mix(under, over, alpha, 0);
    }

    private static int mix(int under, int over, int alpha, int shift) {
        int a = (under >>> shift) & 0xFF;
        int b = (over >>> shift) & 0xFF;
        return (b * alpha + a * (0xFF - alpha)) / 0xFF;
    }
}
