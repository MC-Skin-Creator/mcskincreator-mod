/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.project;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import fr.clixmods.mcsc.mod.MCSkinCreatorClient;

/**
 * The saved skins, kept in a folder on this machine and nowhere else.
 *
 * <p>One {@code <id>.json} per skin, and beside it a {@code <id>.png} holding its front
 * view once one has been drawn. Nothing here touches the network, and nothing here is
 * identified: there is no account and no client identifier, the folder <em>is</em> the
 * library.
 *
 * <p>Every call does file input and output, so callers keep it off the render thread.
 */
public final class SkinLibrary {
    private static final String SKIN = ".json";
    private static final String PICTURE = ".png";

    private final Path folder;

    public SkinLibrary(Path folder) {
        this.folder = folder;
    }

    /** The saved skins, the last one saved first. Unreadable files are left out. */
    public List<SavedSkin> list() throws IOException {
        if (!Files.isDirectory(this.folder)) {
            return List.of();
        }
        List<SavedSkin> skins = new ArrayList<>();
        try (Stream<Path> files = Files.list(this.folder)) {
            for (Path file : files.filter(f -> f.getFileName().toString().endsWith(SKIN)).toList()) {
                String name = file.getFileName().toString();
                String id = name.substring(0, name.length() - SKIN.length());
                try {
                    SavedSkin skin = SavedSkin.parse(id, Files.readString(file, StandardCharsets.UTF_8));
                    if (skin != null) {
                        skins.add(skin);
                        continue;
                    }
                } catch (IOException | RuntimeException cause) {
                    MCSkinCreatorClient.LOGGER.warn("Could not read the saved skin {}", file, cause);
                    continue;
                }
                MCSkinCreatorClient.LOGGER.warn("{} is not a saved skin; skipping it", file);
            }
        }
        skins.sort(Comparator.comparingLong(SavedSkin::at).reversed().thenComparing(SavedSkin::id));
        return List.copyOf(skins);
    }

    public void save(SavedSkin skin) throws IOException {
        Files.createDirectories(this.folder);
        Files.writeString(file(skin.id(), SKIN), skin.toJson().toString(), StandardCharsets.UTF_8);
    }

    public void delete(String id) throws IOException {
        Files.deleteIfExists(file(id, SKIN));
        Files.deleteIfExists(file(id, PICTURE));
    }

    /** The front view kept for this skin, if one has been drawn already. */
    public Optional<byte[]> picture(String id) throws IOException {
        Path file = file(id, PICTURE);
        return Files.isRegularFile(file) ? Optional.of(Files.readAllBytes(file)) : Optional.empty();
    }

    public void savePicture(String id, byte[] png) throws IOException {
        Files.createDirectories(this.folder);
        Files.write(file(id, PICTURE), png);
    }

    private Path file(String id, String extension) {
        // An identifier this mod drew is a UUID, but a file can be dropped in by hand.
        // Anything that could climb out of the folder is refused rather than resolved.
        if (id.isEmpty() || !id.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("not a skin identifier: " + id);
        }
        return this.folder.resolve(id + extension);
    }
}
