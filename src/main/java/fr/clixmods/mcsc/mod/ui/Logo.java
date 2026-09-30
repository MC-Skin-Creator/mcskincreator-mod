/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui;

import java.io.InputStream;

import com.mojang.blaze3d.platform.NativeImage;

import fr.clixmods.mcsc.mod.MCSkinCreatorClient;
import fr.clixmods.mcsc.mod.skin.ManagedTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

/**
 * The mod's mark: the icon the mod already ships.
 *
 * <p>There is exactly one MC Skin Creator logo and it is in this repository, at
 * {@code assets/mcskincreator/icon.png}, where {@code fabric.mod.json} and
 * {@code neoforge.mods.toml} point the loader's mod list at it. Drawing a second one
 * for the editor would mean two marks for one product, drifting apart the first time
 * either is touched.
 *
 * <p>It is loaded through the resource manager rather than copied to a second path,
 * so the file stays in one place. The load happens once per session and falls back to
 * nothing: a mark that cannot be read costs the mark, not the screen.
 */
public final class Logo {
    /** Where the icon sits under {@code assets/mcskincreator/}. */
    private static final Identifier SOURCE =
            Identifier.fromNamespaceAndPath(MCSkinCreatorClient.MOD_ID, "icon.png");

    private static ManagedTexture texture;
    private static boolean attempted;

    private Logo() {
    }

    /**
     * The mark's texture, or null when it could not be read.
     *
     * <p>Must run on the client thread: it may upload a texture.
     */
    public static Identifier texture(Minecraft client) {
        if (!attempted) {
            attempted = true;
            load(client);
        }
        return texture == null ? null : texture.id();
    }

    /**
     * The icon's own size in pixels.
     *
     * <p>The file is far larger than the mark is drawn, so whatever draws it has to
     * sample the whole image rather than a corner of it.
     */
    public static int size() {
        return texture == null ? 0 : texture.width();
    }

    private static void load(Minecraft client) {
        try {
            Resource resource = client.getResourceManager().getResourceOrThrow(SOURCE);
            try (InputStream stream = resource.open()) {
                ManagedTexture uploaded = new ManagedTexture("logo");
                uploaded.upload(NativeImage.read(stream));
                texture = uploaded;
            }
        } catch (Exception failure) {
            // The screen works without a mark; the log says why there is not one.
            MCSkinCreatorClient.LOGGER.warn("Could not read the mod icon at {}", SOURCE, failure);
        }
    }
}
