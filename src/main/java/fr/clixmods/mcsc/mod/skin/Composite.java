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
 * Several 64x64 skin buffers stacked into one, bottom of the list first.
 *
 * <p>The editor's own preview is composed by the server, which is the arbiter of what
 * a project looks like. This is for the one picture the server cannot reasonably be
 * asked for: the thumbnail of a ready-made stack, of which the catalogue offers two
 * hundred and odd, and asking for two hundred compositions to fill one window is not
 * a thing to do to a service — or to a player waiting on it.
 *
 * <p>The blend is the server's, down to how a half is rounded. A
 * {@code Uint8ClampedArray} — which is what the site composes into, in the browser and
 * on the server alike — rounds a half <strong>to even</strong>, where
 * {@code Math.round} rounds it up. It shows on exactly the pixels where an edge is
 * half covered, which is every outline in the library, so the rule is copied rather
 * than approximated.
 */
public final class Composite {
    /** Bytes of one RGBA skin buffer. */
    public static final int BYTES = FrontSprite.SKIN_SIZE * FrontSprite.SKIN_SIZE * 4;

    private Composite() {
    }

    /**
     * @param buffers the layers, bottom first; a null or short one is skipped rather
     *                than throwing, since a category can still be on its way
     * @return a fresh buffer, transparent where nothing was stacked
     */
    public static byte[] of(List<byte[]> buffers) {
        byte[] out = new byte[BYTES];
        for (byte[] source : buffers) {
            if (source == null || source.length < BYTES) {
                continue;
            }
            for (int offset = 0; offset < BYTES; offset += 4) {
                over(out, offset, source[offset] & 0xFF, source[offset + 1] & 0xFF,
                        source[offset + 2] & 0xFF, (source[offset + 3] & 0xFF) / 255.0);
            }
        }
        return out;
    }

    /** One straight-alpha pixel over what is already at {@code offset}. */
    private static void over(byte[] target, int offset, int red, int green, int blue, double alpha) {
        if (!(alpha > 0)) {
            return;
        }
        if (alpha >= 1) {
            target[offset] = (byte) red;
            target[offset + 1] = (byte) green;
            target[offset + 2] = (byte) blue;
            target[offset + 3] = (byte) 0xFF;
            return;
        }

        double backdrop = (target[offset + 3] & 0xFF) / 255.0;
        double result = alpha + backdrop * (1 - alpha);
        target[offset] = channel((red * alpha + (target[offset] & 0xFF) * backdrop * (1 - alpha)) / result);
        target[offset + 1] = channel((green * alpha + (target[offset + 1] & 0xFF) * backdrop * (1 - alpha)) / result);
        target[offset + 2] = channel((blue * alpha + (target[offset + 2] & 0xFF) * backdrop * (1 - alpha)) / result);
        target[offset + 3] = channel(result * 255);
    }

    /** Writing one channel the way a {@code Uint8ClampedArray} does: half to even. */
    private static byte channel(double value) {
        if (Double.isNaN(value) || value <= 0) {
            return 0;
        }
        if (value >= 255) {
            return (byte) 0xFF;
        }
        int floor = (int) Math.floor(value);
        double rest = value - floor;
        if (rest < 0.5) {
            return (byte) floor;
        }
        if (rest > 0.5) {
            return (byte) (floor + 1);
        }
        return (byte) (floor % 2 == 0 ? floor : floor + 1);
    }
}
