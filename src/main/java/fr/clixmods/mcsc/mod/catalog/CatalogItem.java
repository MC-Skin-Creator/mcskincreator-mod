/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.catalog;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One element of the library: a single entry of a category.
 *
 * <p>The two atlas ranks are the element's own buffers inside its category's atlas.
 * An element drawn for the slim model carries a second buffer; one that looks the
 * same on both models carries only the first, and {@link #slimAtlasIndex} is
 * {@value #NONE}.
 *
 * @param atlasIndex     the rank of the element's classic-model buffer
 * @param slimAtlasIndex the rank of its slim-model buffer, or {@value #NONE}
 * @param thumbCrop      the part of the body its thumbnail shows, when the element
 *                       overrides what its category asks for
 * @param credit         the key of the work this element was cut out of, in the
 *                       catalogue's own table, or empty when it names none
 * @param colors         the element's colour keys and the colour each one is drawn
 *                       in, as {@code 0xRRGGBB}, in the catalogue's order; a layer can
 *                       recolour these keys and no others
 * @param colorMap       which key each pixel of the classic buffer belongs to, as the
 *                       catalogue's run-length string, or null when it gives none —
 *                       the engine then puts every opaque pixel under the first key
 * @param slimColorMap   the same for the slim buffer, or null
 */
public record CatalogItem(
        String id,
        CatalogText name,
        int atlasIndex,
        int slimAtlasIndex,
        ThumbCrop thumbCrop,
        String credit,
        Map<String, Integer> colors,
        String colorMap,
        String slimColorMap) {

    public CatalogItem {
        // Insertion order is the catalogue's, and the order the swatches are shown in.
        colors = Collections.unmodifiableMap(new LinkedHashMap<>(colors));
    }

    /** An element with no colour key: nothing of it can be recoloured. */
    public CatalogItem(String id, CatalogText name, int atlasIndex, int slimAtlasIndex,
                       ThumbCrop thumbCrop, String credit) {
        this(id, name, atlasIndex, slimAtlasIndex, thumbCrop, credit, Map.of());
    }

    /** An element with colour keys and no zone map of its own. */
    public CatalogItem(String id, CatalogText name, int atlasIndex, int slimAtlasIndex,
                       ThumbCrop thumbCrop, String credit, Map<String, Integer> colors) {
        this(id, name, atlasIndex, slimAtlasIndex, thumbCrop, credit, colors, null, null);
    }

    /** No slim buffer: the element is drawn the same on both models. */
    public static final int NONE = -1;

    public boolean hasSlim() {
        return this.slimAtlasIndex != NONE;
    }

    /** The buffer to read for one of the two models, falling back to the classic one. */
    public int atlasIndex(boolean slim) {
        return slim && this.hasSlim() ? this.slimAtlasIndex : this.atlasIndex;
    }
}
