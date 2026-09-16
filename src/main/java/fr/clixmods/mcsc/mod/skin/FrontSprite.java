/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.skin;

import java.util.List;

/**
 * Projects a 64x64 skin to the 16x32 front view the library shows in its slots, the
 * same way the site draws its thumbnails.
 *
 * <p>It is a copy of eleven rectangles, not a render: the front face of every body
 * part already sits in the skin, so the whole thumbnail is cut-and-paste plus one
 * alpha blend for the outer layer. Nothing here touches the game, which keeps it
 * usable from the thread that decompressed the atlas.
 *
 * <p>The viewer faces the model, so the model's right arm lands on the viewer's left.
 * The parts are laid out as:
 *
 * <pre>
 *      0   4       12  16
 *   0      +-------+          head
 *   8  +---+-------+---+      right arm | body | left arm
 *  20  ....+---+---+....      right leg | left leg
 *  32      +---+---+
 * </pre>
 */
public final class FrontSprite {
    public static final int WIDTH = 16;
    public static final int HEIGHT = 32;
    /** The side of a skin, in pixels; both dimensions, and the only size the API serves. */
    public static final int SKIN_SIZE = 64;

    private static final List<Face> WIDE_FACES = faces(false);
    private static final List<Face> SLIM_FACES = faces(true);

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
        for (Face face : slim ? SLIM_FACES : WIDE_FACES) {
            for (int row = 0; row < face.height(); row++) {
                for (int column = 0; column < face.width(); column++) {
                    int source = argb(skin, face.u() + column, face.v() + row);
                    int index = (top + face.y() + row) * stride + left + face.x() + column;
                    // The outer layer is drawn over the body, and most of it is
                    // transparent, so it is blended rather than copied.
                    target[index] = face.overlay() ? over(source, target[index]) : source;
                }
            }
        }
    }

    /** One pixel of a skin buffer, as ARGB. */
    private static int argb(byte[] skin, int x, int y) {
        int offset = (y * SKIN_SIZE + x) * 4;
        int red = skin[offset] & 0xFF;
        int green = skin[offset + 1] & 0xFF;
        int blue = skin[offset + 2] & 0xFF;
        int alpha = skin[offset + 3] & 0xFF;
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    /** {@code source} over {@code backdrop}, both ARGB and neither premultiplied. */
    private static int over(int source, int backdrop) {
        int sourceAlpha = source >>> 24;
        if (sourceAlpha == 0xFF) {
            return source;
        }
        if (sourceAlpha == 0) {
            return backdrop;
        }

        int backdropAlpha = backdrop >>> 24;
        int alpha = sourceAlpha + backdropAlpha * (0xFF - sourceAlpha) / 0xFF;
        if (alpha == 0) {
            return 0;
        }

        return alpha << 24
                | channel(source, backdrop, sourceAlpha, backdropAlpha, alpha, 16) << 16
                | channel(source, backdrop, sourceAlpha, backdropAlpha, alpha, 8) << 8
                | channel(source, backdrop, sourceAlpha, backdropAlpha, alpha, 0);
    }

    private static int channel(
            int source, int backdrop, int sourceAlpha, int backdropAlpha, int alpha, int shift) {
        int top = (source >> shift & 0xFF) * sourceAlpha;
        int bottom = (backdrop >> shift & 0xFF) * backdropAlpha * (0xFF - sourceAlpha) / 0xFF;
        return Math.min(0xFF, (top + bottom) / alpha);
    }

    /**
     * The rectangles to copy, body first and outer layer second.
     *
     * <p>These are the coordinates of the 1.8 skin layout, which is the only one the
     * API serves. The slim model narrows both arms to three pixels; the right arm is
     * nudged one pixel inwards so it still meets the body.
     */
    private static List<Face> faces(boolean slim) {
        int arm = slim ? 3 : 4;
        int rightArmX = slim ? 1 : 0;

        return List.of(
                new Face(8, 8, 8, 8, 4, 0, false),              // head
                new Face(20, 20, 8, 12, 4, 8, false),           // body
                new Face(44, 20, arm, 12, rightArmX, 8, false), // right arm
                new Face(36, 52, arm, 12, 12, 8, false),        // left arm
                new Face(4, 20, 4, 12, 4, 20, false),           // right leg
                new Face(20, 52, 4, 12, 8, 20, false),          // left leg

                new Face(40, 8, 8, 8, 4, 0, true),              // hat
                new Face(20, 36, 8, 12, 4, 8, true),            // jacket
                new Face(44, 36, arm, 12, rightArmX, 8, true),  // right sleeve
                new Face(52, 52, arm, 12, 12, 8, true),         // left sleeve
                new Face(4, 36, 4, 12, 4, 20, true),            // right trouser leg
                new Face(4, 52, 4, 12, 8, 20, true));           // left trouser leg
    }

    /** A rectangle of the skin, and where it lands in the sprite. */
    private record Face(int u, int v, int width, int height, int x, int y, boolean overlay) {
    }
}
