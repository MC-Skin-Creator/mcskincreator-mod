/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.remote;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * What the mod keeps of the API's answers between two launches: the catalogue with the
 * {@code ETag} it came with, and the atlases, one file per content hash.
 *
 * <p>Every write goes to a temporary file that is then renamed over the target, so a
 * crash leaves the previous file or none, never half of one. The catalogue's {@code
 * ETag} is dropped <em>before</em> its body is replaced and written back after, which
 * means a body that is incomplete or out of step is never paired with an {@code ETag}
 * that would make the server answer 304 for it.
 *
 * <p>Nothing here throws: a cache that cannot be written is a cache that is not there,
 * and the only cost is downloading again.
 */
final class DiskCache {
    private static final String CATALOG = "catalog.json";
    private static final String CATALOG_ETAG = "catalog.etag";
    private static final String ATLAS_DIR = "atlas";

    private final Path root;

    DiskCache(Path root) {
        this.root = root;
    }

    /** The catalogue as the server sent it, decompressed. */
    Optional<String> catalog() {
        return read(this.root.resolve(CATALOG)).map(bytes -> new String(bytes, StandardCharsets.UTF_8));
    }

    /** The {@code ETag} to send back, exactly as it was received. */
    Optional<String> etag() {
        return read(this.root.resolve(CATALOG_ETAG))
                .map(bytes -> new String(bytes, StandardCharsets.UTF_8).trim())
                .filter(etag -> !etag.isEmpty());
    }

    /** Replaces the catalogue, and its {@code ETag} with it; a null one clears it. */
    void storeCatalog(String body, String etag) {
        try {
            Files.createDirectories(this.root);
            Files.deleteIfExists(this.root.resolve(CATALOG_ETAG));
            write(this.root.resolve(CATALOG), body.getBytes(StandardCharsets.UTF_8));
            if (etag != null && !etag.isBlank()) {
                write(this.root.resolve(CATALOG_ETAG), etag.getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException | RuntimeException cause) {
            forgetEtag();
        }
    }

    /** A cached body that could not be read back as a catalogue is not to be trusted. */
    void forgetCatalog() {
        forgetEtag();
        try {
            Files.deleteIfExists(this.root.resolve(CATALOG));
        } catch (IOException ignored) {
            // Nothing sound to do about it.
        }
    }

    private void forgetEtag() {
        try {
            Files.deleteIfExists(this.root.resolve(CATALOG_ETAG));
        } catch (IOException ignored) {
            // Nothing sound to do about it.
        }
    }

    /** An atlas by category and hash: the hash is in the name, so it is never stale. */
    Optional<byte[]> atlas(String categoryId, String hash) {
        return read(atlasFile(categoryId, hash));
    }

    /** Keeps an atlas, and drops the older ones of the same category. */
    void storeAtlas(String categoryId, String hash, byte[] atlas) {
        Path file = atlasFile(categoryId, hash);
        try {
            Files.createDirectories(file.getParent());
            write(file, atlas);
            String prefix = file.getFileName().toString();
            String category = prefix.substring(0, prefix.indexOf('.') + 1);
            try (Stream<Path> siblings = Files.list(file.getParent())) {
                for (Path sibling : (Iterable<Path>) siblings::iterator) {
                    String name = sibling.getFileName().toString();
                    if (name.startsWith(category) && !name.equals(prefix)) {
                        Files.deleteIfExists(sibling);
                    }
                }
            }
        } catch (IOException | RuntimeException ignored) {
            // Not kept, so fetched again next time.
        }
    }

    void forgetAtlas(String categoryId, String hash) {
        try {
            Files.deleteIfExists(atlasFile(categoryId, hash));
        } catch (IOException ignored) {
            // Nothing sound to do about it.
        }
    }

    /** {@code <category>.<hash>.bin}, both parts reduced to what is safe in a file name. */
    private Path atlasFile(String categoryId, String hash) {
        return this.root.resolve(ATLAS_DIR).resolve(safe(categoryId) + "." + safe(hash) + ".bin");
    }

    private static String safe(String part) {
        return part.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    private static Optional<byte[]> read(Path file) {
        try {
            return Files.isRegularFile(file) ? Optional.of(Files.readAllBytes(file)) : Optional.empty();
        } catch (IOException | RuntimeException cause) {
            return Optional.empty();
        }
    }

    private static void write(Path target, byte[] bytes) throws IOException {
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        Files.write(temporary, bytes);
        try {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
