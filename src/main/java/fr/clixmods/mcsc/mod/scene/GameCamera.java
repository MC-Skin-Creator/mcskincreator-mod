/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.scene;

import fr.clixmods.mcsc.mod.MCSkinCreatorClient;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
//? if >=26.1 {
/*import net.minecraft.world.entity.EntityTypes;
*///?} else {
import net.minecraft.world.entity.EntityType;
//?}
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.phys.Vec3;

/**
 * The game's own camera, borrowed for the two views that need the real world.
 *
 * <p>Two of the three cameras do not draw anything: they let the game draw it. For the
 * in-game view the world is already being rendered behind the editor, so all that is
 * missing is a camera that goes round the character instead of sitting in their head;
 * for the first-person view nothing at all is missing — the game draws the arm, which is
 * the whole question being asked. The site had to fit its arm numerically against a
 * screenshot of the game, two pixels out on a twelve-hundred-pixel image; here the arm
 * <em>is</em> the game's.
 *
 * <p><strong>No second mixin.</strong> Everything below is public API with the same
 * signature on both targets, checked with {@code javap}:
 * {@code Minecraft#setCameraEntity}, {@code Minecraft#getCameraEntity},
 * {@code Options#setCameraType}, {@code Options#getCameraType}. The camera the game
 * follows round the character is a {@link Marker} — a nothing of an entity, no size, no
 * eye height, no tick — held by this class alone and never put in the world, so nothing
 * else can see it and nothing has to clean it up but this.
 *
 * <p>What is borrowed is given back. {@link #release()} is idempotent and is called from
 * the screen's own teardown as well as on every change of camera, because a mod that
 * leaves a player in third person after closing a window has broken their game, not
 * their preview.
 */
public final class GameCamera {
    /** How far the camera sits from the character, in blocks, and how far it may go. */
    private static final double DISTANCE_DEFAULT = 2.4;
    private static final double DISTANCE_MIN = 0.8;
    private static final double DISTANCE_MAX = 12.0;
    private static final double DISTANCE_PER_NOTCH = 1.12;

    /** Where on the character the camera aims: the chest, not the feet. */
    private static final double TARGET_HEIGHT = 1.1;

    private static final float DEGREES_PER_PIXEL = 0.35F;
    private static final float PITCH_LIMIT = 89.0F;

    /** Facing the character from slightly above, which is how a portrait is taken. */
    private static final float YAW_DEFAULT = 0.0F;
    private static final float PITCH_DEFAULT = 8.0F;

    private final Minecraft client;

    private CameraType borrowedType;
    private Entity borrowedEntity;
    private boolean holding;

    private Marker orbit;
    private float yaw = YAW_DEFAULT;
    private float pitch = PITCH_DEFAULT;
    private double distance = DISTANCE_DEFAULT;

    public GameCamera(Minecraft client) {
        this.client = client;
    }

    /** Whether there is a world and a character to look at, and so whether to offer them. */
    public boolean available() {
        return this.client.level != null && this.client.player != null;
    }

    /**
     * Borrows the game's camera for {@code mode}, or gives it back for
     * {@link CameraMode#WORKSHOP}.
     *
     * <p>Both borrowing modes put the camera in first person, and the difference between
     * them is only <em>whose</em> first person it is: the character's own eyes for the
     * first-person view, and a marker flying round them for the in-game view. That is
     * also why the arm appears in one and not the other — the game draws the held hand
     * for the local player and for nobody else.
     */
    public void take(CameraMode mode) {
        if (!mode.takesGameCamera() || !available()) {
            release();
            return;
        }

        LocalPlayer player = this.client.player;
        if (!this.holding) {
            this.borrowedType = this.client.options.getCameraType();
            this.borrowedEntity = this.client.getCameraEntity();
            this.holding = true;
        }
        this.client.options.setCameraType(CameraType.FIRST_PERSON);

        if (mode == CameraMode.FIRST_PERSON) {
            this.client.setCameraEntity(player);
            return;
        }

        Marker marker = orbitMarker();
        if (marker == null) {
            // No camera to fly: stay on the character's own eyes rather than leave the
            // view somewhere undefined. The view is wrong, not broken, and it says so
            // in the log once rather than every frame.
            this.client.setCameraEntity(player);
            return;
        }
        this.client.setCameraEntity(marker);
        follow();
    }

    /**
     * Puts the orbiting camera where it belongs.
     *
     * <p>Called every tick, and again on every gesture. Every tick because the character
     * can still move with a window open — pushed by a mob, carried by a boat, or simply
     * falling — and a camera placed once would be left behind; on the gesture so that a
     * drag moves the view now rather than at the next tick. A tick is also the right
     * moment: the world is drawn before the screen is, so a camera moved during the
     * screen's own drawing would be a frame late.
     */
    public void follow() {
        LocalPlayer player = this.client.player;
        if (!this.holding || this.orbit == null || player == null
                || this.client.getCameraEntity() != this.orbit) {
            return;
        }

        Vec3 target = player.position().add(0, TARGET_HEIGHT, 0);
        // The camera looks at the character, so its position is the target walked
        // backwards along its own view vector. Deriving the position from the angles,
        // rather than the angles from a position, is what keeps the character dead
        // centre however far round the camera has gone.
        Vec3 back = viewVector(this.yaw, this.pitch).scale(-this.distance);
        Vec3 eye = target.add(back);

        this.orbit.snapTo(eye.x, eye.y - this.orbit.getEyeHeight(), eye.z, this.yaw, this.pitch);
        // Previous position set to the new one, so nothing is interpolated: the camera
        // is exactly where it was worked out to be, every frame. Leaving it to
        // interpolate would smooth a moving character and, on the first frame, sweep
        // the camera in from the world origin. A character with a window open is
        // standing still anyway — the game has their input.
        this.orbit.setOldPosAndRot();
    }

    /** Drags the camera round the character. The gestures match the workshop camera's. */
    public void turn(double dragX, double dragY) {
        this.yaw -= (float) dragX * DEGREES_PER_PIXEL;
        this.pitch = SceneCamera.clamp(this.pitch + (float) dragY * DEGREES_PER_PIXEL,
                -PITCH_LIMIT, PITCH_LIMIT);
        follow();
    }

    /** One wheel notch. Positive is a scroll up, which moves closer. */
    public void zoom(double amount) {
        if (amount == 0) {
            return;
        }
        double factor = amount > 0 ? 1 / DISTANCE_PER_NOTCH : DISTANCE_PER_NOTCH;
        this.distance = Math.max(DISTANCE_MIN, Math.min(DISTANCE_MAX, this.distance * factor));
        follow();
    }

    public void recentre() {
        this.yaw = YAW_DEFAULT;
        this.pitch = PITCH_DEFAULT;
        this.distance = DISTANCE_DEFAULT;
        follow();
    }

    /**
     * Gives the game its camera back. Safe to call when nothing was borrowed, and safe
     * to call twice.
     */
    public void release() {
        if (!this.holding) {
            return;
        }
        this.holding = false;
        if (this.borrowedType != null) {
            this.client.options.setCameraType(this.borrowedType);
        }
        // Null is the game's own way of saying "the player", so handing null back is
        // handing back exactly what was taken.
        this.client.setCameraEntity(this.borrowedEntity);
        this.borrowedType = null;
        this.borrowedEntity = null;
        this.orbit = null;
    }

    private Marker orbitMarker() {
        if (this.orbit != null && this.orbit.level() == this.client.level) {
            return this.orbit;
        }
        if (this.client.level == null) {
            return null;
        }
        try {
            //? if >=26.1 {
            /*this.orbit = new Marker(EntityTypes.MARKER, this.client.level);
            *///?} else {
            this.orbit = new Marker(EntityType.MARKER, this.client.level);
            //?}
        } catch (RuntimeException cause) {
            MCSkinCreatorClient.LOGGER.warn("No camera to fly round the character", cause);
            this.orbit = null;
        }
        return this.orbit;
    }

    /**
     * The direction an entity looks in, from its two angles in degrees.
     *
     * <p>The game's own formula, which is also the game's own convention: yaw zero looks
     * towards positive z, and a positive pitch looks down.
     */
    private static Vec3 viewVector(float yaw, float pitch) {
        double yawRadians = Math.toRadians(yaw);
        double pitchRadians = Math.toRadians(pitch);
        double flat = Math.cos(pitchRadians);
        return new Vec3(-Math.sin(yawRadians) * flat, -Math.sin(pitchRadians),
                Math.cos(yawRadians) * flat);
    }
}
