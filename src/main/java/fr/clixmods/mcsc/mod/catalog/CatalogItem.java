/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.catalog;

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
 */
public record CatalogItem(
        String id,
        CatalogText name,
        int atlasIndex,
        int slimAtlasIndex,
        ThumbCrop thumbCrop) {

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
