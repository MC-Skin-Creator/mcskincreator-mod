/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.scene;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;

/**
 * The game's own camera, borrowed for the two views that look at the real world.
 *
 * <p>Both of them let the game draw, because both ask a question only the game can
 * answer: what this skin looks like on the character standing in the world, and what it
 * looks like on the arm actually held. The site had to fit its first-person arm
 * numerically against a screenshot, two pixels out on a twelve-hundred-pixel image; here
 * the arm <em>is</em> the game's.
 *
 * <p><strong>The camera entity is always the player, and that is not a choice.</strong>
 * {@code LevelRenderer.renderLevel} contains {@code if (entity instanceof LocalPlayer &&
 * camera.entity() != entity) continue;} — your own character is drawn only while they
 * are the camera entity. An earlier version flew a marker around them and got an empty
 * world for it. Third person works because the camera is detached, not because it is
 * somewhere else.
 *
 * <p>Which is also why <strong>orbiting means turning the character</strong>. The camera
 * sits behind wherever they look, so going round them is done by rotating their view,
 * and their body is pinned so they keep facing the way they were — otherwise the orbit
 * would be a figure turning with the camera and never showing another side. It is real
 * game state, so it is saved and put back, and in multiplayer other people will see the
 * character turn.
 *
 * <p><strong>No second mixin.</strong> Everything here is public API, checked with
 * {@code javap} on both jars: {@code Minecraft#setCameraEntity},
 * {@code Options#setCameraType}, {@code Entity#setYRot} and friends, and the one call
 * that differs between the targets — hiding the game's own HUD — is a two-line versioned
 * comment.
 *
 * <p>What is borrowed is given back. {@link #release()} is idempotent and is called from
 * the screen's own teardown as well as on every change of camera, because a mod that
 * leaves a player in third person facing the wrong way has broken their game, not their
 * preview.
 */
public final class GameCamera {
    private static final float DEGREES_PER_PIXEL = 0.35F;
    private static final float PITCH_LIMIT = 89.0F;

    private final Minecraft client;

    private CameraType borrowedType;
    private Entity borrowedEntity;
    private boolean borrowedHud;
    private float borrowedYaw;
    private float borrowedPitch;
    private float borrowedBodyYaw;
    private boolean holding;

    /** Where the body is held while the view goes round it. */
    private float pinnedBodyYaw;

    public GameCamera(Minecraft client) {
        this.client = client;
    }

    /** Whether there is a world and a character to look at, and so whether to offer them. */
    public boolean available() {
        return this.client.level != null && this.client.player != null;
    }

    /**
     * Borrows the game's camera for {@code mode}, or gives it back when {@code mode} has
     * no use for it.
     *
     * <p>The camera entity is set as well as the camera type, because it is not
     * necessarily the player: somebody spectating an entity when they opened the editor
     * would otherwise be shown that entity.
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
            this.borrowedHud = hudHidden();
            this.borrowedYaw = player.getYRot();
            this.borrowedPitch = player.getXRot();
            this.borrowedBodyYaw = player.yBodyRot;
            this.pinnedBodyYaw = player.yBodyRot;
            this.holding = true;
        }

        this.client.options.setCameraType(mode == CameraMode.IN_GAME
                ? CameraType.THIRD_PERSON_BACK
                : CameraType.FIRST_PERSON);
        this.client.setCameraEntity(player);
        // The hotbar, the hearts and the crosshair are drawn over the world whether or
        // not a screen is open, and they land straight on top of the arm.
        hideHud(true);
    }

    /**
     * Goes round the character.
     *
     * <p>Only the in-game view has anything to turn: in first person the arm is fixed to
     * the camera, so turning would sweep the landscape and leave the one thing being
     * looked at exactly where it was.
     */
    public void turn(CameraMode mode, double dragX, double dragY) {
        LocalPlayer player = this.client.player;
        if (!this.holding || player == null || mode != CameraMode.IN_GAME) {
            return;
        }
        float yaw = player.getYRot() + (float) dragX * DEGREES_PER_PIXEL;
        float pitch = SceneCamera.clamp(player.getXRot() + (float) dragY * DEGREES_PER_PIXEL,
                -PITCH_LIMIT, PITCH_LIMIT);
        player.setYRot(yaw);
        player.setXRot(pitch);
        player.setYHeadRot(yaw);
        // Pinned, so the character keeps facing the way they were while the camera goes
        // round them. Without this the body turns with the view and the same side stays
        // towards the camera for ever.
        player.yBodyRot = this.pinnedBodyYaw;
        player.yBodyRotO = this.pinnedBodyYaw;
    }

    /** Faces the character the way they were when the editor opened. */
    public void recentre() {
        LocalPlayer player = this.client.player;
        if (!this.holding || player == null) {
            return;
        }
        player.setYRot(this.borrowedYaw);
        player.setXRot(this.borrowedPitch);
        player.setYHeadRot(this.borrowedYaw);
        player.yBodyRot = this.borrowedBodyYaw;
        player.yBodyRotO = this.borrowedBodyYaw;
    }

    /**
     * Gives the game its camera back, and the character their bearings. Safe to call
     * when nothing was borrowed, and safe to call twice.
     */
    public void release() {
        if (!this.holding) {
            return;
        }
        // Before the flag drops, because recentring is a no-op once it has.
        recentre();
        this.holding = false;
        if (this.borrowedType != null) {
            this.client.options.setCameraType(this.borrowedType);
        }
        // Null is the game's own way of saying "the player", so handing null back is
        // handing back exactly what was taken.
        this.client.setCameraEntity(this.borrowedEntity);
        hideHud(this.borrowedHud);
        this.borrowedType = null;
        this.borrowedEntity = null;
    }

    /**
     * Swings the main hand, so the arm can be seen doing what it does in play.
     *
     * <p>Worth knowing: the swing runs on ticks, and the editor pauses a single-player
     * game, so it plays out on a server and stands still at home. Pausing is the more
     * important of the two — nobody wants to be eaten while choosing a hat.
     */
    public void swing() {
        LocalPlayer player = this.client.player;
        if (player != null) {
            // MAIN_HAND, not a side: the game already knows which arm that is, so a
            // left-handed player's left arm swings.
            player.swing(InteractionHand.MAIN_HAND);
        }
    }

    /**
     * Whether the game is currently drawing its own HUD.
     *
     * <p>The one thing in this class the two targets disagree about: 1.21.11 keeps the
     * flag on {@code Options}, 26.2 moved it into {@code Hud} behind a getter and a
     * toggle. Both are public, so it stays a rename rather than becoming a reason for a
     * mixin.
     */
    private boolean hudHidden() {
        //? if >=26.1 {
        /*return this.client.gui.hud.isHidden();
        *///?} else {
        return this.client.options.hideGui;
        //?}
    }

    private void hideHud(boolean hidden) {
        //? if >=26.1 {
        /*if (this.client.gui.hud.isHidden() != hidden) {
            this.client.gui.hud.toggle();
        }
        *///?} else {
        this.client.options.hideGui = hidden;
        //?}
    }
}
