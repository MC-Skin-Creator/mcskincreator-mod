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

    /** Kept so {@link #refresh} can write into the image the texture already owns. */
    private DynamicTexture texture;

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
        this.texture = new DynamicTexture(() -> this.name, image);
        client.getTextureManager().register(this.id, this.texture);
        this.uploaded = true;
    }

    /**
     * Rewrites the pixels of the texture already there, in place.
     *
     * <p>{@link #upload} registers a texture and throws the old one away, which is the
     * right thing when the picture changes now and then. The layer highlight changes it
     * <em>every frame</em>, and sixty registrations a second is sixty textures allocated
     * and freed a second for a picture of sixteen kilobytes. This writes into the image
     * the texture already owns and re-uploads it.
     *
     * <p>Falls back to a full upload when the size does not match or nothing is there
     * yet, so a caller never has to ask which of the two to call.
     *
     * <p>Must run on the client thread: it touches a texture.
     *
     * @param rgba a buffer of {@code width * height} straight-alpha RGBA pixels
     */
    public void refresh(byte[] rgba, int width, int height) {
        NativeImage pixels = this.texture == null ? null : this.texture.getPixels();
        if (!this.uploaded || pixels == null
                || this.width != width || this.height != height) {
            upload(fromRgba(rgba, width, height));
            return;
        }
        write(pixels, rgba, width, height);
        this.texture.upload();
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

    /**
     * A fresh image holding {@code rgba}.
     *
     * <p>Here rather than beside its callers because both of the mod's two sources of
     * raw pixels — an atlas buffer and a composed sheet — arrive in this shape, and two
     * copies of a channel-order conversion is one copy too many to get right twice.
     */
    public static NativeImage fromRgba(byte[] rgba, int width, int height) {
        NativeImage image = new NativeImage(width, height, false);
        write(image, rgba, width, height);
        return image;
    }

    /** Straight-alpha RGBA in, the game's ARGB out. */
    private static void write(NativeImage image, byte[] rgba, int width, int height) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int offset = (y * width + x) * 4;
                int argb = (rgba[offset + 3] & 0xFF) << 24
                        | (rgba[offset] & 0xFF) << 16
                        | (rgba[offset + 1] & 0xFF) << 8
                        | rgba[offset + 2] & 0xFF;
                image.setPixel(x, y, argb);
            }
        }
    }

    @Override
    public void close() {
        if (this.uploaded) {
            // The texture manager closes what it releases, so this frees the image too.
            Minecraft.getInstance().getTextureManager().release(this.id);
            this.uploaded = false;
            this.texture = null;
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
