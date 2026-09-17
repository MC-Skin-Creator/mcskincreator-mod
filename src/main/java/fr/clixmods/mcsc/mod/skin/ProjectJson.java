/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.skin;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import fr.clixmods.mcsc.mod.project.Layer;
import fr.clixmods.mcsc.mod.project.SkinProject;

/**
 * The project body sent to {@code POST /api/v1/textures}.
 *
 * <p>Two shapes: a single preset layer, which is what previewing one element of the
 * library amounts to, and the whole stack. The stack is written bottom to top, the
 * order the server composes in, and a hidden layer is simply left out - the server
 * has no use for a layer that contributes nothing, and sending it would only be a
 * larger body for the same picture.
 *
 * <p>The server's {@code ProjectValidator} is the authority on this format. It is
 * written here once, in one method, so that following the validator is an edit in one
 * place rather than a hunt through the screens.
 */
public final class ProjectJson {
    private static final Gson GSON = new Gson();

    private ProjectJson() {
    }

    /**
     * The whole stack, bottom to top.
     *
     * <p>Fields the layer has not moved from their defaults are left out: the
     * validator fills them in, and a body carrying only what was actually changed is
     * the one that stays readable when something is refused by path.
     */
    public static String stack(SkinProject project) {
        JsonArray layers = new JsonArray();
        for (Layer layer : project.layers()) {
            if (!layer.visible()) {
                continue;
            }
            JsonObject entry = new JsonObject();
            entry.addProperty("kind", "preset");
            entry.addProperty("category", layer.categoryId());
            entry.addProperty("item", layer.itemId());
            if (layer.opacity() != 100) {
                entry.addProperty("opacity", layer.opacity());
            }
            if (!layer.isUnadjusted()) {
                JsonObject adjustments = new JsonObject();
                adjustments.addProperty("hue", layer.hue());
                adjustments.addProperty("saturation", layer.saturation());
                adjustments.addProperty("brightness", layer.brightness());
                entry.add("adj", adjustments);
            }
            layers.add(entry);
        }

        JsonObject body = new JsonObject();
        body.addProperty("model", project.isSlim() ? "slim" : "classic");
        body.add("layers", layers);
        return GSON.toJson(body);
    }

    /** A project holding nothing but {@code itemId} of {@code categoryId}. */
    public static String singlePreset(String categoryId, String itemId, boolean slim) {
        JsonObject layer = new JsonObject();
        layer.addProperty("kind", "preset");
        layer.addProperty("category", categoryId);
        layer.addProperty("item", itemId);

        JsonArray layers = new JsonArray();
        layers.add(layer);

        JsonObject project = new JsonObject();
        project.addProperty("model", slim ? "slim" : "classic");
        project.add("layers", layers);
        return GSON.toJson(project);
    }
}
