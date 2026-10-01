/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.skin;

import net.minecraft.resources.Identifier;

/**
 * What a figure wears: a skin texture, the cape over it if there is one, and the model
 * the texture is drawn on.
 *
 * <p>The mod's own, because the game's own changed shape twice across the supported
 * versions: from 1.21.9 a {@code PlayerSkin} holds client assets, before it a
 * {@code PlayerSkin} holds texture locations, and 1.20.1 has no such object at all - a
 * player there answers its texture and its model name separately. Everything that only
 * needs to know what to draw speaks in this; {@link GameSkins} builds the game's from it
 * at the few places that hand one over.
 *
 * @param texture the skin sheet, as a texture the game has registered
 * @param cape    the cape, or {@code null} for none
 */
public record SkinLook(Identifier texture, Identifier cape, SkinModel model) {
}
