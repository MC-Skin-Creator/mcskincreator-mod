/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.mixin;

import fr.clixmods.mcsc.mod.skin.AppliedSkin;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets {@link AppliedSkin} answer for the player who has just applied a skin.
 *
 * <p>The only mixin in the mod, and it is here because the game offers no other way in.
 * A running client draws a player from the profile it was handed when it joined, and
 * every route to that — {@code PlayerInfo}'s skin supplier, {@code SkinManager}'s
 * caches, the entity's own cached entry — is private on at least one of the supported
 * versions; {@code SkinManager#registerTextures} is package-private on 1.21.11 and
 * private on 26.2, checked with {@code javap}. Clearing the entity's cached
 * {@code PlayerInfo} would achieve nothing either: the profile it would be rebuilt from
 * still carries the old texture property, so the game would go and resolve the old skin
 * again.
 *
 * <p>So the answer is taken at the end rather than the state edited in the middle. That
 * is the smallest possible hold on the game: one public method, one return value, and
 * the mod owns the decision rather than the cache. {@code getSkin} has the same
 * signature on both targets, so there is no version conditional here.
 *
 * <p>{@code @At("RETURN")} rather than {@code HEAD} on purpose. The game resolves the
 * real skin first, and {@link AppliedSkin#worn} keeps its cape and its elytra — the mod
 * replaces the body and nothing else.
 */
@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void mcskincreator$wearAppliedSkin(CallbackInfoReturnable<PlayerSkin> skin) {
        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
        PlayerSkin worn = AppliedSkin.worn(player.getUUID(), skin.getReturnValue());
        if (worn != null) {
            skin.setReturnValue(worn);
        }
    }
}
