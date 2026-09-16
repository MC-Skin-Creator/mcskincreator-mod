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
 * @param atlasHash the content hash naming the category's atlas; it changes whenever
 *                  the category's pixels change, which is what lets a downloaded
 *                  atlas be kept indefinitely
 */
public record CatalogCategory(
        String id,
        String region,
        CatalogText name,
        boolean single,
        String atlasHash,
        List<CatalogItem> items) {

    public CatalogCategory {
        items = List.copyOf(items);
    }
}
