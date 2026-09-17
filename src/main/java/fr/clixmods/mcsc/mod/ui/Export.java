/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import com.mojang.blaze3d.platform.NativeImage;

import fr.clixmods.mcsc.mod.MCSkinCreatorClient;
import fr.clixmods.mcsc.mod.skin.PreviewSkin;
import net.minecraft.client.Minecraft;

/** Writing the composed skin out as a 64x64 PNG, next to the game's own files. */
public final class Export {
    /** Failures of this kind are announced once, then not again until one succeeds. */
    public static final String KIND = "export";

    private Export() {
    }

    /** Where exported skins go: one folder of the game directory, created on demand. */
    public static Path folder(Minecraft client) {
        return client.gameDirectory.toPath().resolve(MCSkinCreatorClient.MOD_ID);
    }

    /**
     * Writes a composed sheet to {@code <game>/mcskincreator/<name>.png}.
     *
     * <p>{@code sheet} is what the server sent back, in either of the two shapes it
     * answers with, so what lands on disk is exactly what was on the model.
     *
     * @return the file written, or null when it could not be written
     */
    public static Path write(Minecraft client, byte[] sheet, String name) {
        try (NativeImage image = PreviewSkin.decode(sheet)) {
            Path folder = folder(client);
            Files.createDirectories(folder);
            Path file = folder.resolve(safe(name) + ".png");
            image.writeToFile(file);
            return file;
        } catch (Exception failure) {
            // Reported to the player through a notification; logged here because the
            // notification has room for one sentence and this does not.
            MCSkinCreatorClient.LOGGER.warn("Could not export the skin", failure);
            return null;
        }
    }

    /** Keeps a player-typed name to something a file system will accept. */
    private static String safe(String name) {
        String cleaned = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]+", "-");
        cleaned = cleaned.replaceAll("^-+|-+$", "");
        return cleaned.isEmpty() ? "skin" : cleaned;
    }
}
