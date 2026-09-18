/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.catalog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
 * ({@code projects}, {@code outfits}, {@code dir}, {@code ajout}, {@code base}) and
 * will grow more. A tree reader ignores what it does not know and survives the next
 * field the site adds, where a bound class breaks on it.
 *
 * <p>Two details of the real schema are easy to get wrong and both are silent when
 * you do. A <strong>category</strong> names itself with {@code label}, while an
 * <strong>element</strong> names itself with {@code name}; reading {@code name} on a
 * category leaves every tab blank. And an element's rank in the atlas is
 * <strong>not</strong> its rank in {@code items}: an element with a slim variant
 * occupies two consecutive buffers, so the ranks run ahead of the list and every
 * element after the first slim one would otherwise be drawn with another's pixels.
 *
 * <p>Item-less categories are dropped here. The site skips them and the mod has
 * nothing to show for one, so removing them at the edge means no screen can render a
 * dead tab. A region whose every category was dropped disappears with them, since
 * {@link Catalog#regions()} is derived from what is left.
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
        return new Catalog(parsed, works(root.get("credits")));
    }

    /**
     * The table of origins, by key.
     *
     * <p>Anything that is not a table of objects reads as no credits at all rather than
     * as a broken catalogue: the library is worth showing without provenance, and the
     * interface already has a sentence for an element that has none.
     */
    private static Map<String, CatalogWork> works(JsonElement credits) {
        if (credits == null || !credits.isJsonObject()) {
            return Map.of();
        }

        Map<String, CatalogWork> works = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : credits.getAsJsonObject().entrySet()) {
            CatalogWork work = CatalogWork.of(entry.getValue());
            if (!work.isEmpty()) {
                works.put(entry.getKey(), work);
            }
        }
        return Map.copyOf(works);
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
                // A category labels itself with "label"; only its elements use "name".
                text(json, "label"),
                bool(json, "single"),
                ThumbCrop.of(string(json, "thumbCrop")),
                atlasPath(json.get("atlas")),
                items);
    }

    /** The category's atlas address, as {@code {"url": "…"}} in the catalogue. */
    private static String atlasPath(JsonElement atlas) {
        if (atlas == null || !atlas.isJsonObject()) {
            return "";
        }
        return string(atlas.getAsJsonObject(), "url");
    }

    private static List<CatalogItem> items(JsonElement element) {
        if (element == null || !element.isJsonArray()) {
            return List.of();
        }

        JsonArray array = element.getAsJsonArray();
        List<CatalogItem> items = new ArrayList<>(array.size());
        // The atlas holds one buffer per element, plus a second one for every element
        // that has a slim variant, in catalogue order. So the rank is a running count
        // over the whole array - not the position in the list the mod keeps, and not
        // the position in "items" either.
        int rank = 0;
        for (JsonElement entry : array) {
            if (!entry.isJsonObject()) {
                continue;
            }

            JsonObject json = entry.getAsJsonObject();
            String id = string(json, "id");
            boolean slim = truthy(json, "slim");
            int atlasIndex = rank++;
            int slimIndex = CatalogItem.NONE;
            if (slim) {
                slimIndex = rank++;
            }

            if (!id.isBlank()) {
                String crop = string(json, "thumbCrop");
                items.add(new CatalogItem(id, text(json, "name"), atlasIndex, slimIndex,
                        crop.isBlank() ? null : ThumbCrop.of(crop), string(json, "credit")));
            }
        }
        return List.copyOf(items);
    }

    private static CatalogText text(JsonObject json, String defaultKey) {
        String base = string(json, defaultKey);
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

    /**
     * A flag the catalogue writes as a number: {@code "slim": 1}. Read loosely so that
     * a boolean would do just as well, because which one it is is not worth a bug.
     */
    private static boolean truthy(JsonObject json, String key) {
        JsonElement element = json.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return false;
        }
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (primitive.isBoolean()) {
            return primitive.getAsBoolean();
        }
        return primitive.isNumber() && primitive.getAsInt() != 0;
    }
}
