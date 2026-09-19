/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.scene;

import java.util.UUID;

import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/**
 * What the character in the world is doing while the in-game view is open.
 *
 * <p>The in-game view shows the real character, drawn by the game inside the world — so
 * the world's light reaches them, and so does whatever shader pack is running. The price
 * of that is that the mod does not own the figure: it cannot hand the renderer a pose the
 * way the workshop view does, because the renderer reads a live entity.
 *
 * <p>So the pose is left here, and the one mixin on the player renderer picks it up on
 * its way out. Nothing is written to the entity, nothing is sent anywhere, and no other
 * player is touched: it is a decision about how <em>this</em> client draws <em>this</em>
 * character for as long as one window is open. The character is not walking; they are
 * being drawn walking.
 *
 * <p>Read on every frame the world renders, for every avatar in it, so the miss — which
 * is every other player, always — is a volatile read and a comparison, the same shape as
 * {@code AppliedSkin.worn}.
 */
public final class WorldPose {
    /** Whose character this applies to. Null when the view is closed: the switch for the lot. */
    private static volatile UUID subject;

    private static volatile ScenePose pose = ScenePose.IDLE;
    private static volatile boolean playing;
    private static volatile float seconds;
    private static volatile float bodyYaw;

    private WorldPose() {
    }

    /**
     * Draws {@code id} in {@code pose} from now until {@link #clear()}.
     *
     * @param bodyYaw which way the character faces, held still while the camera goes
     *                round them
     */
    public static void show(UUID id, ScenePose pose, boolean playing, float seconds,
                            float bodyYaw) {
        WorldPose.pose = pose;
        WorldPose.playing = playing;
        WorldPose.seconds = seconds;
        WorldPose.bodyYaw = bodyYaw;
        // Set last: it is what makes the override live, and the rest must be in place
        // before the next frame reads it.
        WorldPose.subject = id;
    }

    /** Back to whatever the character is really doing. Idempotent. */
    public static void clear() {
        WorldPose.subject = null;
    }

    /** Whether this is the character being posed. */
    public static boolean applies(UUID id) {
        UUID posed = subject;
        return posed != null && posed.equals(id);
    }

    /**
     * Puts the pose on a state the game has just filled in from the entity.
     *
     * <p>Two things happen here, and the second is the reason the mixin exists at all
     * rather than the camera doing the work:
     *
     * <ul>
     *   <li>the animation replaces whatever the character is really doing, which is
     *       standing still;
     *   <li><strong>the head is pinned</strong> — and that cannot be done on the entity,
     *       because the camera's pitch and the head's pitch are the same field there
     *       ({@code Entity.xRot}). On the render state they are two, so the view can be
     *       tilted while the head stays where it is.
     * </ul>
     */
    public static void apply(AvatarRenderState state) {
        ScenePose.reset(state);
        // Facing where they were, not where the camera has gone.
        state.bodyRot = bodyYaw;
        // Both relative to the body, in degrees: zero is a head carried straight.
        state.yRot = 0;
        state.xRot = 0;
        pose.apply(state, seconds, playing);
    }
}
