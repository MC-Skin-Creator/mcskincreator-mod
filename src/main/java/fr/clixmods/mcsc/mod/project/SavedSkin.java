/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.project;

import java.util.UUID;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/**
 * One skin the player put aside: a name, a date, and the project itself.
 *
 * <p>{@code data} is exactly what {@link fr.clixmods.mcsc.mod.skin.ProjectJson} writes
 * and reads. The entry lives in one file on this machine, see {@link SkinLibrary}, and
 * nowhere else.
 */
public record SavedSkin(String id, String name, long at, JsonObject data) {
    /** A fresh identifier, which is also the file's name: letters, digits and dashes only. */
    public static String newId() {
        return UUID.randomUUID().toString();
    }

    /** What is written to the entry's file. */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("name", this.name);
        json.addProperty("at", this.at);
        json.add("data", this.data);
        return json;
    }

    /**
     * Reads an entry back from its file.
     *
     * @return the entry, or null when the text is not one — a damaged file is skipped,
     *         it does not take the rest of the library with it
     */
    static SavedSkin parse(String id, String text) {
        try {
            JsonElement root = JsonParser.parseString(text);
            if (!root.isJsonObject()) {
                return null;
            }
            JsonObject json = root.getAsJsonObject();
            JsonElement data = json.get("data");
            if (data == null || !data.isJsonObject()) {
                return null;
            }
            return new SavedSkin(id, string(json, "name"), number(json, "at"), data.getAsJsonObject());
        } catch (JsonParseException | IllegalStateException malformed) {
            return null;
        }
    }

    private static long number(JsonObject json, String key) {
        JsonElement value = json.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()
                ? value.getAsLong()
                : 0;
    }

    private static String string(JsonObject json, String key) {
        JsonElement value = json.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                ? value.getAsString()
                : "";
    }
}
