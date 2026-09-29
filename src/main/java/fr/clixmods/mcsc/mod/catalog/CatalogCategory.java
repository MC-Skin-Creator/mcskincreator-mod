/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.catalog;

import java.util.List;

/**
 * A category of the library, as the site's left-hand panel groups them.
 *
 * @param region    the region the category belongs to, which is the first level the
 *                  screen browses
 * @param single    whether the category replaces its layer instead of stacking one,
 *                  the way the skin does
 * @param thumbCrop the part of the body its elements' thumbnails show, which an
 *                  element may override
 * @param atlasPath where the category's atlas is served, exactly as the catalogue
 *                  gives it. The catalogue's address carries the hash of the
 *                  category's pixels, so a downloaded atlas can be kept
 *                  indefinitely - and the mod follows the address it is handed
 *                  rather than building one from the hash, so that moving the route
 *                  is the server's business alone.
 */
public record CatalogCategory(
        String id,
        String region,
        CatalogText name,
        boolean single,
        ThumbCrop thumbCrop,
        String atlasPath,
        List<CatalogItem> items) {

    public CatalogCategory {
        items = List.copyOf(items);
    }

    /** The crop for one of this category's elements: its own, or the category's. */
    public ThumbCrop thumbCrop(CatalogItem item) {
        return item.thumbCrop() == null ? this.thumbCrop : item.thumbCrop();
    }
}
