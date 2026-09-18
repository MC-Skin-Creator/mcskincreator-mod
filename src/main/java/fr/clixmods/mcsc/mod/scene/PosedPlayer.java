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
import net.minecraft.world.entity.player.PlayerSkin;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Hands the figure to the game to be drawn: a render state, and where to look at it from.
 *
 * <p>The state is built rather than taken from an entity, because there may be no entity
 * — the editor opens from the title screen, where no world is loaded. That it works at
 * all rests on one fact, read out of {@code EntityRenderDispatcher.getRenderer}: an
 * {@code AvatarRenderState} is dispatched <strong>on its skin</strong>, not on an entity
 * type, and the skin's model type is what picks the classic or the slim renderer. So a
 * state carrying {@link PlayerSkin} is drawn by the game's own player renderer, with its
 * overlay layer, its animation and its proportions, and the mod supplies nothing but a
 * skin and a situation.
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
     * Draws the figure in a rectangle.
     *
     * @param skin    what it wears, and so also which of the two player models it is
     * @param seconds how long the animation has been running
     * @param playing false leaves it standing rather than frozen mid-stride
     */
    public static void draw(Canvas canvas, PlayerSkin skin, ScenePose pose, boolean playing,
                            float seconds, SceneCamera camera,
                            int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }

        float scale = scale(pose, camera, width, height);
        AvatarRenderState state = state(skin, pose, playing, seconds, camera);

        // Vanilla's base orientation: the picture-in-picture space has y running down, so
        // a model built y-up is turned over to stand on its feet. The tilt is multiplied
        // in after it, and handed over a second time as the camera angle, which is what
        // the lighting follows.
        Quaternionf tilt = new Quaternionf().rotateX(camera.pitch());
        Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI).mul(tilt);

        // Half the box down puts the feet below the middle, so the figure sits in the
        // rectangle rather than hanging from its top. The pan is in pixels, so it is
        // divided by the scale — which is pixels per block — and a drag of ten pixels
        // moves the figure ten pixels whatever the zoom.
        Vector3f translation = new Vector3f(camera.panX() / scale,
                BOX_HEIGHT / 2 + camera.panY() / scale, 0);

        canvas.entity(state, scale, translation, rotation, tilt, x, y, width, height);
    }

    /**
     * Pixels per block: the size that fits the figure, times the zoom.
     *
     * <p>Whichever of width and height runs out first decides, so the figure keeps its
     * proportions at any window size and any of the four GUI scales — and a pose that
     * lies down is fitted to its length rather than to its height, which is the
     * difference between a sleeper and a sleeper with no feet.
     */
    private static float scale(ScenePose pose, SceneCamera camera, int width, int height) {
        float fit = Math.min(width / pose.spread(), height / FIT_HEIGHT);
        return Math.max(1.0F, fit * camera.zoom());
    }

    private static AvatarRenderState state(PlayerSkin skin, ScenePose pose, boolean playing,
                                           float seconds, SceneCamera camera) {
        AvatarRenderState state = new AvatarRenderState();
        state.skin = skin;
        state.boundingBoxHeight = BOX_HEIGHT;
        state.boundingBoxWidth = BOX_WIDTH;

        // A body at 180 degrees faces you; the game rotates by bodyRot - 180.
        state.bodyRot = 180 + camera.yaw() * DEGREES;
        // The head, in degrees and relative to the body. It keeps facing you as the view
        // tilts, which is what vanilla's portrait does and what makes the tilt read as
        // moving around the figure rather than tipping it over.
        state.yRot = 0;
        state.xRot = -camera.pitch() * DEGREES;

        pose.apply(state, seconds, playing);
        return state;
    }
}
