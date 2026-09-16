/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.catalog;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

/**
 * Reads {@code GET /api/v1/catalog} into the model above.
 *
 * <p>Deliberately reads the JSON as a tree rather than binding it to classes: the
 * catalogue serves the whole site, so it carries fields the mod has no use for
 * ({@code credits}, {@code projects}, {@code outfits}, per-element colour keys and
 * region maps) and will grow more. A tree reader ignores what it does not know and
 * survives the next field the site adds, where a bound class breaks on it.
 *
 * <p><strong>Item-less categories are dropped here.</strong> The site skips them and
 * the mod has nothing to show for one, so removing them at the edge means no screen
 * can render a dead tab. A region whose every category was dropped disappears with
 * them, since {@link Catalog#regions()} is derived from what is left.
 */
public final class CatalogParser {
    /** Categories the catalogue gives no region: they browse as one unnamed region. */
    static final String DEFAULT_REGION = "";

    private CatalogParser() {
    }

    /**
     * @throws CatalogFormatException if the payload is not a catalogue at all. A
     *                                single malformed category or element is skipped
     *                                instead: one bad entry should not cost the
     *                                player the whole library.
     */
    public static Catalog parse(String json) throws CatalogFormatException {
        JsonObject root;
        try {
            root = JsonParser.parseString(json).getAsJsonObject();
        } catch (JsonParseException | IllegalStateException cause) {
            throw new CatalogFormatException("not a JSON object", cause);
        }

        JsonElement categories = root.get("categories");
        if (categories == null || !categories.isJsonArray()) {
            throw new CatalogFormatException("no \"categories\" array");
        }

        List<CatalogCategory> parsed = new ArrayList<>();
        for (JsonElement element : categories.getAsJsonArray()) {
            if (element.isJsonObject()) {
                CatalogCategory category = category(element.getAsJsonObject());
                if (category != null) {
                    parsed.add(category);
                }
            }
        }
        return new Catalog(parsed);
    }

    /** @return the category, or {@code null} if it has no usable id or no element */
    private static CatalogCategory category(JsonObject json) {
        String id = string(json, "id");
        if (id.isBlank()) {
            return null;
        }

        List<CatalogItem> items = items(json.get("items"));
        if (items.isEmpty()) {
            return null;
        }

        String region = string(json, "region");
        return new CatalogCategory(
                id,
                region.isBlank() ? DEFAULT_REGION : region,
                text(json),
                bool(json, "single"),
                string(json, "hash"),
                items);
    }

    private static List<CatalogItem> items(JsonElement element) {
        if (element == null || !element.isJsonArray()) {
            return List.of();
        }

        JsonArray array = element.getAsJsonArray();
        List<CatalogItem> items = new ArrayList<>(array.size());
        // Counted over the whole array rather than over what is kept: the atlas holds one
        // buffer per catalogue entry, in catalogue order, so skipping an entry here must
        // not shift the ranks of the ones after it off their pixels.
        int rank = 0;
        for (JsonElement entry : array) {
            if (entry.isJsonObject()) {
                JsonObject json = entry.getAsJsonObject();
                String id = string(json, "id");
                if (!id.isBlank()) {
                    items.add(new CatalogItem(id, text(json), rank));
                }
            }
            rank++;
        }
        return List.copyOf(items);
    }

    private static CatalogText text(JsonObject json) {
        String base = string(json, "name");
        String english = string(json, "en");
        String spanish = string(json, "es");
        if (base.isBlank() && english.isBlank() && spanish.isBlank()) {
            return CatalogText.EMPTY;
        }
        return new CatalogText(base, english, spanish);
    }

    private static String string(JsonObject json, String key) {
        JsonElement element = json.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return "";
        }
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        return primitive.isString() ? primitive.getAsString() : "";
    }

    private static boolean bool(JsonObject json, String key) {
        JsonElement element = json.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return false;
        }
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        return primitive.isBoolean() && primitive.getAsBoolean();
    }
}
