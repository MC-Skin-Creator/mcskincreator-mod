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
//? if >=26.3 {
/*import net.minecraft.world.item.component.SwingAnimation;
*///?}

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
 * <p>The entity's own yaw and pitch are the only things written here, because they are
 * the only things the camera reads. How the character <em>looks</em> — head, body, pose —
 * is decided on the render state instead, by {@code AvatarRendererMixin}. That split is
 * what lets the view tilt without the head tilting with it: on the entity the camera's
 * pitch and the head's pitch are one field, and on the render state they are two.
 *
 * <p><strong>The game's own way of hiding the HUD hides the hand.</strong> One flag
 * covers both — 1.21.11 checks {@code Options.hideGui} and 26.2
 * {@code GuiRenderState.isHudHidden}, each guarding the same {@code ItemInHandRenderer}
 * call. That is exactly what a world backdrop wants and exactly what first person must
 * not have, so the flag is kept for the backdrop and the two views go through
 * {@link HiddenHud} and {@code GuiMixin}, which skip the drawing without touching the
 * setting the player owns.
 *
 * <p>Everything else here is public API, checked with {@code javap} on both jars:
 * {@code Minecraft#setCameraEntity}, {@code Options#setCameraType},
 * {@code Entity#setYRot} and friends, and the one call that differs between the targets
 * — reading and writing the game's HUD flag — is a two-line versioned comment.
 *
 * <p>What is borrowed is given back. {@link #release()} is idempotent and is called from
 * the screen's own teardown as well as on every change of camera, because a mod that
 * leaves a player in third person facing the wrong way has broken their game, not their
 * preview.
 */
public final class GameCamera {
    private static final float DEGREES_PER_PIXEL = 0.35F;

    /** Short of straight up and down, where the camera flips over the character. */
    private static final float PITCH_LIMIT = 85.0F;

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

    /**
     * @param client the running game, or {@code null} where there is none — the preview
     *               tool lays this panel out with no game at all, and a camera with
     *               nothing to borrow simply reports itself unavailable
     */
    public GameCamera(Minecraft client) {
        this.client = client;
    }

    /** Whether there is a world and a character to look at, and so whether to offer them. */
    public boolean available() {
        return this.client != null && this.client.level != null && this.client.player != null;
    }

    /** The character, or null when there is no game to have one. */
    private LocalPlayer player() {
        return this.client == null ? null : this.client.player;
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

        LocalPlayer player = player();
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

        // Two ways to take the HUD away, and they are not interchangeable. The game's
        // flag takes the held hand with it, so it is right for a backdrop — the world
        // and nothing else — and wrong for first person, where the hand is the picture.
        // The views that keep their hand go through the mixin instead.
        boolean backdropOnly = !mode.takesGameCamera();
        hideHud(backdropOnly);
        HiddenHud.hide(!backdropOnly);

        // Anything but the in-game view looks out of the character's own eyes, and the
        // in-game view is steered by turning them: coming back to first person after an
        // orbit would otherwise start it staring at the sky, where the hand is off the
        // bottom of the window and the view looks broken. So the eyes go back to where
        // they were borrowed from.
        if (mode != CameraMode.IN_GAME) {
            recentre();
        }
    }

    /** Which way the character is held facing, for whoever draws them. */
    public float bodyYaw() {
        return this.pinnedBodyYaw;
    }

    /**
     * Tells the world renderer what the real character should be doing.
     *
     * <p>Pushed every frame rather than on every change, because the animation clock
     * moves every frame anyway. The world is drawn before the editor is, so what the
     * renderer reads is one frame old — sixteen milliseconds of a walk cycle, which is
     * not a thing anybody can see.
     *
     * <p>It lives here rather than in the panel because this is the only class that
     * holds the running game, and the panel has to be able to lay itself out without
     * one.
     */
    public void poseInWorld(ScenePose pose, boolean playing, float seconds) {
        LocalPlayer player = player();
        if (this.held != CameraMode.IN_GAME || player == null) {
            WorldPose.clear();
            return;
        }
        WorldPose.show(player.getUUID(), pose, playing, seconds, this.pinnedBodyYaw);
    }

    /**
     * Goes round the character.
     *
     * <p>Only the in-game view has anything to turn: in first person the arm is fixed to
     * the camera, so turning would sweep the landscape and leave the one thing being
     * looked at exactly where it was.
     */
    public void turn(CameraMode mode, double dragX, double dragY) {
        LocalPlayer player = player();
        if (!this.holding || player == null || mode != CameraMode.IN_GAME) {
            return;
        }
        float yaw = player.getYRot() + (float) dragX * DEGREES_PER_PIXEL;
        float pitch = SceneCamera.clamp(player.getXRot() + (float) dragY * DEGREES_PER_PIXEL,
                -PITCH_LIMIT, PITCH_LIMIT);
        player.setYRot(yaw);
        player.yRotO = yaw;
        player.setXRot(pitch);
        player.xRotO = pitch;
    }

    /** Faces the character exactly the way they were when the editor opened. */
    public void recentre() {
        LocalPlayer player = player();
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
        WorldPose.clear();
        HiddenHud.hide(false);
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
        HiddenHud.hide(false);
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
        LocalPlayer player = player();
        if (player != null) {
            // MAIN_HAND, not a side: the game already knows which arm that is, so a
            // left-handed player's left arm swings.
            //? if >=26.3 {
            /*player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
            *///?} else {
            player.swing(InteractionHand.MAIN_HAND);
            //?}
        }
    }

    /**
     * Whether the game is currently drawing its own HUD.
     *
     * <p>Where the targets disagree about the HUD: up to 26.1 the flag lives on
     * {@code Options}, 26.2 moved it into {@code Hud} behind a getter and a toggle. Both are public, so it stays a rename rather than becoming a reason for a
     * mixin.
     */
    private boolean hudHidden() {
        //? if >=26.2 {
        /*return this.client.gui.hud.isHidden();
        *///?} else {
        return this.client.options.hideGui;
        //?}
    }

    private void hideHud(boolean hidden) {
        //? if >=26.2 {
        /*if (this.client.gui.hud.isHidden() != hidden) {
            this.client.gui.hud.toggle();
        }
        *///?} else {
        this.client.options.hideGui = hidden;
        //?}
    }
}
