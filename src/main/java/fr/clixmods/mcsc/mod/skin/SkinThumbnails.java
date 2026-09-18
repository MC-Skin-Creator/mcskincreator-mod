/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.skin;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.resources.Identifier;

/**
 * The pictures of the saved skins, one per entry of the library.
 *
 * <p>They come from {@code GET /api/v1/skins/{id}/thumbnail.png}: the server already
 * composed that skin when it stored it, so a front view of it costs a request and no
 * composition at all — which is what makes a list of a dozen skins open without the
 * mod rebuilding a dozen stacks it is not showing.
 *
 * <p>Every texture here is owned by this cache and released by {@link #close()}, which
 * the screen calls when it goes away. An entry deleted from the library is forgotten at
 * once rather than left to sit on a texture nothing will ever draw again.
 */
public final class SkinThumbnails implements AutoCloseable {
    /** The magnification asked of the server: 16 by 32 pixels, twice. */
    public static final int SCALE = 2;

    private final Map<String, ManagedTexture> textures = new LinkedHashMap<>();

    /** Whether this entry has been asked for already, so it is asked for once. */
    public boolean has(String skinId) {
        return this.textures.containsKey(skinId);
    }

    /**
     * Uploads the picture of one entry, replacing the one it had.
     *
     * <p>Must run on the client thread: it uploads a texture.
     */
    public void put(String skinId, byte[] png) throws IOException {
        ManagedTexture texture = this.textures.computeIfAbsent(skinId,
                id -> new ManagedTexture("skin_thumbnail"));
        NativeImage image = NativeImage.read(png);
        try {
            texture.upload(image);
        } catch (RuntimeException | Error failure) {
            image.close();
            throw failure;
        }
    }

    /** The picture of an entry, or null while it has not arrived. */
    public Identifier of(String skinId) {
        ManagedTexture texture = this.textures.get(skinId);
        return texture != null && texture.isUploaded() ? texture.id() : null;
    }

    public int width(String skinId) {
        ManagedTexture texture = this.textures.get(skinId);
        return texture == null ? 0 : texture.width();
    }

    public int height(String skinId) {
        ManagedTexture texture = this.textures.get(skinId);
        return texture == null ? 0 : texture.height();
    }

    public void forget(String skinId) {
        ManagedTexture texture = this.textures.remove(skinId);
        if (texture != null) {
            texture.close();
        }
    }

    @Override
    public void close() {
        this.textures.values().forEach(ManagedTexture::close);
        this.textures.clear();
    }
}
