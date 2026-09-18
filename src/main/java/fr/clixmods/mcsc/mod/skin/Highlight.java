/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.skin;

import fr.clixmods.mcsc.mod.project.Layer;

/**
 * The pulse that says where a layer sits on the figure.
 *
 * <p>Pointing at a row in the layers panel used to put that layer on the model
 * <em>on its own</em>, which answers the wrong question: it shows what the layer is, and
 * hides where it is. Three brown layers still look like three brown layers, and a layer
 * that something else covers — or that is switched off — showed up as the whole figure
 * vanishing.
 *
 * <p>So this does what the site does: the layer's own texels are made to pulse over the
 * composed stack, and everything else stays where it was. You see where the layer lands
 * even when nothing of it is visible, which is exactly when you need to.
 *
 * <p>Two details are worth keeping rather than simplifying, both taken from the site:
 *
 * <ul>
 *   <li><strong>the flash colour is decided texel by texel</strong>, from what lies
 *       underneath — gold over anything dark, near-black over anything light. One colour
 *       for the lot disappears against half the catalogue;
 *   <li><strong>the alpha rises too.</strong> That is what makes a hidden layer visible:
 *       on an empty texel the flash is written at full strength and faded in by its
 *       alpha alone.
 * </ul>
 *
 * <p>The pulse runs on the clock, not on a frame count, so it beats at the same speed
 * whatever the frame rate — and leaving the row fades out rather than snapping off.
 */
public final class Highlight {
    /** One beat, in milliseconds. */
    private static final long PERIOD_MS = 600;

    /** How long the fade takes when the pointer leaves the row. */
    private static final long FADE_MS = 180;

    /** Below this the blend cannot change a single byte, so there is nothing to draw. */
    private static final float DEAD = 0.004F;

    private static final int GOLD_RED = 255;
    private static final int GOLD_GREEN = 214;
    private static final int GOLD_BLUE = 64;
    private static final int INK_RED = 20;
    private static final int INK_GREEN = 20;
    private static final int INK_BLUE = 30;

    /** Under this alpha a texel counts as empty, and so as something to light up. */
    private static final int EMPTY_ALPHA = 128;

    /** Under this luminance the backdrop is dark, and the flash goes gold. */
    private static final int DARK_LUMINANCE = 145;

    private final byte[] blended = new byte[Composite.BYTES];

    private Layer layer;
    private long startedAt;
    private long fadingSince;
    private float fadeFrom;

    /**
     * Starts — or resumes — the pulse on {@code layer}.
     *
     * <p>Coming back to a row that is still fading picks the beat up at the strength it
     * had reached, rather than restarting it from nothing: sliding along the rows would
     * otherwise flash each one from black.
     */
    public void show(Layer layer, long now) {
        if (this.layer == layer && this.fadingSince == 0) {
            return;
        }
        if (this.layer == layer) {
            this.startedAt = now - phaseFor(strength(now));
        } else {
            this.layer = layer;
            this.startedAt = now;
        }
        this.fadingSince = 0;
    }

    /** Leaves the row: fades out rather than cutting. */
    public void hide(long now) {
        if (this.layer == null || this.fadingSince != 0) {
            return;
        }
        this.fadeFrom = strength(now);
        this.fadingSince = now;
    }

    /** Drops the pulse at once, with no fade — for a layer that no longer exists. */
    public void drop() {
        this.layer = null;
        this.fadingSince = 0;
    }

    public Layer layer() {
        return this.layer;
    }

    /**
     * Whether anything is being highlighted right now.
     *
     * <p>Ends the fade itself when it runs out. A deleted layer's row will never report
     * the pointer leaving it, so nothing else would.
     */
    public boolean active(long now) {
        if (this.layer == null) {
            return false;
        }
        if (this.fadingSince != 0 && now - this.fadingSince >= FADE_MS) {
            drop();
            return false;
        }
        return strength(now) > DEAD;
    }

    /**
     * {@code base} with the layer's texels flashed over it.
     *
     * <p>Returns a buffer this object owns and overwrites on the next call: it is
     * uploaded straight to the preview texture and never kept.
     *
     * @return {@code base} itself when there is nothing to add, so a caller can compare
     *         by identity and skip the upload
     */
    public byte[] over(byte[] base, byte[] layerBuffer, long now) {
        if (base == null || base.length < Composite.BYTES || !active(now)) {
            return base;
        }
        return blend(base, layerBuffer, strength(now), this.blended);
    }

    /** How strong the flash is at {@code now}, from 0 to 1. */
    float strength(long now) {
        if (this.layer == null) {
            return 0;
        }
        if (this.fadingSince != 0) {
            float done = (float) (now - this.fadingSince) / FADE_MS;
            return done >= 1 ? 0 : this.fadeFrom * (1 - done);
        }
        long elapsed = Math.max(0, now - this.startedAt);
        double phase = 2 * Math.PI * (elapsed % PERIOD_MS) / PERIOD_MS;
        return (float) ((1 - Math.cos(phase)) / 2);
    }

    /** Where in the beat a given strength is reached, on the way up. */
    private static long phaseFor(float strength) {
        double clamped = Math.max(0, Math.min(1, strength));
        return (long) (Math.acos(1 - 2 * clamped) / (2 * Math.PI) * PERIOD_MS);
    }

    /**
     * One frame of the flash.
     *
     * <p>Separated from the clock so that what it paints can be checked without waiting
     * for anything.
     *
     * @param strength 0 leaves {@code base} alone, 1 is the top of the beat
     * @param out      where to write; may not be {@code base}
     * @return {@code out}, or {@code base} itself when the layer adds nothing — which
     *         includes a layer with no opaque texel at all, so that an element the
     *         catalogue serves empty costs no upload rather than one a frame
     */
    public static byte[] blend(byte[] base, byte[] layerBuffer, float strength, byte[] out) {
        if (layerBuffer == null || layerBuffer.length < Composite.BYTES || strength <= DEAD) {
            return base;
        }
        System.arraycopy(base, 0, out, 0, Composite.BYTES);

        boolean touched = false;
        for (int offset = 0; offset < Composite.BYTES; offset += 4) {
            if ((layerBuffer[offset + 3] & 0xFF) == 0) {
                continue;
            }
            touched = true;
            int red = base[offset] & 0xFF;
            int green = base[offset + 1] & 0xFF;
            int blue = base[offset + 2] & 0xFF;
            int alpha = base[offset + 3] & 0xFF;

            boolean empty = alpha < EMPTY_ALPHA;
            // Rec. 601 luminance, the same weights the site uses.
            boolean onDark = empty
                    || 0.299 * red + 0.587 * green + 0.114 * blue < DARK_LUMINANCE;
            int flashRed = onDark ? GOLD_RED : INK_RED;
            int flashGreen = onDark ? GOLD_GREEN : INK_GREEN;
            int flashBlue = onDark ? GOLD_BLUE : INK_BLUE;

            if (empty) {
                // Nothing underneath to blend with, so the colour goes on whole and the
                // alpha alone does the fading.
                out[offset] = (byte) flashRed;
                out[offset + 1] = (byte) flashGreen;
                out[offset + 2] = (byte) flashBlue;
            } else {
                out[offset] = mix(red, flashRed, strength);
                out[offset + 1] = mix(green, flashGreen, strength);
                out[offset + 2] = mix(blue, flashBlue, strength);
            }
            out[offset + 3] = mix(alpha, 255, strength);
        }
        return touched ? out : base;
    }

    private static byte mix(int from, int to, float amount) {
        int value = Math.round(from + (to - from) * amount);
        return (byte) Math.max(0, Math.min(255, value));
    }
}
