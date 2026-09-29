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

/**
 * One entry of the player's library: a name, a date, and the project itself.
 *
 * <p>{@code data} is exactly what {@link fr.clixmods.mcsc.mod.skin.ProjectJson} writes
 * and reads. An entry lives in a file on this machine, see {@link SkinLibrary}, and
 * nowhere else; nothing here carries pixels.
 *
 * @param at when the entry last changed. The mod moves it on every write, since every
 *           project in its library is one entry that saves itself, and the date worth
 *           showing is the last change.
 */
public record SavedSkin(String id, String name, long at, JsonObject data) {
    /** A fresh identifier, also the name of the entry's file: letters, digits and dashes. */
    public static String newId() {
        return UUID.randomUUID().toString();
    }

    /** What is written to the entry's file, besides its identifier. */
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

    /** One entry on its own, as the mod also writes it down: null when it is not one. */
    public static SavedSkin parseOne(JsonElement element) {
        return of(element);
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
