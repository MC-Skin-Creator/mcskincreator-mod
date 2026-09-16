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

/**
 * The project body sent to {@code POST /api/v1/textures}.
 *
 * <p>Only the one shape this milestone needs: a project of a single preset layer,
 * which is what previewing one element of the library amounts to. The stack - several
 * layers, ordering, opacity, visibility, the {@code single} categories that replace
 * instead of stacking - is issue #6, and the colour and HSL adjustments are issue #7.
 *
 * <p>The server's {@code ProjectValidator} is the authority on this format. It is
 * written here once, in one method, so that following the validator is an edit in one
 * place rather than a hunt through the screens.
 */
public final class ProjectJson {
    private static final Gson GSON = new Gson();

    private ProjectJson() {
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
