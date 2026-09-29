/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.skin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 *
 * <p>An instance remembers what it blended, and from which buffers. The pictures are
 * asked for again every time an atlas lands, and opening the models window lands
 * dozens of them in a row; blending two hundred stacks again for each one is what made
 * that window crawl. A picture is blended again only when one of the buffers it is
 * made of is a different one — which is to say, never, once its atlases are here. Not
 * safe for two threads at once: give each sheet its own.
 */
public final class ReadyMadeSkins {
    private Map<CatalogModel, Blend> blends = new HashMap<>();

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
    public List<byte[]> of(List<CatalogModel> entries, Catalog catalog,
                           Function<String, CategorySprites> sprites, boolean slim,
                           byte[] under) {
        byte[] missing = new byte[Composite.BYTES];
        List<byte[]> skins = new ArrayList<>(entries.size());
        // Rebuilt rather than added to, so an entry the catalogue has dropped is
        // forgotten along with it.
        Map<CatalogModel, Blend> kept = new HashMap<>();
        for (CatalogModel entry : entries) {
            // A model was drawn for one of the two player models; an outfit is worn by
            // whichever body is already there.
            boolean posed = entry.kind() == CatalogModel.Kind.MODEL ? entry.slim() : slim;
            List<Stacked> pieces = pieces(entry, catalog, sprites, posed, under);
            if (pieces == null) {
                skins.add(missing);
                continue;
            }
            List<byte[]> sources = new ArrayList<>(pieces.size());
            for (Stacked piece : pieces) {
                sources.add(piece.source());
            }
            // Keyed on the atlas buffers rather than the recoloured ones: a recolour is
            // a fresh array every time, and it is itself part of what is worth sparing.
            Blend blend = this.blends.get(entry);
            if (blend == null || !blend.madeOf(sources)) {
                List<byte[]> stacked = new ArrayList<>(pieces.size());
                for (Stacked piece : pieces) {
                    stacked.add(piece.colored());
                }
                blend = new Blend(sources, Composite.of(stacked));
            }
            kept.put(entry, blend);
            skins.add(blend.skin());
        }
        this.blends = kept;
        return skins;
    }

    /** A picture, and the very buffers it was blended from. */
    private record Blend(List<byte[]> pieces, byte[] skin) {
        /**
         * By identity, not by content: an atlas buffer is never written to once sliced,
         * so the same array is the same pixels, and comparing the pixels would cost
         * most of what blending them again does.
         */
        boolean madeOf(List<byte[]> others) {
            if (others.size() != this.pieces.size()) {
                return false;
            }
            for (int index = 0; index < others.size(); index++) {
                if (others.get(index) != this.pieces.get(index)) {
                    return false;
                }
            }
            return true;
        }
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

    /**
     * One buffer to stack, as the atlas holds it, and the colour override to apply to
     * it — applied only when the picture is actually blended.
     */
    private record Stacked(byte[] source, String categoryId, CatalogItem item,
                           Map<String, Integer> colors, boolean slim) {
        static Stacked plain(byte[] source) {
            return new Stacked(source, null, null, Map.of(), false);
        }

        byte[] colored() {
            return this.colors.isEmpty() ? this.source
                    : Composite.recolor(this.categoryId, this.item, this.colors, this.slim, this.source);
        }
    }

    /** The pieces to stack, bottom first, or null when any of their pixels is missing. */
    private static List<Stacked> pieces(CatalogModel entry, Catalog catalog,
                                       Function<String, CategorySprites> sprites, boolean slim,
                                       byte[] under) {
        List<Stacked> buffers = new ArrayList<>(entry.pieces().size() + 1);
        if (under != null) {
            buffers.add(Stacked.plain(under));
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
            // The piece's colour override, so the picture is the one choosing it gives.
            buffers.add(new Stacked(buffer, piece.categoryId(), item, piece.colors(), slim));
        }
        return buffers.isEmpty() ? null : buffers;
    }
}
