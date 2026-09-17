/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.skin;

import java.io.IOException;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

/**
 * The skin shown on the model in the editor: one runtime texture, and the
 * {@link PlayerSkin} the game's player widget reads it through.
 *
 * <p>Going through {@code PlayerSkin} is what lets the preview be vanilla's own
 * player rendering rather than a model assembled by the mod. The widget reads exactly
 * two things from it, the body texture and the model type, which is why the cape and
 * the elytra are left empty.
 */
public final class PreviewSkin implements AutoCloseable {
    private final ManagedTexture texture = new ManagedTexture("preview");

    private PlayerModelType model = PlayerModelType.WIDE;

    /** Which of the two player models the preview is drawn on. */
    public PlayerModelType model() {
        return this.model;
    }

    public void model(PlayerModelType model) {
        this.model = model;
    }

    public boolean isSlim() {
        return this.model == PlayerModelType.SLIM;
    }

    /**
     * Replaces the previewed pixels.
     *
     * <p>Accepts either of the two shapes the API answers with: a PNG, or a raw RGBA
     * buffer of the kind the atlases carry. Telling them apart on the payload rather
     * than on a header keeps the two paths - a composed texture from
     * {@code POST /textures} and an element's own buffer from an atlas - on the same
     * code.
     *
     * <p>Must run on the client thread: it uploads a texture.
     */
    public void show(byte[] payload) throws IOException {
        NativeImage image = decode(payload);
        try {
            this.texture.upload(image);
        } catch (RuntimeException | Error failure) {
            image.close();
            throw failure;
        }
    }

    /** Reads either shape the API answers with: a PNG, or a raw RGBA buffer. */
    public static NativeImage decode(byte[] payload) throws IOException {
        if (payload.length == FrontSprite.SKIN_SIZE * FrontSprite.SKIN_SIZE * 4) {
            return fromRgba(payload);
        }
        return NativeImage.read(payload);
    }

    private static NativeImage fromRgba(byte[] rgba) {
        NativeImage image = new NativeImage(FrontSprite.SKIN_SIZE, FrontSprite.SKIN_SIZE, false);
        for (int y = 0; y < FrontSprite.SKIN_SIZE; y++) {
            for (int x = 0; x < FrontSprite.SKIN_SIZE; x++) {
                int offset = (y * FrontSprite.SKIN_SIZE + x) * 4;
                int argb = (rgba[offset + 3] & 0xFF) << 24
                        | (rgba[offset] & 0xFF) << 16
                        | (rgba[offset + 1] & 0xFF) << 8
                        | rgba[offset + 2] & 0xFF;
                image.setPixel(x, y, argb);
            }
        }
        return image;
    }

    /** Back to the default skin: what starting over leaves on the model. */
    public void clear() {
        this.texture.close();
    }

    /** Whether anything has been composed yet, and so whether the sheet can be shown. */
    public boolean hasTexture() {
        return this.texture.isUploaded();
    }

    /** The composed sheet itself, for the view that shows the 64x64 rather than the player. */
    public Identifier texture() {
        return this.texture.id();
    }

    /**
     * What the player widget draws. Falls back to the default skin until something has
     * been composed, so the model is on screen from the moment the editor opens rather
     * than appearing once the first request lands.
     */
    public PlayerSkin playerSkin() {
        ClientAsset.Texture body = this.texture.isUploaded()
                ? new RuntimeTexture(this.texture.id())
                : DefaultPlayerSkin.getDefaultSkin().body();
        return PlayerSkin.insecure(body, null, null, this.model);
    }

    @Override
    public void close() {
        this.texture.close();
    }

    /**
     * A texture the mod registered itself.
     *
     * <p>The game's own implementations derive the texture path from the asset id;
     * a runtime texture is registered under its name directly, so the two are the
     * same here.
     */
    private record RuntimeTexture(Identifier id) implements ClientAsset.Texture {
        @Override
        public Identifier texturePath() {
            return this.id;
        }
    }
}
