/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.scene;

import fr.clixmods.mcsc.mod.ui.Canvas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
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
 * <p>Two figures, one camera. {@link #draw} builds a state from nothing — a skin and a
 * situation — for the workshop view, which has to work on the title screen where there
 * is no entity at all. {@link #drawLive} takes the state off the player who is actually
 * standing in the world, so the in-game view shows their armour, their held item and
 * their cape. Both then go through the same camera and the same call.
 *
 * <p>That the built one works at all rests on one fact, read out of
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

    /** What vanilla lights a figure in a menu with: everything. */
    private static final int FULL_BRIGHT = 15728880;

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
                            int x, int y, int width, int height) {
        AvatarRenderState state = new AvatarRenderState();
        state.skin = skin;
        state.boundingBoxHeight = BOX_HEIGHT;
        state.boundingBoxWidth = BOX_WIDTH;
        pose.apply(state, seconds, playing);
        submit(canvas, state, pose.spread(), camera, x, y, width, height);
    }

    /**
     * Draws the character who is actually standing in the world.
     *
     * <p>The state is the game's, extracted exactly as vanilla's inventory portrait
     * extracts it, so what appears is the real thing: armour, held item, cape, and
     * whatever the character happens to be doing. The skin comes off the entity, which
     * means it comes through the mod's own override — so what is being edited is what is
     * worn, with nothing extra to wire up.
     *
     * <p>This is also why the in-game view does not go through the game's camera. The
     * level renderer refuses to draw the local player unless they are the camera entity,
     * so a camera flown around them shows an empty world; compositing the figure over
     * that world is what gets it on screen, and it gains a turn, a zoom and a pan the
     * game's own third person has never had.
     */
    public static void drawLive(Canvas canvas, LocalPlayer player, SceneCamera camera,
                                int x, int y, int width, int height) {
        EntityRenderState state = extract(player);
        if (state == null) {
            return;
        }
        submit(canvas, state, ScenePose.IDLE.spread(), camera, x, y, width, height);
    }

    /**
     * The player's render state, lit for a menu rather than for the world.
     *
     * <p>Vanilla's own three adjustments come with it: full brightness, no shadow and no
     * outline. A figure drawn in a panel with the light of the cave it is standing in is
     * a black rectangle.
     */
    private static EntityRenderState extract(LocalPlayer player) {
        EntityRenderer<? super LocalPlayer, ?> renderer =
                Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
        if (renderer == null) {
            return null;
        }
        EntityRenderState state = renderer.createRenderState(player, 1.0F);
        state.lightCoords = FULL_BRIGHT;
        state.shadowPieces.clear();
        state.outlineColor = 0;
        // The name tag belongs over a head in the world, not in a panel eight inches
        // from the eye.
        state.nameTag = null;
        if (state instanceof LivingEntityRenderState living) {
            // Vanilla divides the box out by the scale and then renders at scale one, so
            // that the figure is centred on its own size rather than on a scaled one.
            living.boundingBoxWidth /= living.scale;
            living.boundingBoxHeight /= living.scale;
            living.scale = 1.0F;
        }
        return state;
    }

    /**
     * The one call both figures end at: the camera, and the game's picture-in-picture
     * route.
     *
     * @param spread how wide the figure gets, in blocks, so it can be fitted whole
     */
    private static void submit(Canvas canvas, EntityRenderState state, float spread,
                               SceneCamera camera, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
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

        // Half the box down puts the feet below the middle, so the figure sits in the
        // rectangle rather than hanging from its top. The pan is in pixels, so it is
        // divided by the scale — which is pixels per block — and a drag of ten pixels
        // moves the figure ten pixels whatever the zoom.
        Vector3f translation = new Vector3f(camera.panX() / scale,
                state.boundingBoxHeight / 2 + camera.panY() / scale, 0);

        canvas.entity(state, scale, translation, rotation, tilt, x, y, width, height);
    }
}
