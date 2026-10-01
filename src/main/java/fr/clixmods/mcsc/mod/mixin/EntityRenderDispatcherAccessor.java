/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
//? if >=1.21.2 && <1.21.6 {
/*package fr.clixmods.mcsc.mod.mixin;

import java.util.Map;

import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// The player renderers, by model, on 1.21.2 to 1.21.5.
//
// Those versions draw a player from a render state, which the editor's figure is, but
// their dispatcher only hands out a renderer for an entity - and the figure has none,
// since the editor opens from the title screen. The renderers are a private map on the
// dispatcher; this reads it, and nothing else. From 1.21.6 the game's picture-in-picture
// path does the lookup itself, from the state.
@Mixin(EntityRenderDispatcher.class)
public interface EntityRenderDispatcherAccessor {
    @Accessor("playerRenderers")
    Map<PlayerSkin.Model, EntityRenderer<? extends Player, ?>> mcskincreator$playerRenderers();
}
*///?}
