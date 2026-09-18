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
 * A ready-made stack the catalogue offers: a whole character, or a set of clothes.
 *
 * <p>The site serves these beside the elements, under {@code projects} and
 * {@code outfits}, and treats them differently — which is the only reason they share
 * a class rather than being one list. A <strong>model</strong> is a finished
 * character and <em>replaces</em> what is being edited, down to which of the two
 * player models it was drawn for. An <strong>outfit</strong> is clothes and
 * <em>stacks</em> on whoever is wearing them, leaving the body, the hair and the face
 * where they were.
 *
 * @param slim   the player model a model was drawn for; always false on an outfit,
 *               which is worn by whichever body is already there
 * @param pieces the elements to stack, bottom first — the order the catalogue lists
 *               them in, which is the order they compose in
 */
public record CatalogModel(String id, CatalogText name, Kind kind, boolean slim, List<Piece> pieces) {

    public CatalogModel {
        pieces = List.copyOf(pieces);
    }

    /** Which of the two the entry is, and so what choosing it does to the stack. */
    public enum Kind {
        /** A whole character: replaces the stack. */
        MODEL,
        /** A set of clothes: stacks on top of what is there. */
        OUTFIT
    }

    /**
     * One element of a ready-made stack, named the way a project names it.
     *
     * <p>The catalogue also gives some pieces a {@code colors} override — the same
     * cloak in green rather than in red. The mod has no per-key colours on a layer
     * yet, so those are left behind and the element is stacked in the colours it was
     * drawn in. Carrying them into a layer that cannot hold them would only lose them
     * somewhere less visible.
     */
    public record Piece(String categoryId, String itemId) {
    }
}
