/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.skin;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import com.mojang.blaze3d.platform.NativeImage;

import fr.clixmods.mcsc.mod.catalog.CatalogModel;

/**
 * Telling whether two skins are the same skin, by their pixels.
 *
 * <p>Two files of the same skin are rarely the same bytes: Mojang, the site and this
 * mod each encode their own PNG. So skins are compared as 64x64 straight-alpha RGBA,
 * and a pixel nobody can see is one pixel whatever colour it was left holding — a
 * fully transparent texel is written out as zero before anything is compared.
 */
public final class SkinPixels {
    private static final int BYTES = Composite.BYTES;

    private SkinPixels() {
    }

    /**
     * A skin in the one shape the comparisons read: 64x64 RGBA, red first.
     *
     * <p>Must run where a {@link NativeImage} can be made, which is anywhere in the
     * client; nothing is uploaded.
     *
     * @return the pixels, or null for an image that is not a 64x64 skin — the legacy
     *         64x32 sheet included, which no model of the catalogue can be
     */
    public static byte[] rgba(byte[] payload) throws IOException {
        if (payload.length == BYTES && !Png.isPng(payload)) {
            return normalized(payload);
        }
        try (NativeImage image = NativeImage.read(payload)) {
            if (image.getWidth() != FrontSprite.SKIN_SIZE || image.getHeight() != FrontSprite.SKIN_SIZE) {
                return null;
            }
            byte[] rgba = new byte[BYTES];
            for (int y = 0; y < FrontSprite.SKIN_SIZE; y++) {
                for (int x = 0; x < FrontSprite.SKIN_SIZE; x++) {
                    int argb = image.getPixel(x, y);
                    int offset = (y * FrontSprite.SKIN_SIZE + x) * 4;
                    rgba[offset] = (byte) (argb >>> 16);
                    rgba[offset + 1] = (byte) (argb >>> 8);
                    rgba[offset + 2] = (byte) argb;
                    rgba[offset + 3] = (byte) (argb >>> 24);
                }
            }
            return normalized(rgba);
        }
    }

    /** A copy in which every invisible texel reads as zero. */
    public static byte[] normalized(byte[] rgba) {
        byte[] copy = rgba.clone();
        for (int offset = 0; offset + 3 < copy.length; offset += 4) {
            if (copy[offset + 3] == 0) {
                copy[offset] = 0;
                copy[offset + 1] = 0;
                copy[offset + 2] = 0;
            }
        }
        return copy;
    }

    /** Whether two sheets show the same skin. Either being null means they do not. */
    public static boolean same(byte[] first, byte[] second) {
        if (first == null || second == null || first.length != BYTES || second.length != BYTES) {
            return false;
        }
        for (int offset = 0; offset < BYTES; offset += 4) {
            int alpha = first[offset + 3];
            if (alpha != second[offset + 3]) {
                return false;
            }
            if (alpha != 0 && (first[offset] != second[offset]
                    || first[offset + 1] != second[offset + 1]
                    || first[offset + 2] != second[offset + 2])) {
                return false;
            }
        }
        return true;
    }

    /**
     * A short name for a skin's pixels, which is what the mod writes down to recognise
     * a skin again without keeping it.
     */
    public static String fingerprint(byte[] rgba) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(normalized(rgba));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            // Every Java runtime is required to carry SHA-256.
            throw new IllegalStateException(impossible);
        }
    }

    /**
     * The model {@code skin} is, if it is exactly one of them.
     *
     * @param pictures the models' own sheets, in the order of {@code models} — the ones
     *                 {@link ReadyMadeSkins#of} draws. A transparent one, for a model
     *                 whose pieces have not all arrived, matches nothing.
     */
    public static Optional<CatalogModel> match(List<CatalogModel> models, List<byte[]> pictures,
                                               byte[] skin) {
        if (skin == null) {
            return Optional.empty();
        }
        for (int index = 0; index < models.size() && index < pictures.size(); index++) {
            if (same(skin, normalized(pictures.get(index)))) {
                return Optional.of(models.get(index));
            }
        }
        return Optional.empty();
    }
}
