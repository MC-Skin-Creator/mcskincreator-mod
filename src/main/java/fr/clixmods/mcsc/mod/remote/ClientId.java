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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import fr.clixmods.mcsc.mod.MCSkinCreatorClient;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Who the player is to the server: a UUID this installation drew once.
 *
 * <p>The storage side of the API has no accounts. Every call to {@code /api/v1/skins}
 * carries an {@code X-Client-Id} header, a UUID and nothing else, and the server
 * refuses the call outright without one — there is no shared place it could write to
 * instead. The site draws the same thing in the browser and keeps it in local storage.
 *
 * <p>So it is kept in a file of the config folder, and that file <strong>is</strong>
 * the library: losing it does not lose the skins on the server, but it does lose the
 * way back to them. It is written once, never rewritten, and a file that has been
 * damaged is replaced rather than sent — a malformed id would be refused on every
 * single call.
 *
 * <p>The day accounts exist, one will gather several of these without the mod's side
 * of the contract changing.
 */
public final class ClientId {
    static final String FILE_NAME = "mcskincreator-client.txt";

    private static String cached;

    private ClientId() {
    }

    /** The id of this installation, drawn and written down on first use. */
    public static synchronized String get() {
        if (cached == null) {
            cached = read(FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME));
        }
        return cached;
    }

    /**
     * Reads the id from a file, drawing and writing a new one when there is nothing
     * usable there.
     *
     * <p>A failure to write is not a failure to run: the id is returned anyway, and
     * the only consequence is that this session's library is not found again next
     * time. Refusing to open the editor over it would be worse.
     */
    static String read(Path file) {
        try {
            if (Files.isRegularFile(file)) {
                String stored = Files.readString(file, StandardCharsets.UTF_8).trim();
                if (isUuid(stored)) {
                    return stored;
                }
                MCSkinCreatorClient.LOGGER.warn("{} does not hold a client id; drawing a new one", file);
            }
        } catch (IOException | RuntimeException cause) {
            MCSkinCreatorClient.LOGGER.warn("Could not read the client id from {}", file, cause);
        }

        String drawn = UUID.randomUUID().toString();
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, drawn + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException cause) {
            MCSkinCreatorClient.LOGGER.warn("Could not write the client id to {}; "
                    + "this session's saved skins will not be found again", file, cause);
        }
        return drawn;
    }

    static boolean isUuid(String candidate) {
        try {
            return candidate != null && UUID.fromString(candidate).toString().equalsIgnoreCase(candidate);
        } catch (IllegalArgumentException malformed) {
            return false;
        }
    }
}
