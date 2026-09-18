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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionHand;

/**
 * The game's own camera, borrowed for the first-person view and given straight back.
 *
 * <p>One camera needs this, and only one. The first-person view asks a question only the
 * game can answer — what does this skin look like on the arm I actually hold — so the
 * game is put in first person and draws the arm itself. The site had to fit its arm
 * numerically against a screenshot of the game, two pixels out on a twelve-hundred-pixel
 * image; here the arm <em>is</em> the game's.
 *
 * <p>The in-game view does <strong>not</strong> come through here, and the reason is a
 * rule in {@code LevelRenderer.renderLevel}: {@code if (entity instanceof LocalPlayer &&
 * camera.entity() != entity) continue;}. The local player is drawn only while they are
 * the camera entity, so a camera flown around them shows an empty world — which is
 * precisely what the first version of this did. See {@link CameraMode#IN_GAME} for what
 * replaced it.
 *
 * <p><strong>No second mixin.</strong> Everything below is public API with the same
 * signature on both targets, checked with {@code javap}:
 * {@code Minecraft#setCameraEntity}, {@code Minecraft#getCameraEntity},
 * {@code Options#setCameraType}, {@code Options#getCameraType}.
 *
 * <p>What is borrowed is given back. {@link #release()} is idempotent and is called from
 * the screen's own teardown as well as on every change of camera, because a mod that
 * leaves a player in third person after closing a window has broken their game, not
 * their preview.
 */
public final class GameCamera {
    private final Minecraft client;

    private CameraType borrowedType;
    private Entity borrowedEntity;
    private boolean holding;

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
     * necessarily the player: somebody who was spectating an entity when they opened the
     * editor would otherwise get that entity's arm.
     */
    public void take(CameraMode mode) {
        if (!mode.takesGameCamera() || !available()) {
            release();
            return;
        }
        if (!this.holding) {
            this.borrowedType = this.client.options.getCameraType();
            this.borrowedEntity = this.client.getCameraEntity();
            this.holding = true;
        }
        this.client.options.setCameraType(CameraType.FIRST_PERSON);
        this.client.setCameraEntity(this.client.player);
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
}
