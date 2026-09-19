/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.skin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import fr.clixmods.mcsc.mod.catalog.Catalog;
import fr.clixmods.mcsc.mod.catalog.CatalogCategory;
import fr.clixmods.mcsc.mod.catalog.CatalogItem;
import fr.clixmods.mcsc.mod.catalog.CatalogModel;

/**
 * A ready-made stack turned back into the 64x64 skin it stands for.
 *
 * <p>Models and outfits are both lists of elements, and both need a picture of what
 * they would look like once stacked. That picture is composed here, out of the
 * category atlases already in memory, rather than asked for: the catalogue offers two
 * hundred and odd of them, and two hundred requests to fill one panel is not a thing
 * to do to the service or to somebody waiting on it.
 *
 * <p>The result is one buffer per entry, in the order given, so a
 * {@link CategorySprites} sheet can be built from them and every thumbnail in the
 * interface stays the same widget drawing the same kind of thing.
 */
public final class ReadyMadeSkins {

    private ReadyMadeSkins() {
    }

    /**
     * The skin each entry stands for.
     *
     * @param sprites the sheet of a category, or null while its atlas is on its way
     * @param under   a body to stack under every entry, or null for none. An outfit is
     *                clothes and nothing else, so without one its thumbnail is a pair
     *                of empty sleeves; a model carries its own skin and wants none.
     * @return one buffer per entry. An entry whose categories have not all arrived
     *         gets a transparent one rather than a half-dressed body — it is drawn
     *         again when they do.
     */
    public static List<byte[]> of(List<CatalogModel> entries, Catalog catalog,
                                  Function<String, CategorySprites> sprites, boolean slim,
                                  byte[] under) {
        List<byte[]> skins = new ArrayList<>(entries.size());
        for (CatalogModel entry : entries) {
            // A model was drawn for one of the two player models; an outfit is worn by
            // whichever body is already there.
            boolean posed = entry.kind() == CatalogModel.Kind.MODEL ? entry.slim() : slim;
            List<byte[]> pieces = pieces(entry, catalog, sprites, posed, under);
            skins.add(pieces == null ? new byte[Composite.BYTES] : Composite.of(pieces));
        }
        return skins;
    }

    /**
     * A plain body to stand the clothes on: the first element of the category that
     * holds the skin.
     *
     * <p>Taken from the catalogue rather than named outright. The site names one
     * (<code>skin-clair</code>) and the catalogue no longer carries it, which is
     * exactly the failure a hardcoded id gets you — a silently empty body.
     *
     * @return the buffer, or null when the skin category is not here yet
     */
    public static byte[] mannequin(Catalog catalog, Function<String, CategorySprites> sprites, boolean slim) {
        for (CatalogCategory category : catalog.categories()) {
            if (!category.single() || category.items().isEmpty()) {
                continue;
            }
            CategorySprites sheet = sprites.apply(category.id());
            CatalogItem first = category.items().get(0);
            return sheet == null ? null : sheet.buffer(first.atlasIndex(slim));
        }
        return null;
    }

    /** The pieces to stack, bottom first, or null when any of their pixels is missing. */
    private static List<byte[]> pieces(CatalogModel entry, Catalog catalog,
                                       Function<String, CategorySprites> sprites, boolean slim,
                                       byte[] under) {
        List<byte[]> buffers = new ArrayList<>(entry.pieces().size() + 1);
        if (under != null) {
            buffers.add(under);
        }
        for (CatalogModel.Piece piece : entry.pieces()) {
            CatalogCategory category = catalog.category(piece.categoryId()).orElse(null);
            if (category == null) {
                // A piece the catalogue has dropped is skipped, exactly as stacking it
                // would skip it.
                continue;
            }
            CategorySprites sheet = sprites.apply(piece.categoryId());
            if (sheet == null) {
                return null;   // still downloading: worth drawing again later
            }
            CatalogItem item = category.items().stream()
                    .filter(candidate -> candidate.id().equals(piece.itemId()))
                    .findFirst()
                    .orElse(null);
            if (item == null) {
                continue;
            }
            byte[] buffer = sheet.buffer(item.atlasIndex(slim));
            if (buffer == null) {
                return null;
            }
            buffers.add(buffer);
        }
        return buffers.isEmpty() ? null : buffers;
    }
}
