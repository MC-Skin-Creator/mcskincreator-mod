/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
//? if <1.21.2 {
/*package fr.clixmods.mcsc.mod.mixin;

import fr.clixmods.mcsc.mod.scene.FigureState;
import fr.clixmods.mcsc.mod.scene.ModelFigure;
import fr.clixmods.mcsc.mod.scene.WorldPose;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Bends the local player's limbs for the editor's in-game view, on the targets before
// render states: 1.20.1 to 1.21.1.
//
// From 1.21.9 AvatarRendererMixin does this by posing the render state the model is
// bent from. Before render states the model is bent from the entity itself, so the
// pose is put on the model once the game has bent it, and only for the one player
// WorldPose names - every other player, and this one whenever the view is closed, is
// left as the game drew them. ModelFigure is the same bending the editor's own figure
// gets, so both views show one pose.
//
// The descriptor is spelled out because the class also has the bridge method javac
// generates for the generic one, with Entity in place of LivingEntity.
@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void mcskincreator$poseForTheEditor(LivingEntity entity, float limbSwing, float limbSwingAmount,
                                                float ageInTicks, float headYaw, float headPitch,
                                                CallbackInfo info) {
        if (WorldPose.applies(entity.getUUID())) {
            FigureState state = new FigureState();
            WorldPose.apply(state);
            ModelFigure.pose((PlayerModel<?>) (Object) this, state);
        }
    }
}
*///?}
