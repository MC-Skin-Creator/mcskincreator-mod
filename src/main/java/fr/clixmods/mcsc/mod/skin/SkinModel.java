/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.skin;

/**
 * Which of the two player models a skin is drawn on: four pixel arms or three.
 *
 * <p>The mod's own, rather than the game's {@code SkinModel}, because the game has
 * spelled this three ways across the supported versions - {@code SkinModel} from
 * 1.21.9, {@code PlayerSkin.Model} before it, and a bare {@code "slim"} string in 1.20.1.
 * The project, its history and its file speak in this one, and {@link GameSkins} turns it
 * into the game's at the few places that hand a skin over.
 */
public enum SkinModel {
    /** Classic: four pixel arms. Mojang's {@code classic}. */
    WIDE,
    /** Alex: three pixel arms. Mojang's {@code slim}. */
    SLIM;

    public boolean slim() {
        return this == SLIM;
    }
}
