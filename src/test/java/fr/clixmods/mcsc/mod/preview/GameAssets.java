/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.preview;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The game's own assets, read out of the Minecraft jar the tests already compile
 * against.
 *
 * <p>That jar is on the test classpath — it has to be, or nothing in this source tree
 * would resolve {@code Component} — and it carries every texture, every sprite
 * definition and every translation the game ships. So the preview needs no download,
 * no resource pack and no running game: it opens the same files the client opens.
 *
 * <p>Anything missing comes back null rather than throwing. A preview is a picture,
 * and a picture with one sprite missing is still worth looking at; a preview that
 * refuses to render because of one is not.
 */
final class GameAssets {
    private static final Map<String, BufferedImage> IMAGES = new HashMap<>();

    private GameAssets() {
    }

    static InputStream open(String path) {
        return GameAssets.class.getClassLoader().getResourceAsStream(path);
    }

    /** A texture, cached — the same sprite is drawn dozens of times in one frame. */
    static BufferedImage image(String path) {
        return IMAGES.computeIfAbsent(path, key -> {
            try (InputStream stream = open(key)) {
                return stream == null ? null : ImageIO.read(stream);
            } catch (IOException failure) {
                return null;
            }
        });
    }

    static JsonObject json(String path) {
        try (InputStream stream = open(path)) {
            if (stream == null) {
                return null;
            }
            return JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException | RuntimeException failure) {
            return null;
        }
    }

    /** Turns {@code minecraft:font/ascii.png} into the path it lives at in the jar. */
    static String texturePath(String reference) {
        int colon = reference.indexOf(':');
        String namespace = colon < 0 ? "minecraft" : reference.substring(0, colon);
        String path = colon < 0 ? reference : reference.substring(colon + 1);
        return "assets/" + namespace + "/textures/" + path;
    }
}
