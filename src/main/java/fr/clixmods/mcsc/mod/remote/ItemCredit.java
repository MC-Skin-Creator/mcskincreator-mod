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
import fr.clixmods.mcsc.mod.catalog.CatalogText;
import fr.clixmods.mcsc.mod.catalog.CatalogWork;

/**
 * The answer of {@code GET /api/v1/presets/{cat}/{id}/credit}: where an element came
 * from, and which starter models it is a piece of.
 *
 * <p>The second half is the part the catalogue cannot give. Nearly every element is
 * cut out of one texture, so knowing whether it is the piece of one character, a piece
 * ten of them share, or the orphan of none says what to expect of it elsewhere. The
 * server names eight at most and counts the rest — the four-texel eye comes back in
 * dozens of them.
 *
 * @param models   the starter models naming this element, eight at most
 * @param total    how many there are in all, which is what {@code models} is a head of
 */
public record ItemCredit(CatalogWork work, List<Model> models, int total) {
    public static final ItemCredit UNKNOWN = new ItemCredit(CatalogWork.NONE, List.of(), 0);

    /** One starter model, with the three labels the catalogue carries for it. */
    public record Model(String id, CatalogText name) {
    }

    public ItemCredit {
        models = List.copyOf(models);
    }

    public boolean hasWork() {
        return !this.work.isEmpty();
    }

    /** How many models are named nowhere in {@link #models}, because eight was enough. */
    public int unnamedModels() {
        return Math.max(0, this.total - this.models.size());
    }

    public static ItemCredit parse(String json) throws CatalogFormatException {
        JsonObject root;
        try {
            root = JsonParser.parseString(json).getAsJsonObject();
        } catch (JsonParseException | IllegalStateException cause) {
            throw new CatalogFormatException("not a credit card", cause);
        }

        List<Model> models = new ArrayList<>();
        JsonElement projects = root.get("projects");
        if (projects != null && projects.isJsonArray()) {
            for (JsonElement element : (JsonArray) projects) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject model = element.getAsJsonObject();
                String id = string(model, "id");
                if (!id.isBlank()) {
                    models.add(new Model(id, new CatalogText(string(model, "name"),
                            string(model, "en"), string(model, "es"))));
                }
            }
        }

        JsonElement total = root.get("total");
        int count = total != null && total.isJsonPrimitive() && total.getAsJsonPrimitive().isNumber()
                ? total.getAsInt()
                : models.size();
        return new ItemCredit(CatalogWork.of(root.get("work")), models, count);
    }

    private static String string(JsonObject json, String key) {
        JsonElement value = json.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                ? value.getAsString()
                : "";
    }
}
