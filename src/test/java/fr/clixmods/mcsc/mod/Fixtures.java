/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod;

import java.util.ArrayList;
import java.util.List;

import fr.clixmods.mcsc.mod.catalog.CatalogCategory;
import fr.clixmods.mcsc.mod.catalog.CatalogItem;
import fr.clixmods.mcsc.mod.catalog.CatalogText;
import fr.clixmods.mcsc.mod.catalog.ThumbCrop;

/**
 * Catalogue entries built by hand, so a test about the layer stack does not have to
 * carry a catalogue payload with it.
 *
 * <p>The atlas ranks here are deliberately plain: what a parser makes of them is
 * {@code CatalogParserTest}'s business, not the stack's.
 */
public final class Fixtures {
    private Fixtures() {
    }

    /** An element whose three labels are all its id, so any language shows the id. */
    public static CatalogItem item(String id) {
        return new CatalogItem(id, new CatalogText(id, id, id), 0, CatalogItem.NONE, null, "");
    }

    /** A category that stacks: every element added to it adds a layer. */
    public static CatalogCategory category(String id, String region, String... itemIds) {
        return category(id, region, false, itemIds);
    }

    /** A category that holds one layer at a time, the way the skin does. */
    public static CatalogCategory single(String id, String region, String... itemIds) {
        return category(id, region, true, itemIds);
    }

    private static CatalogCategory category(String id, String region, boolean single, String... itemIds) {
        List<CatalogItem> items = new ArrayList<>(itemIds.length);
        for (String itemId : itemIds) {
            items.add(item(itemId));
        }
        return new CatalogCategory(id, region, new CatalogText(id, id, id), single, ThumbCrop.ALL, "", items);
    }
}
