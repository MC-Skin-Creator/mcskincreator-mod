/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
//? if <1.21.2 {
/*package fr.clixmods.mcsc.mod.scene;

import java.util.List;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import fr.clixmods.mcsc.mod.skin.SkinModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// The figure on the targets that came before render states: 1.20.1 to 1.21.1.
//
// From 1.21.2 the game poses a player from a render state, and the editor hands it one.
// Before, it poses the model from a live entity, inside the renderer, and the editor's
// figure has no entity. So this does by hand the two things the game does from 1.21.2
// on: it bends the player model from a FigureState, the way HumanoidModel#setupAnim
// bends it from an AvatarRenderState, and it turns and lays the body down the way the
// avatar renderer's setupRotations does. Both are read from 1.21.11's own code, so a
// pose looks the same on every target; what the newer renderers do that a pose never
// asks for - held items, spin attacks, death - is left out.
//
// The same two halves pose the real character for the in-world view, through
// PlayerModelMixin and PlayerRendererMixin: there the game draws the entity, and this
// changes what it draws.
public final class ModelFigure {
    // What the game draws a figure in the inventory at: full brightness.
    private static final int FULL_BRIGHT = 15728880;

    private static final float DEGREES = (float) (Math.PI / 180);

    private static PlayerModel<?> wide;
    private static PlayerModel<?> slim;

    private ModelFigure() {
    }

    // Draws the figure in a rectangle of the screen: what GuiGraphics#submitEntityRenderState
    // does from 1.21.6, and the inventory portrait before it, with the same numbers. The
    // rectangle is the scissor, and its centre is where the figure stands.
    public static void draw(GuiGraphics graphics, FigureState state, float scale, Vector3f translation,
                            Quaternionf rotation, int x, int y, int width, int height) {
        PlayerModel<?> model = model(state.skin.model());
        pose(model, state);

        // Everything queued so far goes first, so the depth cleared next cannot hide it.
        // The depth is cleared inside the scissor before the figure, so what is under it
        // does not cut it, and after, so what is drawn over it does: the newer path renders
        // the figure to a texture of its own and gets both for nothing.
        graphics.flush();
        graphics.enableScissor(x, y, x + width, y + height);
        GuiDepth.clear();

        PoseStack stack = graphics.pose();
        stack.pushPose();
        stack.translate(x + width / 2.0F, y + height / 2.0F, 50.0F);
        FigureScale.apply(stack, scale);
        stack.translate(translation.x, translation.y, translation.z);
        stack.mulPose(rotation);
        if (state.isCrouching) {
            // The player renderer's own offset for a crouch: two pixels down.
            stack.translate(0.0F, -2.0F / 16.0F, 0.0F);
        }
        place(stack, state);

        Lighting.setupForEntityInInventory();
        VertexConsumer buffer = graphics.bufferSource()
                .getBuffer(RenderType.entityTranslucent(state.skin.texture()));
        for (ModelPart part : parts(model)) {
            part.render(stack, buffer, FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        }
        graphics.flush();
        Lighting.setupFor3DItems();
        stack.popPose();

        GuiDepth.clear();
        graphics.disableScissor();
    }

    // What the renderer does to a living entity between placing it and drawing its model:
    // the turn and the lie, the flip, the player's scale, and the lift to its feet.
    private static void place(PoseStack stack, FigureState state) {
        if (state.hasPose(Pose.SLEEPING) && state.bedOrientation != null) {
            // The game shifts a sleeper by its eye height less a tenth, towards the head
            // of the bed. A state built for the screen has an eye height of zero.
            float shift = -0.1F;
            stack.translate(-state.bedOrientation.getStepX() * shift, 0.0F,
                    -state.bedOrientation.getStepZ() * shift);
        }
        rotate(stack, state);
        stack.scale(-1.0F, -1.0F, 1.0F);
        stack.scale(0.9375F, 0.9375F, 0.9375F);
        stack.translate(0.0F, -1.501F, 0.0F);
    }

    // The body's turn, and whatever lays it down: a glide, a swim or a bed. The avatar
    // renderer's setupRotations and the living renderer's under it, from the state.
    public static void rotate(PoseStack stack, FigureState state) {
        if (state.hasPose(Pose.SLEEPING)) {
            float facing = state.bedOrientation != null ? bedRotation(state.bedOrientation) : state.bodyRot;
            stack.mulPose(Axis.YP.rotationDegrees(facing));
            stack.mulPose(Axis.ZP.rotationDegrees(90.0F));
            stack.mulPose(Axis.YP.rotationDegrees(270.0F));
            return;
        }
        stack.mulPose(Axis.YP.rotationDegrees(180.0F - state.bodyRot));
        if (state.isFallFlying) {
            float flight = Mth.clamp(state.fallFlyingTimeInTicks * state.fallFlyingTimeInTicks / 100.0F, 0.0F, 1.0F);
            stack.mulPose(Axis.XP.rotationDegrees(flight * (-90.0F - state.xRot)));
        } else if (state.swimAmount > 0.0F) {
            float lean = Mth.lerp(state.swimAmount, 0.0F, state.isInWater ? -90.0F - state.xRot : -90.0F);
            stack.mulPose(Axis.XP.rotationDegrees(lean));
            if (state.isVisuallySwimming) {
                stack.translate(0.0F, -1.0F, 0.3F);
            }
        }
    }

    private static float bedRotation(Direction facing) {
        switch (facing) {
            case SOUTH:
                return 90.0F;
            case NORTH:
                return 270.0F;
            case EAST:
                return 180.0F;
            default:
                return 0.0F;
        }
    }

    // Bends the limbs: HumanoidModel#setupAnim from 1.21.11, for a figure whose hands are
    // empty and which is always a grown player - and then the overlay layer onto what it
    // covers, which from 1.21.2 is a child of each part and before it a part of its own.
    public static void pose(PlayerModel<?> model, FigureState state) {
        for (ModelPart part : List.of(model.head, model.hat, model.body, model.rightArm,
                model.leftArm, model.rightLeg, model.leftLeg)) {
            part.resetPose();
        }

        model.head.xRot = state.xRot * DEGREES;
        model.head.yRot = state.yRot * DEGREES;
        if (state.isFallFlying) {
            model.head.xRot = (float) (-Math.PI / 4);
        } else if (state.swimAmount > 0.0F) {
            model.head.xRot = rotLerpRad(state.swimAmount, model.head.xRot, (float) (-Math.PI / 4));
        }

        float stride = state.walkAnimationPos;
        float speed = state.walkAnimationSpeed;
        model.rightArm.xRot = Mth.cos(stride * 0.6662F + (float) Math.PI) * 2.0F * speed * 0.5F;
        model.leftArm.xRot = Mth.cos(stride * 0.6662F) * 2.0F * speed * 0.5F;
        model.rightLeg.xRot = Mth.cos(stride * 0.6662F) * 1.4F * speed;
        model.leftLeg.xRot = Mth.cos(stride * 0.6662F + (float) Math.PI) * 1.4F * speed;
        model.rightLeg.yRot = 0.005F;
        model.leftLeg.yRot = -0.005F;
        model.rightLeg.zRot = 0.005F;
        model.leftLeg.zRot = -0.005F;
        if (state.isPassenger) {
            model.rightArm.xRot += (float) (-Math.PI / 5);
            model.leftArm.xRot += (float) (-Math.PI / 5);
            model.rightLeg.xRot = -1.4137167F;
            model.rightLeg.yRot = (float) (Math.PI / 10);
            model.rightLeg.zRot = 0.07853982F;
            model.leftLeg.xRot = -1.4137167F;
            model.leftLeg.yRot = (float) (-Math.PI / 10);
            model.leftLeg.zRot = -0.07853982F;
        }

        swing(model, state);

        if (state.isCrouching) {
            model.body.xRot = 0.5F;
            model.rightArm.xRot += 0.4F;
            model.leftArm.xRot += 0.4F;
            model.rightLeg.z += 4.0F;
            model.leftLeg.z += 4.0F;
            model.head.y += 4.2F;
            model.body.y += 3.2F;
            model.leftArm.y += 3.2F;
            model.rightArm.y += 3.2F;
        }

        // The idle sway of the arms, which never stops.
        model.rightArm.zRot += Mth.cos(state.ageInTicks * 0.09F) * 0.05F + 0.05F;
        model.rightArm.xRot += Mth.sin(state.ageInTicks * 0.067F) * 0.05F;
        model.leftArm.zRot -= Mth.cos(state.ageInTicks * 0.09F) * 0.05F + 0.05F;
        model.leftArm.xRot -= Mth.sin(state.ageInTicks * 0.067F) * 0.05F;

        if (state.swimAmount > 0.0F) {
            swim(model, state);
        }

        model.hat.copyFrom(model.head);
        model.jacket.copyFrom(model.body);
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightPants.copyFrom(model.rightLeg);
        model.leftPants.copyFrom(model.leftLeg);
    }

    // The attack swing: the body twists, and the striking arm comes down.
    private static void swing(PlayerModel<?> model, FigureState state) {
        float time = state.attackTime;
        if (time <= 0.0F) {
            return;
        }
        model.body.yRot = Mth.sin(Mth.sqrt(time) * (float) (Math.PI * 2)) * 0.2F;
        if (state.attackArm == HumanoidArm.LEFT) {
            model.body.yRot *= -1.0F;
        }
        model.rightArm.z = Mth.sin(model.body.yRot) * 5.0F;
        model.rightArm.x = -Mth.cos(model.body.yRot) * 5.0F;
        model.leftArm.z = -Mth.sin(model.body.yRot) * 5.0F;
        model.leftArm.x = Mth.cos(model.body.yRot) * 5.0F;
        model.rightArm.yRot += model.body.yRot;
        model.leftArm.yRot += model.body.yRot;
        model.leftArm.xRot += model.body.yRot;

        float eased = 1.0F - (1.0F - time) * (1.0F - time) * (1.0F - time) * (1.0F - time);
        float lift = Mth.sin(eased * (float) Math.PI);
        float follow = Mth.sin(time * (float) Math.PI) * -(model.head.xRot - 0.7F) * 0.75F;
        ModelPart arm = state.attackArm == HumanoidArm.LEFT ? model.leftArm : model.rightArm;
        arm.xRot -= lift * 1.2F + follow;
        arm.yRot += model.body.yRot * 2.0F;
        arm.zRot += Mth.sin(time * (float) Math.PI) * -0.4F;
    }

    // The crawl stroke: the arms go round, the legs flutter.
    private static void swim(PlayerModel<?> model, FigureState state) {
        float amount = state.swimAmount;
        float cycle = state.walkAnimationPos % 26.0F;
        float right = state.attackArm == HumanoidArm.RIGHT && state.attackTime > 0.0F ? 0.0F : amount;
        float left = state.attackArm == HumanoidArm.LEFT && state.attackTime > 0.0F ? 0.0F : amount;
        if (cycle < 14.0F) {
            float reach = 1.8707964F * stroke(cycle) / stroke(14.0F);
            model.leftArm.xRot = rotLerpRad(left, model.leftArm.xRot, 0.0F);
            model.rightArm.xRot = Mth.lerp(right, model.rightArm.xRot, 0.0F);
            model.leftArm.yRot = rotLerpRad(left, model.leftArm.yRot, (float) Math.PI);
            model.rightArm.yRot = Mth.lerp(right, model.rightArm.yRot, (float) Math.PI);
            model.leftArm.zRot = rotLerpRad(left, model.leftArm.zRot, (float) Math.PI + reach);
            model.rightArm.zRot = Mth.lerp(right, model.rightArm.zRot, (float) Math.PI - reach);
        } else if (cycle < 22.0F) {
            float progress = (cycle - 14.0F) / 8.0F;
            model.leftArm.xRot = rotLerpRad(left, model.leftArm.xRot, (float) (Math.PI / 2) * progress);
            model.rightArm.xRot = Mth.lerp(right, model.rightArm.xRot, (float) (Math.PI / 2) * progress);
            model.leftArm.yRot = rotLerpRad(left, model.leftArm.yRot, (float) Math.PI);
            model.rightArm.yRot = Mth.lerp(right, model.rightArm.yRot, (float) Math.PI);
            model.leftArm.zRot = rotLerpRad(left, model.leftArm.zRot, 5.012389F - 1.8707964F * progress);
            model.rightArm.zRot = Mth.lerp(right, model.rightArm.zRot, 1.2707963F + 1.8707964F * progress);
        } else {
            float progress = (cycle - 22.0F) / 4.0F;
            model.leftArm.xRot = rotLerpRad(left, model.leftArm.xRot,
                    (float) (Math.PI / 2) - (float) (Math.PI / 2) * progress);
            model.rightArm.xRot = Mth.lerp(right, model.rightArm.xRot,
                    (float) (Math.PI / 2) - (float) (Math.PI / 2) * progress);
            model.leftArm.yRot = rotLerpRad(left, model.leftArm.yRot, (float) Math.PI);
            model.rightArm.yRot = Mth.lerp(right, model.rightArm.yRot, (float) Math.PI);
            model.leftArm.zRot = rotLerpRad(left, model.leftArm.zRot, (float) Math.PI);
            model.rightArm.zRot = Mth.lerp(right, model.rightArm.zRot, (float) Math.PI);
        }
        model.leftLeg.xRot = Mth.lerp(amount, model.leftLeg.xRot,
                0.3F * Mth.cos(state.walkAnimationPos * 0.33333334F + (float) Math.PI));
        model.rightLeg.xRot = Mth.lerp(amount, model.rightLeg.xRot,
                0.3F * Mth.cos(state.walkAnimationPos * 0.33333334F));
    }

    private static float stroke(float cycle) {
        return -65.0F * cycle + cycle * cycle;
    }

    // An angle eased towards another the short way round, as the model's own helper does.
    private static float rotLerpRad(float delta, float from, float to) {
        float turn = (to - from) % (float) (Math.PI * 2);
        if (turn < (float) -Math.PI) {
            turn += (float) (Math.PI * 2);
        }
        if (turn >= (float) Math.PI) {
            turn -= (float) (Math.PI * 2);
        }
        return from + delta * turn;
    }

    // Every part the figure shows, the overlay layer included; the ears and the cape are
    // drawn by layers of the player renderer, which the figure does without.
    private static List<ModelPart> parts(PlayerModel<?> model) {
        return List.of(model.head, model.hat, model.body, model.jacket,
                model.rightArm, model.rightSleeve, model.leftArm, model.leftSleeve,
                model.rightLeg, model.rightPants, model.leftLeg, model.leftPants);
    }

    // One model per build, baked on first use. The parts are the game's own layer
    // definitions, so a slim figure has the slim arms.
    private static PlayerModel<?> model(SkinModel kind) {
        if (kind == SkinModel.SLIM) {
            if (slim == null) {
                slim = new PlayerModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER_SLIM), true);
            }
            return slim;
        }
        if (wide == null) {
            wide = new PlayerModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
        }
        return wide;
    }
}
*///?}
