/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.skin;

import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;

/**
 * A texture the mod registered itself, handed to the game where it expects one of its
 * own assets.
 *
 * <p>The game's own implementations derive the texture path from the asset id; a
 * runtime texture is registered under its name directly, so the two are the same here.
 */
public record RuntimeTexture(Identifier id) implements ClientAsset.Texture {
    @Override
    public Identifier texturePath() {
        return this.id;
    }
}
