/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.catalog;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

/**
 * The library the server serves, reduced to what the screen browses: regions, and
 * the categories inside them.
 *
 * <p>Order is the catalogue's own: it is the order the site shows, and the mod has no
 * better one to offer.
 */
public record Catalog(List<CatalogCategory> categories) {
    public static final Catalog EMPTY = new Catalog(List.of());

    public Catalog {
        categories = List.copyOf(categories);
    }

    public boolean isEmpty() {
        return this.categories.isEmpty();
    }

    /** The regions, in catalogue order and without repeats. */
    public List<String> regions() {
        LinkedHashSet<String> regions = new LinkedHashSet<>();
        for (CatalogCategory category : this.categories) {
            regions.add(category.region());
        }
        return List.copyOf(regions);
    }

    public List<CatalogCategory> categoriesIn(String region) {
        List<CatalogCategory> found = new ArrayList<>();
        for (CatalogCategory category : this.categories) {
            if (category.region().equals(region)) {
                found.add(category);
            }
        }
        return List.copyOf(found);
    }

    public Optional<CatalogCategory> category(String id) {
        return this.categories.stream().filter(category -> category.id().equals(id)).findFirst();
    }
}
