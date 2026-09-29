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
import java.util.Map;

import fr.clixmods.mcsc.mod.catalog.Catalog;
import fr.clixmods.mcsc.mod.catalog.CatalogCategory;
import fr.clixmods.mcsc.mod.catalog.CatalogItem;
import fr.clixmods.mcsc.mod.catalog.CatalogModel;
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

    /** A catalogue holding nothing but these categories: no credits, no ready-made stacks. */
    public static Catalog catalog(CatalogCategory... categories) {
        return new Catalog(List.of(categories), Map.of(), List.of(), List.of());
    }

    /**
     * A ready-made stack. Each piece is written {@code "category/item"}, bottom first,
     * which keeps a test's expectation readable next to the stack it should produce.
     */
    public static CatalogModel readyMade(String id, CatalogModel.Kind kind, boolean slim, String... pieces) {
        List<CatalogModel.Piece> parts = new ArrayList<>(pieces.length);
        for (String piece : pieces) {
            String[] split = piece.split("/", 2);
            parts.add(new CatalogModel.Piece(split[0], split[1]));
        }
        return new CatalogModel(id, new CatalogText(id, id, id), kind, slim, parts);
    }

    private static CatalogCategory category(String id, String region, boolean single, String... itemIds) {
        List<CatalogItem> items = new ArrayList<>(itemIds.length);
        for (String itemId : itemIds) {
            items.add(item(itemId));
        }
        return new CatalogCategory(id, region, new CatalogText(id, id, id), single, ThumbCrop.ALL, "", items);
    }
}
