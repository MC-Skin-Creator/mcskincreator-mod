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
import java.util.Map;
import java.util.Optional;

/**
 * The library the server serves, reduced to what the screen browses: regions, and
 * the categories inside them.
 *
 * <p>Order is the catalogue's own: it is the order the site shows, and the mod has no
 * better one to offer.
 *
 * @param works the table of origins the catalogue publishes, by key. An element points
 *              at one of these rather than carrying it, because the same skin is cut
 *              into dozens of elements and is credited once.
 */
public record Catalog(List<CatalogCategory> categories, Map<String, CatalogWork> works) {
    public static final Catalog EMPTY = new Catalog(List.of(), Map.of());

    public Catalog {
        categories = List.copyOf(categories);
        works = Map.copyOf(works);
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

    /**
     * Where an element came from, as far as the catalogue alone can say.
     *
     * <p>Empty covers the two cases the interface says differently: an element that
     * names no work at all, and one naming a work the table does not hold — the second
     * is a broken link and the kind of thing the site has a test for.
     */
    public CatalogWork workOf(CatalogItem item) {
        if (item == null || item.credit().isBlank()) {
            return CatalogWork.NONE;
        }
        return this.works.getOrDefault(item.credit(), CatalogWork.NONE);
    }

    public Optional<CatalogCategory> category(String id) {
        return this.categories.stream().filter(category -> category.id().equals(id)).findFirst();
    }
}
