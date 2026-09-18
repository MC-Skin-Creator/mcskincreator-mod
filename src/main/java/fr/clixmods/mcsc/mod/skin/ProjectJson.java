/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.skin;

import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import fr.clixmods.mcsc.mod.catalog.Catalog;
import fr.clixmods.mcsc.mod.catalog.CatalogCategory;
import fr.clixmods.mcsc.mod.catalog.CatalogItem;
import fr.clixmods.mcsc.mod.project.Layer;
import fr.clixmods.mcsc.mod.project.SkinProject;
import net.minecraft.world.entity.player.PlayerModelType;

/**
 * A project, in the one shape the server accepts.
 *
 * <p>It is the same document everywhere it travels: {@code POST /api/v1/textures} and
 * {@code /api/v1/thumbnails} compose it, {@code PUT /api/v1/skins/{id}} stores it, and
 * a stored one comes back through {@link #read} — so the editor and the site read each
 * other's skins rather than two dialects of the same thing.
 *
 * <p>The server's {@code ProjectValidator} is the authority on the format, and three
 * of its rules are not guessable, each of them silent when broken:
 *
 * <ul>
 *   <li>the model is <strong>{@code slim}</strong>, a boolean at the top level, not a
 *       {@code model} string;</li>
 *   <li>a layer names its element with <strong>{@code cat}</strong> and
 *       <strong>{@code preset}</strong>;</li>
 *   <li>the numbers are <strong>factors</strong>, not percentages: opacity runs 0 to 1,
 *       {@code sat} 0 to 2 and {@code lum} -1 to 1, while the sliders of this mod are
 *       whole percentages. {@link Layer} keeps the percentages, this class converts.</li>
 * </ul>
 *
 * <p>Hidden layers are written out like any other, carrying {@code visible: false}: the
 * server skips them when it composes, and a project that dropped them would come back
 * from storage with layers missing.
 */
public final class ProjectJson {
    /** The only document version the server knows. */
    public static final int VERSION = 1;

    /**
     * The regions the server accepts. A catalogue region outside this set is left out
     * rather than sent: the validator refuses an unknown one, and the site's own reader
     * falls back to {@code base}.
     */
    private static final Set<String> REGIONS = Set.of("base", "head", "torso", "arms", "legs");

    private static final Gson GSON = new Gson();

    private ProjectJson() {
    }

    /** The whole project, bottom to top: what is composed, and what is stored. */
    public static String project(SkinProject project) {
        JsonArray layers = new JsonArray();
        for (Layer layer : project.layers()) {
            layers.add(layer(layer));
        }
        return GSON.toJson(document(project.isSlim(), layers));
    }

    /** A project holding nothing but {@code itemId} of {@code categoryId}. */
    public static String singlePreset(String categoryId, String itemId, boolean slim) {
        JsonArray layers = new JsonArray();
        layers.add(preset(categoryId, itemId));
        return GSON.toJson(document(slim, layers));
    }

    private static JsonObject document(boolean slim, JsonArray layers) {
        JsonObject body = new JsonObject();
        body.addProperty("v", VERSION);
        body.addProperty("slim", slim);
        body.add("layers", layers);
        return body;
    }

    /**
     * One layer.
     *
     * <p>Fields left at their default are left out — the server fills them in, and a
     * body carrying only what was actually changed is the one that stays readable when
     * something is refused by path.
     */
    private static JsonObject layer(Layer layer) {
        JsonObject entry = preset(layer.categoryId(), layer.itemId());
        entry.addProperty("name", layer.name().getString());
        if (REGIONS.contains(layer.region())) {
            entry.addProperty("region", layer.region());
        }
        if (!layer.visible()) {
            entry.addProperty("visible", false);
        }
        if (layer.opacity() != 100) {
            entry.addProperty("opacity", factor(layer.opacity()));
        }
        if (!layer.isUnadjusted()) {
            JsonObject adjustments = new JsonObject();
            adjustments.addProperty("hue", layer.hue());
            adjustments.addProperty("sat", factor(layer.saturation()));
            adjustments.addProperty("lum", factor(layer.brightness()));
            entry.add("adj", adjustments);
        }
        return entry;
    }

    private static JsonObject preset(String categoryId, String itemId) {
        JsonObject entry = new JsonObject();
        entry.addProperty("kind", "preset");
        entry.addProperty("cat", categoryId);
        entry.addProperty("preset", itemId);
        return entry;
    }

    /** A whole percentage as the factor the server reads. */
    private static double factor(int percent) {
        return percent / 100.0;
    }

    private static int percent(JsonObject json, String key, int fallback) {
        JsonElement value = json.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            return fallback;
        }
        return (int) Math.round(value.getAsDouble() * 100);
    }

    /**
     * Reads a stored project back into the editor.
     *
     * <p>An element the catalogue no longer carries is skipped rather than refused: the
     * catalogue loses entries — a texture recut, a source unpublished — and a skin saved
     * last month should open with the layers that are still there instead of not
     * opening at all. How many were dropped is the return value, so the screen can say
     * so.
     *
     * <p>A layer the mod cannot represent is dropped the same way and counted the same:
     * a drawing layer ({@code kind: paint}) is the site's, and the mod has no pixel
     * tools yet.
     *
     * @return how many layers of the document could not be restored
     */
    public static int read(JsonObject document, SkinProject into, Catalog catalog, String languageCode) {
        into.clear();
        into.setModel(bool(document, "slim") ? PlayerModelType.SLIM : PlayerModelType.WIDE);

        JsonElement layers = document.get("layers");
        if (layers == null || !layers.isJsonArray()) {
            return 0;
        }

        int dropped = 0;
        for (JsonElement element : layers.getAsJsonArray()) {
            if (!element.isJsonObject() || !restore(element.getAsJsonObject(), into, catalog, languageCode)) {
                dropped++;
            }
        }
        into.select(null);
        return dropped;
    }

    /** @return false when the layer is not one the mod can put back */
    private static boolean restore(JsonObject entry, SkinProject into, Catalog catalog, String languageCode) {
        if (!"preset".equals(string(entry, "kind"))) {
            return false;
        }
        CatalogCategory category = catalog.category(string(entry, "cat")).orElse(null);
        if (category == null) {
            return false;
        }
        String itemId = string(entry, "preset");
        CatalogItem item = category.items().stream()
                .filter(candidate -> candidate.id().equals(itemId))
                .findFirst()
                .orElse(null);
        if (item == null) {
            return false;
        }

        Layer layer = into.add(category, item, languageCode);
        if (layer == null) {
            return false;
        }
        layer.setVisible(!entry.has("visible") || bool(entry, "visible"));
        layer.setOpacity(percent(entry, "opacity", 100));

        JsonElement adjustments = entry.get("adj");
        if (adjustments != null && adjustments.isJsonObject()) {
            JsonObject adj = adjustments.getAsJsonObject();
            JsonElement hue = adj.get("hue");
            layer.setHue(hue != null && hue.isJsonPrimitive() ? (int) Math.round(hue.getAsDouble()) : 0);
            layer.setSaturation(percent(adj, "sat", 100));
            layer.setBrightness(percent(adj, "lum", 0));
        }
        return true;
    }

    private static String string(JsonObject json, String key) {
        JsonElement value = json.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                ? value.getAsString()
                : "";
    }

    private static boolean bool(JsonObject json, String key) {
        JsonElement value = json.get(key);
        return value != null && value.isJsonPrimitive()
                && value.getAsJsonPrimitive().isBoolean() && value.getAsBoolean();
    }
}
