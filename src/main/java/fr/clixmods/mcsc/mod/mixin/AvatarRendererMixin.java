/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.mixin;

import fr.clixmods.mcsc.mod.scene.WorldPose;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Poses the character the editor is looking at, in the world, on this client only.
 *
 * <p>The second mixin, and the bar was the same as the first: there is no other way in.
 * The in-game view shows the real character <em>inside</em> the world — which is the
 * whole point of it, because that is what puts them under the world's light and under
 * whatever shader pack is running — and a character in the world is drawn from a live
 * entity, not from anything the mod can hand over. The workshop view builds its own
 * render state and needs none of this; that is the difference between the two views.
 *
 * <p>The alternative was to animate the entity itself, and it is worse in every
 * direction: it is real game state, it is sent to the server, other players see it, and
 * a crash between setting it and putting it back leaves somebody permanently crouched.
 * Taking the render state on its way out changes nothing but a picture, on one client,
 * for as long as one window is open.
 *
 * <p>It also buys something the camera could not: <strong>a head that stays still.</strong>
 * On the entity, the camera's pitch and the head's pitch are one field
 * ({@code Entity.xRot}), so tilting the view tilts the head. On the render state they are
 * two, so the view can go up and down while the character keeps looking where they were.
 *
 * <p>Three things about the target, checked rather than assumed:
 *
 * <ul>
 *   <li>{@code extractRenderState} is public with the same signature on both targets, so
 *       this file carries no Stonecutter directive — the repository's own test of whether
 *       a mixin is the right answer;
 *   <li>there are <strong>three</strong> overloads of that name on the class, so the
 *       descriptor is spelled out in full. Without it Mixin has nothing to choose by;
 *   <li>{@code @At("RETURN")}, like the mod's other mixin: the game works out what the
 *       character is doing, and then the mod says what to draw instead. Nothing is
 *       intercepted half-done.
 * </ul>
 */
@Mixin(net.minecraft.client.renderer.entity.player.AvatarRenderer.class)
public abstract class AvatarRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;"
            + "Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("RETURN"))
    private void mcskincreator$poseForTheEditor(Avatar avatar, AvatarRenderState state,
                                                float partialTick, CallbackInfo info) {
        if (WorldPose.applies(avatar.getUUID())) {
            WorldPose.apply(state);
        }
    }
}
