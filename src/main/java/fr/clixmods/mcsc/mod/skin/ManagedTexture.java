/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.skin;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import com.mojang.blaze3d.platform.NativeImage;
import fr.clixmods.mcsc.mod.MCSkinCreatorClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * A texture the mod builds at runtime and hands to the game's texture manager.
 *
 * <p>Its whole point is {@link #close()}. A registered texture lives in the texture
 * manager until something releases it, and the manager closes what it releases, so
 * releasing here is the one and only step: every texture the mod uploads is owned by
 * a {@code ManagedTexture}, and every screen that makes one closes it when it goes
 * away.
 *
 * <p>Names are unique per instance rather than per category or per player: reusing a
 * name across two live textures would have the second registration release the first
 * one from under whoever still draws it.
 */
public final class ManagedTexture implements AutoCloseable {
    private static final AtomicInteger NAMES = new AtomicInteger();

    private final Identifier id;
    private final String name;

    private boolean uploaded;
    private int width;
    private int height;

    /** @param purpose a word naming what the texture is for, for logs and crash reports */
    public ManagedTexture(String purpose) {
        this.name = sanitize(purpose) + "_" + NAMES.incrementAndGet();
        this.id = Identifier.fromNamespaceAndPath(MCSkinCreatorClient.MOD_ID, "runtime/" + this.name);
    }

    /**
     * Uploads {@code image} under this texture's name, replacing whatever was there.
     *
     * <p>Takes ownership of {@code image}: the game's {@code DynamicTexture} closes it,
     * and so callers must not keep using it afterwards. Must run on the client thread,
     * which is the only one allowed to touch the texture manager.
     */
    public void upload(NativeImage image) {
        this.width = image.getWidth();
        this.height = image.getHeight();

        Minecraft client = Minecraft.getInstance();
        // Releasing first closes the previous image instead of leaking it; registering
        // over a live name would drop it on the floor.
        client.getTextureManager().release(this.id);
        client.getTextureManager().register(this.id, new DynamicTexture(() -> this.name, image));
        this.uploaded = true;
    }

    /** Whether anything has been uploaded yet, and so whether {@link #id()} can be drawn. */
    public boolean isUploaded() {
        return this.uploaded;
    }

    public Identifier id() {
        return this.id;
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }

    @Override
    public void close() {
        if (this.uploaded) {
            // The texture manager closes what it releases, so this frees the image too.
            Minecraft.getInstance().getTextureManager().release(this.id);
            this.uploaded = false;
        }
    }

    /** Keeps the name inside what an {@link Identifier} path accepts. */
    private static String sanitize(String purpose) {
        StringBuilder sanitized = new StringBuilder(purpose.length());
        for (char character : purpose.toLowerCase(Locale.ROOT).toCharArray()) {
            sanitized.append(character >= 'a' && character <= 'z' || character >= '0' && character <= '9'
                    ? character
                    : '_');
        }
        return sanitized.isEmpty() ? "texture" : sanitized.toString();
    }
}
