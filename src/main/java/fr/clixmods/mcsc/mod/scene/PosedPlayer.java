/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.scene;

import fr.clixmods.mcsc.mod.ui.Canvas;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.PlayerSkin;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Hands the figure to the game to be drawn: a render state, and where to look at it from.
 *
 * <p>The state is built rather than taken off an entity, because there may be no entity:
 * the editor opens from the title screen, where no world is loaded. The in-game view is
 * a different thing entirely — there the game draws the real character itself, because
 * the level renderer will not draw the local player any other way.
 *
 * <p>That this works at all rests on one fact, read out of
 * {@code EntityRenderDispatcher.getRenderer}: an {@code AvatarRenderState} is dispatched
 * <strong>on its skin</strong>, not on an entity type, and the skin's model type is what
 * picks the classic or the slim renderer. So a state carrying {@link PlayerSkin} is drawn
 * by the game's own player renderer, with its overlay layer, its animation and its
 * proportions, and the mod supplies nothing but a skin and a situation.
 *
 * <p>Every other field a plain {@code new AvatarRenderState()} needs, it already has:
 * full-bright light, scale one, standing pose, empty hands, every overlay shown. That
 * was checked against the constructors on both targets rather than assumed, and it is
 * why a fresh state is built every frame instead of one being kept and reset — a kept
 * state is a list of fields to remember to clear, and forgetting one leaves the last
 * pose's crouch on the next pose.
 *
 * <p>The camera numbers are vanilla's own, from
 * {@code InventoryScreen.renderEntityInInventoryFollowsMouse}: the figure's turn goes on
 * the state as {@code bodyRot} in degrees, the tilt into the rotation quaternion, the
 * zoom into the scale and the pan into the translation. Deriving them from vanilla
 * rather than from first principles is what settles the signs — every one of them is a
 * chance to send the figure off the side of the panel.
 */
public final class PosedPlayer {
    /** A player is 1.8 blocks tall and 0.6 wide; the box is what centres the figure. */
    private static final float BOX_HEIGHT = 1.8F;
    private static final float BOX_WIDTH = 0.6F;

    /** Room the figure is fitted into, in blocks: tall enough to leave air over the head. */
    private static final float FIT_HEIGHT = 2.1F;

    private static final float DEGREES = (float) (180 / Math.PI);

    private PosedPlayer() {
    }

    /**
     * Draws a figure built from a skin and a pose.
     *
     * @param skin    what it wears, and so also which of the two player models it is
     * @param seconds how long the animation has been running
     * @param playing false leaves it standing rather than frozen mid-stride
     */
    public static void draw(Canvas canvas, PlayerSkin skin, ScenePose pose, boolean playing,
                            float seconds, SceneCamera camera,
                            int x, int y, int width, int height,
                            int stageX, int stageY, int stageWidth, int stageHeight) {
        AvatarRenderState state = new AvatarRenderState();
        state.skin = skin;
        state.boundingBoxHeight = BOX_HEIGHT;
        state.boundingBoxWidth = BOX_WIDTH;
        pose.apply(state, seconds, playing);
        submit(canvas, state, pose.spread(), camera, x, y, width, height,
                stageX, stageY, stageWidth, stageHeight);
    }

    /**
     * The one call both figures end at: the camera, and the game's picture-in-picture
     * route.
     *
     * @param spread how wide the figure gets, in blocks, so it can be fitted whole
     */
    private static void submit(Canvas canvas, EntityRenderState state, float spread,
                               SceneCamera camera, int x, int y, int width, int height,
                               int stageX, int stageY, int stageWidth, int stageHeight) {
        if (width <= 0 || height <= 0 || stageWidth <= 0 || stageHeight <= 0) {
            return;
        }
        float scale = Math.max(1.0F, Math.min(width / spread, height / FIT_HEIGHT)
                * camera.zoom());

        if (state instanceof LivingEntityRenderState living) {
            // A body at 180 degrees faces you; the game rotates by bodyRot - 180.
            living.bodyRot = 180 + camera.yaw() * DEGREES;
            // The head, in degrees and relative to the body. It keeps facing you as the
            // view tilts, which is what vanilla's portrait does and what makes the tilt
            // read as moving around the figure rather than tipping it over.
            living.yRot = 0;
            // Vanilla leaves a gliding figure's head alone, because the glide already
            // owns its pitch; tilting it as well folds the neck.
            living.xRot = living.hasPose(Pose.FALL_FLYING) ? 0 : -camera.pitch() * DEGREES;
        }

        // Vanilla's base orientation: the picture-in-picture space has y running down, so
        // a model built y-up is turned over to stand on its feet. The tilt is multiplied
        // in after it, and handed over a second time as the camera angle, which is what
        // the lighting follows.
        Quaternionf tilt = new Quaternionf().rotateX(camera.pitch());
        Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI).mul(tilt);

        // The rectangle handed over does two jobs at once in the game's picture-in-picture
        // path: it is the scissor, and its centre is where the figure is put. Only the
        // first of those is wanted from the stage — the figure's own box decides where it
        // stands — so the difference between the two centres is added back here, in
        // blocks, which is pixels over the scale.
        float offsetX = (x + width / 2.0F) - (stageX + stageWidth / 2.0F);
        float offsetY = (y + height / 2.0F) - (stageY + stageHeight / 2.0F);

        // Half the box down puts the feet below the middle, so the figure sits in the
        // rectangle rather than hanging from its top. The pan is in pixels, so it is
        // divided by the scale — which is pixels per block — and a drag of ten pixels
        // moves the figure ten pixels whatever the zoom.
        Vector3f translation = new Vector3f((camera.panX() + offsetX) / scale,
                state.boundingBoxHeight / 2 + (camera.panY() + offsetY) / scale, 0);

        canvas.entity(state, scale, translation, rotation, tilt,
                stageX, stageY, stageWidth, stageHeight);
    }
}
