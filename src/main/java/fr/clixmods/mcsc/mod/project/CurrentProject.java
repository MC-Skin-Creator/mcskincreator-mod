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

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import fr.clixmods.mcsc.mod.MCSkinCreatorClient;
import net.fabricmc.loader.api.FabricLoader;

/**
 * The project in progress, which outlives the editor.
 *
 * <p>There is always one. Whatever is on the stack is it, and closing the editor does
 * not end it: the next opening puts it back exactly as it was left. It is kept in two
 * places on this machine for two different reasons. A file of the config folder is what
 * brings it back at once. The player's library, {@link SkinLibrary}, is where it can be
 * <em>seen</em> — it is an entry of "My skins" like any other, under its own
 * identifier, marked as the one in progress.
 *
 * <p>Beside it, the file remembers which skin the account was wearing the last time
 * the mod looked. That is what tells the mod that the skin was changed somewhere else
 * — the launcher, the website — while a project was in progress, which is the one case
 * where the project and the account have to part ways.
 */
public final class CurrentProject {
    static final String FILE_NAME = "mcskincreator-project.json";
    private static final int VERSION = 1;
    private static final Gson GSON = new Gson();

    /**
     * What the account was last seen wearing.
     *
     * @param fingerprint the skin's pixels, as {@code SkinPixels#fingerprint} names them
     * @param address     where Mojang serves it, or empty when the mod put it there itself
     *                    and has not read it back yet
     */
    public record Account(String fingerprint, String address) {
        public Account {
            address = address == null ? "" : address;
        }
    }

    /** Everything the file holds. Either half can be missing. */
    public record State(SavedSkin project, Account account) {
    }

    private static State cached;
    private static boolean loaded;

    /**
     * Whether this game session has put a skin on the account itself.
     *
     * <p>Then the account is known without asking: it is what was sent. Asking would
     * be wrong for a while besides, since Mojang serves the old profile for some time
     * after an upload.
     */
    private static boolean appliedThisSession;

    private CurrentProject() {
    }

    /** What the file said, read once per game session. */
    public static synchronized State get() {
        if (!loaded) {
            cached = read(file());
            loaded = true;
        }
        return cached;
    }

    /** Replaces the project in progress, and writes it down. */
    public static synchronized void setProject(SavedSkin project) {
        State state = get();
        cached = new State(project, state == null ? null : state.account());
        write(file(), cached);
    }

    /** Records what the account wears, and writes it down. */
    public static synchronized void setAccount(Account account) {
        State state = get();
        cached = new State(state == null ? null : state.project(), account);
        write(file(), cached);
    }

    public static synchronized void markApplied(String fingerprint) {
        appliedThisSession = true;
        setAccount(new Account(fingerprint, ""));
    }

    public static synchronized boolean appliedThisSession() {
        return appliedThisSession;
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    /**
     * What to do with the project in progress, now that the account's skin is known.
     */
    public enum Start {
        /** No project yet: this is the first opening. */
        FIRST,
        /** Carry on with the project in progress. */
        RESUME,
        /** The account moved while a project was in progress: keep it, start another. */
        CHANGED
    }

    /**
     * @param wearing the fingerprint of the account's skin now, or null when it could
     *                not be read — offline, a default skin, a development account.
     *                What cannot be known is never taken as a change.
     */
    public static Start decide(State state, String wearing) {
        if (state == null || state.project() == null) {
            return Start.FIRST;
        }
        if (wearing == null || state.account() == null
                || state.account().fingerprint().equals(wearing)) {
            return Start.RESUME;
        }
        return Start.CHANGED;
    }

    /** @return the state, or null when there is none or it cannot be read */
    static State read(Path file) {
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            JsonObject json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            SavedSkin project = null;
            JsonElement stored = json.get("project");
            if (stored != null && stored.isJsonObject()) {
                project = SavedSkin.parseOne(stored);
            }
            Account account = null;
            JsonElement seen = json.get("account");
            if (seen != null && seen.isJsonObject()) {
                String fingerprint = string(seen.getAsJsonObject(), "skin");
                if (!fingerprint.isEmpty()) {
                    account = new Account(fingerprint, string(seen.getAsJsonObject(), "url"));
                }
            }
            return project == null && account == null ? null : new State(project, account);
        } catch (IOException | JsonParseException | IllegalStateException cause) {
            MCSkinCreatorClient.LOGGER.warn("Could not read the project in progress from {}", file, cause);
            return null;
        }
    }

    /**
     * Writes the state beside itself and moves it into place, so a game that stops
     * halfway through a write leaves the previous project rather than half of one.
     */
    static void write(Path file, State state) {
        JsonObject json = new JsonObject();
        json.addProperty("v", VERSION);
        if (state != null && state.project() != null) {
            JsonObject project = state.project().body();
            project.addProperty("id", state.project().id());
            json.add("project", project);
        }
        if (state != null && state.account() != null) {
            JsonObject account = new JsonObject();
            account.addProperty("skin", state.account().fingerprint());
            account.addProperty("url", state.account().address());
            json.add("account", account);
        }
        try {
            Files.createDirectories(file.getParent());
            Path partial = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(partial, GSON.toJson(json), StandardCharsets.UTF_8);
            try {
                Files.move(partial, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(partial, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException | RuntimeException cause) {
            MCSkinCreatorClient.LOGGER.warn("Could not write the project in progress to {}", file, cause);
        }
    }

    private static String string(JsonObject json, String key) {
        JsonElement value = json.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : "";
    }
}
