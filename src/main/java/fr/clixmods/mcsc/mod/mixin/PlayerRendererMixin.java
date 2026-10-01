/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
//? if <1.20.5 {
/*package fr.clixmods.mcsc.mod.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.clixmods.mcsc.mod.scene.FigureState;
import fr.clixmods.mcsc.mod.scene.ModelFigure;
import fr.clixmods.mcsc.mod.scene.WorldPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Turns and lays down the local player for the editor's in-game view, on the targets
// before render states: 1.20.1 to 1.21.1. PlayerModelMixin bends the limbs; this does
// what the renderer does around them - which way the body faces, whether it lies in a
// bed, glides or swims, and the drop of a crouch - from the same pose, through
// ModelFigure, for the one player WorldPose names.
//
// The body faces where it was when the view opened, not where the camera has gone:
// that is the pose's bodyRot, used in place of the entity's.
//
// setupRotations took one argument fewer before 1.20.5, hence the two versions of this
// class. Both descriptors are spelled out, because the class also has the bridge
// methods javac generates, with the entity typed as its superclass.
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {
    @Inject(method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;"
            + "Lcom/mojang/blaze3d/vertex/PoseStack;FFF)V",
            at = @At("HEAD"), cancellable = true)
    private void mcskincreator$turnForTheEditor(AbstractClientPlayer player, PoseStack stack,
                                                float bob, float bodyYaw, float partialTick, CallbackInfo info) {
        if (WorldPose.applies(player.getUUID())) {
            FigureState state = new FigureState();
            WorldPose.apply(state);
            ModelFigure.rotate(stack, state);
            info.cancel();
        }
    }

    @Inject(method = "getRenderOffset(Lnet/minecraft/client/player/AbstractClientPlayer;F)"
            + "Lnet/minecraft/world/phys/Vec3;",
            at = @At("RETURN"), cancellable = true)
    private void mcskincreator$crouchForTheEditor(AbstractClientPlayer player, float partialTick,
                                                  CallbackInfoReturnable<Vec3> offset) {
        if (WorldPose.applies(player.getUUID())) {
            FigureState state = new FigureState();
            WorldPose.apply(state);
            // The renderer's own drop for a crouch, two pixels - and none for anything
            // else, whether or not the player is crouching for real.
            offset.setReturnValue(state.isCrouching ? new Vec3(0.0, -2.0 / 16.0, 0.0) : Vec3.ZERO);
        }
    }
}
*///?} elif <1.21.2 {
/*package fr.clixmods.mcsc.mod.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.clixmods.mcsc.mod.scene.FigureState;
import fr.clixmods.mcsc.mod.scene.ModelFigure;
import fr.clixmods.mcsc.mod.scene.WorldPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Turns and lays down the local player for the editor's in-game view, on the targets
// before render states: 1.20.1 to 1.21.1. PlayerModelMixin bends the limbs; this does
// what the renderer does around them - which way the body faces, whether it lies in a
// bed, glides or swims, and the drop of a crouch - from the same pose, through
// ModelFigure, for the one player WorldPose names.
//
// The body faces where it was when the view opened, not where the camera has gone:
// that is the pose's bodyRot, used in place of the entity's.
//
// setupRotations took one argument fewer before 1.20.5, hence the two versions of this
// class. Both descriptors are spelled out, because the class also has the bridge
// methods javac generates, with the entity typed as its superclass.
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {
    @Inject(method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;"
            + "Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V",
            at = @At("HEAD"), cancellable = true)
    private void mcskincreator$turnForTheEditor(AbstractClientPlayer player, PoseStack stack,
                                                float bob, float bodyYaw, float partialTick,
                                                float scale, CallbackInfo info) {
        if (WorldPose.applies(player.getUUID())) {
            FigureState state = new FigureState();
            WorldPose.apply(state);
            ModelFigure.rotate(stack, state);
            info.cancel();
        }
    }

    @Inject(method = "getRenderOffset(Lnet/minecraft/client/player/AbstractClientPlayer;F)"
            + "Lnet/minecraft/world/phys/Vec3;",
            at = @At("RETURN"), cancellable = true)
    private void mcskincreator$crouchForTheEditor(AbstractClientPlayer player, float partialTick,
                                                  CallbackInfoReturnable<Vec3> offset) {
        if (WorldPose.applies(player.getUUID())) {
            FigureState state = new FigureState();
            WorldPose.apply(state);
            // The renderer's own drop for a crouch, two pixels - and none for anything
            // else, whether or not the player is crouching for real.
            offset.setReturnValue(state.isCrouching ? new Vec3(0.0, -2.0 / 16.0, 0.0) : Vec3.ZERO);
        }
    }
}
*///?}
