/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.remote;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import fr.clixmods.mcsc.mod.catalog.CatalogFormatException;

/**
 * One entry of the player's library, as {@code /api/v1/skins} keeps it: a name, a
 * date, and the project itself.
 *
 * <p>{@code data} is exactly what {@link fr.clixmods.mcsc.mod.skin.ProjectJson} writes
 * and reads, which is what makes a skin saved in the game open on the site and the
 * other way round. The server composes its texture on every write and serves it at
 * {@code /skins/{id}/texture.png}, so nothing here carries pixels.
 *
 * @param at when the entry was last saved <em>on purpose</em>. The site does not move
 *           it on an autosave, and neither does the mod: it is what tells a version
 *           deliberately put aside from one that merely drifted.
 */
public record SavedSkin(String id, String name, long at, JsonObject data) {
    /** What the server accepts as an identifier: {@code [A-Za-z0-9_-]{1,64}}. */
    public static String newId() {
        return UUID.randomUUID().toString();
    }

    /** The body of {@code PUT /api/v1/skins/{id}}. */
    public JsonObject body() {
        JsonObject body = new JsonObject();
        body.addProperty("name", this.name);
        body.addProperty("at", this.at);
        body.add("data", this.data);
        return body;
    }

    public SavedSkin renamedTo(String name) {
        return new SavedSkin(this.id, name, System.currentTimeMillis(), this.data);
    }

    /** Reads {@code GET /api/v1/skins}, newest first — the order the server sends. */
    public static List<SavedSkin> parseList(String json) throws CatalogFormatException {
        JsonArray array;
        try {
            array = JsonParser.parseString(json).getAsJsonArray();
        } catch (JsonParseException | IllegalStateException cause) {
            throw new CatalogFormatException("not a list of skins", cause);
        }

        List<SavedSkin> skins = new ArrayList<>(array.size());
        for (JsonElement element : array) {
            SavedSkin skin = of(element);
            if (skin != null) {
                skins.add(skin);
            }
        }
        return List.copyOf(skins);
    }

    /** @return the entry, or null when it carries no identifier or no project */
    static SavedSkin of(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return null;
        }
        JsonObject json = element.getAsJsonObject();
        String id = string(json, "id");
        JsonElement data = json.get("data");
        if (id.isBlank() || data == null || !data.isJsonObject()) {
            return null;
        }

        JsonElement at = json.get("at");
        long saved = at != null && at.isJsonPrimitive() && at.getAsJsonPrimitive().isNumber()
                ? at.getAsLong()
                : 0;
        return new SavedSkin(id, string(json, "name"), saved, data.getAsJsonObject());
    }

    private static String string(JsonObject json, String key) {
        JsonElement value = json.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                ? value.getAsString()
                : "";
    }
}
