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
 * @param id         the identifier the server knows the element by, and the one a
 *                   project layer references
 * @param name       the element's labels, as the catalogue carries them
 * @param atlasIndex the element's rank in its category, which is also the rank of
 *                   its 64x64 buffer inside the category's atlas
 */
public record CatalogItem(String id, CatalogText name, int atlasIndex) {
}
