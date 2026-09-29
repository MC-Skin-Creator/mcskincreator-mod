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
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import fr.clixmods.mcsc.mod.MCSkinCreatorClient;

/**
 * The saved skins, kept in a folder on this machine and nowhere else.
 *
 * <p>One {@code <id>.json} per skin, and beside it a {@code <id>.png} holding its front
 * view once one has been drawn. Nothing here touches the network and nothing is
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

    /** The saved skins, the last changed first. Unreadable files are left out. */
    public List<SavedSkin> list() throws IOException {
        if (!Files.isDirectory(this.folder)) {
            return List.of();
        }
        List<SavedSkin> skins = new ArrayList<>();
        try (Stream<Path> files = Files.list(this.folder)) {
            for (Path file : files.filter(f -> f.getFileName().toString().endsWith(SKIN)).toList()) {
                SavedSkin skin = read(file);
                if (skin != null) {
                    skins.add(skin);
                }
            }
        }
        skins.sort(Comparator.comparingLong(SavedSkin::at).reversed().thenComparing(SavedSkin::id));
        return List.copyOf(skins);
    }

    private static SavedSkin read(Path file) {
        String name = file.getFileName().toString();
        String id = name.substring(0, name.length() - SKIN.length());
        try {
            SavedSkin skin = SavedSkin.parseOne(JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)));
            // The file's name is the identifier, whatever a hand-edited body says.
            return skin == null || !skin.id().equals(id) ? null : skin;
        } catch (IOException | JsonParseException | IllegalStateException cause) {
            MCSkinCreatorClient.LOGGER.warn("Could not read the saved skin {}", file, cause);
            return null;
        }
    }

    /** Writes the skin, and drops the picture kept for its previous version. */
    public void save(SavedSkin skin) throws IOException {
        Files.createDirectories(this.folder);
        JsonObject json = skin.body();
        json.addProperty("id", skin.id());
        Path target = file(skin.id(), SKIN);
        // Beside itself and then moved, so a game that stops halfway leaves the
        // previous version rather than half of one.
        Path partial = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(partial, json.toString(), StandardCharsets.UTF_8);
        try {
            Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING);
        }
        Files.deleteIfExists(file(skin.id(), PICTURE));
    }

    public void delete(String id) throws IOException {
        Files.deleteIfExists(file(id, SKIN));
        Files.deleteIfExists(file(id, PICTURE));
    }

    /** The front view kept for this skin, if one has been drawn since it last changed. */
    public Optional<byte[]> picture(String id) throws IOException {
        Path file = file(id, PICTURE);
        return Files.isRegularFile(file) ? Optional.of(Files.readAllBytes(file)) : Optional.empty();
    }

    public void savePicture(String id, byte[] png) throws IOException {
        Files.createDirectories(this.folder);
        Files.write(file(id, PICTURE), png);
    }

    private Path file(String id, String extension) {
        // An identifier this mod drew is a UUID; anything that could climb out of the
        // folder is refused rather than resolved.
        if (!id.matches("[A-Za-z0-9_-]{1,64}")) {
            throw new IllegalArgumentException("not a skin identifier: " + id);
        }
        return this.folder.resolve(id + extension);
    }
}
