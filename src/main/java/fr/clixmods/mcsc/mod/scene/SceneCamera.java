/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.scene;

/**
 * Where the scene is looked at from: the turn, the tilt, the zoom and the pan.
 *
 * <p>Five numbers and their bounds, and nothing that touches the game — which is the
 * point. A camera made of state a test can read is a camera whose bounds are checked
 * rather than eyeballed, and eyeballing is the one thing this environment cannot do.
 *
 * <p>The gestures are the site's, so that the two editors feel the same in the hand:
 * drag to turn, drag with the right button (or with shift held) to pan, wheel to zoom,
 * and one button that puts all five back. The numbers behind them are not the site's,
 * because the units are not: the site works in skin texels at a camera distance, this
 * works in blocks at a pixels-per-block scale, which is what the game's own
 * picture-in-picture path takes.
 *
 * <p><strong>The pan is in pixels, not in blocks</strong>, and that is deliberate. A pan
 * measured in blocks drifts away from the pointer as the zoom changes — you drag an inch
 * and the figure moves half of one. Kept in pixels and divided by the scale in
 * {@link PosedPlayer}, a drag of ten pixels moves the figure ten pixels at every zoom.
 */
public final class SceneCamera {
    /** Facing you, very slightly from above: what the editor opens on. */
    public static final float YAW_DEFAULT = 0.0F;
    public static final float PITCH_DEFAULT = 0.0F;
    public static final float ZOOM_DEFAULT = 1.0F;

    /**
     * Past this the figure lies down and the tilt reads as a bug rather than a view.
     * The site stops at the same place.
     */
    public static final float PITCH_LIMIT = 1.35F;

    /** Zoom is a multiplier of the size the figure is fitted to, so 1 is "fits". */
    public static final float ZOOM_MIN = 0.35F;
    public static final float ZOOM_MAX = 4.0F;

    /**
     * A pan cannot leave the figure outside the panel: past this it is gone and the
     * only way back is the button, which is a trap rather than a freedom.
     */
    public static final float PAN_LIMIT = 400.0F;

    private static final float TURN_PER_PIXEL = 0.012F;
    private static final float TILT_PER_PIXEL = 0.008F;

    /** One wheel notch, as a factor. The site zooms by 9% a notch; so does this. */
    private static final float ZOOM_PER_NOTCH = 1.09F;

    private float yaw = YAW_DEFAULT;
    private float pitch = PITCH_DEFAULT;
    private float zoom = ZOOM_DEFAULT;
    private float panX;
    private float panY;

    /** Turns the figure and tilts the view, by how far the pointer moved. */
    public void turn(double dragX, double dragY) {
        this.yaw = wrap(this.yaw - (float) dragX * TURN_PER_PIXEL);
        this.pitch = clamp(this.pitch + (float) dragY * TILT_PER_PIXEL,
                -PITCH_LIMIT, PITCH_LIMIT);
    }

    /**
     * Slides the figure inside the panel.
     *
     * <p>Positive {@code dragX} is rightwards on screen, so the figure follows the
     * pointer rather than fleeing it.
     */
    public void pan(double dragX, double dragY) {
        this.panX = clamp(this.panX + (float) dragX, -PAN_LIMIT, PAN_LIMIT);
        this.panY = clamp(this.panY + (float) dragY, -PAN_LIMIT, PAN_LIMIT);
    }

    /** One wheel notch. Positive {@code amount} is a scroll up, which moves closer. */
    public void zoom(double amount) {
        if (amount == 0) {
            return;
        }
        float factor = amount > 0 ? ZOOM_PER_NOTCH : 1 / ZOOM_PER_NOTCH;
        this.zoom = clamp(this.zoom * factor, ZOOM_MIN, ZOOM_MAX);
    }

    /** Everything back where it started — the one control with something behind it. */
    public void recentre() {
        this.yaw = YAW_DEFAULT;
        this.pitch = PITCH_DEFAULT;
        this.zoom = ZOOM_DEFAULT;
        this.panX = 0;
        this.panY = 0;
    }

    /** True while every number is at its default, which is when recentring is a no-op. */
    public boolean isCentred() {
        return this.yaw == YAW_DEFAULT && this.pitch == PITCH_DEFAULT
                && this.zoom == ZOOM_DEFAULT && this.panX == 0 && this.panY == 0;
    }

    public float yaw() {
        return this.yaw;
    }

    public float pitch() {
        return this.pitch;
    }

    public float zoom() {
        return this.zoom;
    }

    /** Rightwards on screen, in pixels. */
    public float panX() {
        return this.panX;
    }

    /** Downwards on screen, in pixels. */
    public float panY() {
        return this.panY;
    }

    /**
     * Keeps the turn in one revolution.
     *
     * <p>Not for looks: the angle goes onto the render state in degrees, and a player
     * who spins the figure for a minute would otherwise hand the game a number big
     * enough for a float to lose whole degrees of it.
     */
    private static float wrap(float angle) {
        float revolution = (float) (Math.PI * 2);
        float wrapped = angle % revolution;
        return wrapped < 0 ? wrapped + revolution : wrapped;
    }

    static float clamp(float value, float low, float high) {
        return value < low ? low : value > high ? high : value;
    }
}
