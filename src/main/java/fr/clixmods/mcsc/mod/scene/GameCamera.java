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
 * and their body <em>and head</em> are pinned so they keep facing the way they were —
 * otherwise the orbit would be a figure turning with the camera and never showing
 * another side. It is real game state, so it is saved and put back, and in multiplayer
 * other people will see the character turn.
 *
 * <p><strong>The orbit is horizontal only</strong>, and that is the game's doing rather
 * than a corner cut: the camera's pitch and the head's pitch are the same field
 * ({@code Entity.xRot} — the camera reads it through {@code getViewXRot}, the model
 * through the render state). Tilting the view therefore tilts the head, and a head that
 * follows the camera is exactly what this view must not do. So the pitch is held level
 * and a vertical drag does nothing.
 *
 * <p><strong>Hiding the HUD hides the hand.</strong> One flag covers both — 1.21.11
 * checks {@code Options.hideGui} and 26.2 {@code GuiRenderState.isHudHidden}, each
 * guarding the same {@code ItemInHandRenderer} call. So the HUD goes only where the hand
 * is unwanted anyway: third person, and a world backdrop behind the workshop figure. The
 * first-person view keeps the HUD, because the alternative is keeping no arm.
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

    private final Minecraft client;

    private CameraType borrowedType;
    private Entity borrowedEntity;
    private boolean borrowedHud;
    private float borrowedYaw;
    private float borrowedPitch;
    private float borrowedBodyYaw;
    private float borrowedHeadYaw;
    private boolean holding;

    /** Where the body and the head are held while the view goes round them. */
    private float pinnedBodyYaw;

    /** The view currently held, so ticking can keep re-pinning what the game moves. */
    private CameraMode held = CameraMode.WORKSHOP;

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
    public void take(CameraMode mode, boolean worldBackdrop) {
        // A world backdrop behind the workshop figure needs the camera too, and for the
        // opposite reason: there the game must draw the world and *nothing else*, so it
        // goes to first person — which is how the level renderer is told to skip the
        // player — with the HUD off, which takes the hand with it.
        boolean wanted = mode.takesGameCamera() || worldBackdrop;
        if (!wanted || !available()) {
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
            this.borrowedHeadYaw = player.yHeadRot;
            this.pinnedBodyYaw = player.yBodyRot;
            this.holding = true;
        }
        this.held = mode;

        this.client.options.setCameraType(mode == CameraMode.IN_GAME
                ? CameraType.THIRD_PERSON_BACK
                : CameraType.FIRST_PERSON);
        this.client.setCameraEntity(player);
        // Everywhere but first person, where the same flag would take the arm away.
        hideHud(mode != CameraMode.FIRST_PERSON);

        if (mode == CameraMode.IN_GAME) {
            hold();
        }
    }

    /**
     * Re-pins what the game would otherwise move.
     *
     * <p>Called every tick while the in-game view holds. A tick lerps the head towards
     * the view and drags the body after it once the two are far enough apart, so pinning
     * once at the start lasts exactly until the next tick — which is every tick on a
     * server, where the editor does not pause anything.
     */
    public void hold() {
        LocalPlayer player = this.client.player;
        if (!this.holding || player == null || this.held != CameraMode.IN_GAME) {
            return;
        }
        player.setXRot(0);
        player.xRotO = 0;
        player.yBodyRot = this.pinnedBodyYaw;
        player.yBodyRotO = this.pinnedBodyYaw;
        // The head is pinned to the body, not to the view: the camera is what goes
        // round, and the character is meant to stand there and be looked at.
        player.yHeadRot = this.pinnedBodyYaw;
        player.yHeadRotO = this.pinnedBodyYaw;
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
        // Horizontal only. The vertical drag is dropped on purpose: see the note on
        // xRot above — tilting the camera is tilting the head.
        float yaw = player.getYRot() + (float) dragX * DEGREES_PER_PIXEL;
        player.setYRot(yaw);
        player.yRotO = yaw;
        hold();
    }

    /** Faces the character exactly the way they were when the editor opened. */
    public void recentre() {
        LocalPlayer player = this.client.player;
        if (!this.holding || player == null) {
            return;
        }
        player.setYRot(this.borrowedYaw);
        player.yRotO = this.borrowedYaw;
        player.setXRot(this.borrowedPitch);
        player.xRotO = this.borrowedPitch;
        player.setYHeadRot(this.borrowedHeadYaw);
        player.yHeadRotO = this.borrowedHeadYaw;
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
        this.held = CameraMode.WORKSHOP;
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
