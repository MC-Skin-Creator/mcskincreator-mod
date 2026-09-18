/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.catalog;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Where an element came from: almost none of the catalogue is an original drawing,
 * most of it is a piece cut out of somebody's skin, and that somebody is named here.
 *
 * <p>The catalogue publishes the four fields under French names — {@code titre},
 * {@code auteur}, {@code licence}, {@code url} — because they are copied straight out
 * of the files the site's workshop writes. They are part of the {@code /api/v1}
 * contract under those names and cannot be renamed there, so this is the one place in
 * the mod that reads them; everything above it is English like the rest.
 *
 * <p>A missing work is not a fault. The site says "provenance not documented" and so
 * does the mod: an undocumented origin is an open question, and showing it is what
 * keeps it visible.
 *
 * @param licence the licence <em>key</em> the catalogue uses ({@code cc-by}, {@code maison}…),
 *                which is a name to show, not a sentence of terms
 */
public record CatalogWork(String title, String author, String licence, String url) {
    public static final CatalogWork NONE = new CatalogWork("", "", "", "");

    /** The work drawn by the repository itself, which credits nobody outside it. */
    public static final String IN_HOUSE = "maison";

    public boolean isEmpty() {
        return this.title.isBlank() && this.author.isBlank()
                && this.licence.isBlank() && this.url.isBlank();
    }

    public boolean hasUrl() {
        return !this.url.isBlank();
    }

    /** Reads one entry of the catalogue's {@code credits} table. */
    public static CatalogWork of(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return NONE;
        }
        JsonObject json = element.getAsJsonObject();
        return new CatalogWork(string(json, "titre"), string(json, "auteur"),
                string(json, "licence"), string(json, "url"));
    }

    private static String string(JsonObject json, String key) {
        JsonElement value = json.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                ? value.getAsString()
                : "";
    }
}
