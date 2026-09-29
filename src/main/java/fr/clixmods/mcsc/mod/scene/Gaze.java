/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.scene;

/**
 * Where the figure is looking, which is wherever the pointer is.
 *
 * <p>Two numbers and the angles they come to, and nothing that touches the game — the
 * same bargain {@link SceneCamera} makes, for the same reason: every line here is a sign
 * or a bound, none of them can be seen in a screenshot, and a head turned the wrong way
 * looks exactly like a head turned the right way until you move the mouse.
 *
 * <p><strong>Only the head moves.</strong> The body is the player's, set by dragging, and
 * a body that also drifted towards the pointer would argue with the drag the whole time
 * it was happening. So the turn below is measured against wherever the body has been left
 * and subtracts it: point at the figure and the head faces you whichever way round the
 * body is, which is the same trick {@link PosedPlayer} already plays with the tilt.
 *
 * <p>The falloff and its reach are vanilla's, off
 * {@code InventoryScreen.renderEntityInInventoryFollowsMouse}, so the figure answers the
 * mouse at the pace the player in the inventory does. Its reach is split between a body
 * that leans and a head that turns; here the body does not lean, so the head carries the
 * pair. Not so for the nod: the other half of vanilla's was the whole model tipping over,
 * which in this editor is the player's tilt and not the neck's to take.
 *
 * @param right how far the pointer is to the right of the figure, in pixels
 * @param below how far the pointer is below the figure's eyes, in pixels
 */
public record Gaze(float right, float below) {
    /** Nothing to look at: the head sits wherever the body is pointing it. */
    public static final Gaze AHEAD = new Gaze(0, 0);

    /** How far a head turns off its body before the neck gives up, in degrees. */
    public static final float NECK_LIMIT = 50.0F;

    /** How far the pointer travels for most of the turn, in pixels. */
    private static final float FALLOFF = 40.0F;

    /** Degrees at the far end of that falloff, sideways and up or down. */
    private static final float TURN_REACH = 40.0F;
    private static final float NOD_REACH = 20.0F;

    /** A player's eyes, in blocks off the ground they are standing on. */
    private static final float EYE_HEIGHT = 1.62F;

    /**
     * Where the pointer is, relative to the eyes of a figure standing on {@code feetY}.
     *
     * @param centreX the figure's centre line on screen
     * @param feetY   where the figure's feet are on screen
     * @param scale   pixels per block, so that the eyes stay put as the zoom changes
     */
    public static Gaze towards(float mouseX, float mouseY, float centreX, float feetY, float scale) {
        return new Gaze(mouseX - centreX, mouseY - (feetY - EYE_HEIGHT * scale));
    }

    /**
     * How far the head turns off the body, in degrees, for a body left at
     * {@code bodyTurn} degrees off facing you.
     *
     * <p>Wrapped before it is bounded, or a player who has spun the figure a few times
     * hands a head that only needs to turn ten degrees a number in the hundreds, and it
     * sticks at the limit for the rest of the revolution.
     */
    public float headTurn(float bodyTurn) {
        // Vanilla measures the pointer from the figure leftwards — atan((centre - mouse)
        // / 40) — because the game counts a body's turn the other way round from the
        // screen's x. The minus is that, kept here so the rest can read in screen terms.
        float aim = -follow(this.right) * TURN_REACH;
        return clamp(wrap(aim - bodyTurn), -NECK_LIMIT, NECK_LIMIT);
    }

    /**
     * How far the head nods, in degrees, positive downwards.
     *
     * <p>No bound of its own: the falloff below never reaches a right angle, so the nod
     * stops around thirty degrees on its own and the neck is never asked for more.
     */
    public float headNod() {
        return follow(this.below) * NOD_REACH;
    }

    /** Quick while the pointer is near the figure, then settling as it goes further. */
    private static float follow(float offset) {
        return (float) Math.atan(offset / FALLOFF);
    }

    /** Into the half-turn either side of straight ahead, which is where a neck lives. */
    private static float wrap(float degrees) {
        float turn = degrees % 360;
        if (turn > 180) {
            return turn - 360;
        }
        return turn < -180 ? turn + 360 : turn;
    }

    private static float clamp(float value, float low, float high) {
        return value < low ? low : value > high ? high : value;
    }
}
