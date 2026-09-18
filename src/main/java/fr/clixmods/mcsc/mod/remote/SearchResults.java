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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import fr.clixmods.mcsc.mod.catalog.CatalogFormatException;

/**
 * The answer of {@code GET /api/v1/search}: what matches, across every region.
 *
 * <p>Searching is the server's rather than the panel's for one reason — it looks in
 * the three languages the catalogue carries at once. A player running the game in
 * English finds an element whose English name was never written, because its French
 * one matches; filtering the loaded names in the player's language, which is what the
 * panel falls back to when the server cannot be reached, finds nothing there.
 *
 * <p>Outfits come back too ({@code type: outfit}), and they have no category. The mod
 * has no outfit list to stack them from, so it keeps the presets and counts the rest
 * — {@link #total} stays the server's count, which is what a "and N more" line needs.
 */
public record SearchResults(String query, int total, int page, int size, List<Hit> hits) {
    public static final SearchResults NONE = new SearchResults("", 0, 0, 0, List.of());

    /** One match: an element of a category, or an outfit, which has none. */
    public record Hit(String type, String category, String id) {
        public boolean isPreset() {
            return "preset".equals(this.type);
        }
    }

    public SearchResults {
        hits = List.copyOf(hits);
    }

    public static SearchResults parse(String json) throws CatalogFormatException {
        JsonObject root;
        try {
            root = JsonParser.parseString(json).getAsJsonObject();
        } catch (JsonParseException | IllegalStateException cause) {
            throw new CatalogFormatException("not a search answer", cause);
        }

        List<Hit> hits = new ArrayList<>();
        JsonElement results = root.get("results");
        if (results != null && results.isJsonArray()) {
            for (JsonElement element : (JsonArray) results) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject hit = element.getAsJsonObject();
                String id = string(hit, "id");
                if (!id.isBlank()) {
                    hits.add(new Hit(string(hit, "type"), string(hit, "cat"), id));
                }
            }
        }
        return new SearchResults(string(root, "q"), number(root, "total", hits.size()),
                number(root, "page", 0), number(root, "size", hits.size()), hits);
    }

    private static String string(JsonObject json, String key) {
        JsonElement value = json.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                ? value.getAsString()
                : "";
    }

    private static int number(JsonObject json, String key, int fallback) {
        JsonElement value = json.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()
                ? value.getAsInt()
                : fallback;
    }
}
