/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fr.clixmods.mcsc.mod.catalog.Catalog;
import fr.clixmods.mcsc.mod.catalog.CatalogCategory;
import fr.clixmods.mcsc.mod.catalog.CatalogItem;
import fr.clixmods.mcsc.mod.catalog.CatalogModel;
import net.minecraft.world.entity.player.PlayerModelType;

/**
 * The project being edited: an ordered stack of layers and the model they are cut
 * for.
 *
 * <p>The list runs bottom to top, the order the sheet is composed in. The layers
 * panel shows it upside down — topmost layer at the top of the screen — which is a
 * display concern and stays in the panel.
 *
 * <p>Nothing here draws, and nothing here writes to the screen. The state changes
 * and the display follows on the next frame, through {@link #revision()}: a mutation
 * that forgot to bump the revision would leave the preview one edit behind with no
 * error anywhere, so every mutation goes through {@link #touch()}.
 */
public final class SkinProject {
    /** The server refuses a project past 300 layers, so the mod never builds one. */
    public static final int MAX_LAYERS = 300;

    /** The skin a new project starts from: plain, shade A. */
    public static final String DEFAULT_SKIN = "skin-uni";

    private final List<Layer> layers = new ArrayList<>();
    private PlayerModelType model = PlayerModelType.WIDE;
    private Layer selected;
    private int revision;

    /** Bottom to top. */
    public List<Layer> layers() {
        return Collections.unmodifiableList(this.layers);
    }

    public PlayerModelType model() {
        return this.model;
    }

    public boolean isSlim() {
        return this.model == PlayerModelType.SLIM;
    }

    public void setModel(PlayerModelType model) {
        this.model = model;
        touch();
    }

    public Layer selected() {
        return this.selected;
    }

    public void select(Layer layer) {
        this.selected = layer;
    }

    public boolean isEmpty() {
        return this.layers.isEmpty();
    }

    /** The regions holding something, in the order the catalogue lists them. */
    public List<String> regionsInUse(List<String> catalogOrder) {
        List<String> used = new ArrayList<>();
        for (String region : catalogOrder) {
            if (!displayOrder(region).isEmpty()) {
                used.add(region);
            }
        }
        // A region the catalogue no longer lists can still be in an open project; it
        // is shown after the known ones rather than silently dropped from the stack.
        for (Layer layer : this.layers) {
            if (!used.contains(layer.region())) {
                used.add(layer.region());
            }
        }
        return used;
    }

    /** The layers of one region, topmost first — the order the panel lists them in. */
    public List<Layer> displayOrder(String region) {
        List<Layer> ofRegion = new ArrayList<>(this.layers.stream()
                .filter(layer -> layer.region().equals(region))
                .toList());
        Collections.reverse(ofRegion);
        return ofRegion;
    }

    /**
     * Stacks an element.
     *
     * <p>A category marked single holds one layer at a time: choosing a skin replaces
     * the skin already there and stays at the bottom of the stack, rather than
     * burying the body under a second one.
     *
     * @return the layer that ended up in the stack, or null when it was full
     */
    public Layer add(CatalogCategory category, CatalogItem item, String languageCode) {
        Layer layer = new Layer(category, item, languageCode);
        if (category.single()) {
            this.layers.removeIf(existing -> existing.categoryId().equals(category.id()));
            this.layers.add(0, layer);
        } else {
            if (this.layers.size() >= MAX_LAYERS) {
                return null;
            }
            this.layers.add(layer);
        }
        this.selected = layer;
        touch();
        return layer;
    }

    /**
     * Applies one of the catalogue's ready-made stacks.
     *
     * <p>The two kinds do deliberately different things, as they do on the site. A
     * <strong>model</strong> is a finished character: it replaces the stack and brings
     * its own player model with it, because it was drawn for one of the two and its
     * arms are that width. An <strong>outfit</strong> is clothes: it stacks on the
     * body already there, leaving the skin, the hair and the face alone — which is why
     * the catalogue gives no outfit a {@code single} category.
     *
     * <p>Either way this is <em>one</em> change: the caller records a single history
     * entry around it, so a model that stacked eleven elements is taken back off by
     * one undo rather than eleven.
     *
     * @param catalog the catalogue the pieces are named in; a piece naming an element
     *                it no longer has is skipped rather than refused, the way the
     *                site's own reader skips it
     * @return how many pieces actually made it onto the stack
     */
    public int apply(CatalogModel model, Catalog catalog, String languageCode) {
        if (model.kind() == CatalogModel.Kind.MODEL) {
            this.layers.clear();
            this.selected = null;
            this.model = model.slim() ? PlayerModelType.SLIM : PlayerModelType.WIDE;
        }

        int stacked = 0;
        for (CatalogModel.Piece piece : model.pieces()) {
            CatalogCategory category = catalog.category(piece.categoryId()).orElse(null);
            if (category == null) {
                continue;
            }
            CatalogItem item = category.items().stream()
                    .filter(candidate -> candidate.id().equals(piece.itemId()))
                    .findFirst()
                    .orElse(null);
            if (item != null && add(category, item, languageCode) != null) {
                stacked++;
            }
        }
        touch();
        return stacked;
    }

    /**
     * Starts again from a bare body: the plain skin the catalogue calls
     * {@value #DEFAULT_SKIN}, on the wide model.
     *
     * <p>A new project is never an empty stack, because an empty stack is not a skin
     * anyone could wear. The element is named, which is fragile, so a catalogue that
     * has dropped it falls back to the first element of the first single category —
     * the skin category — rather than to nothing.
     */
    public void startFresh(Catalog catalog, String languageCode) {
        this.layers.clear();
        this.selected = null;
        this.model = PlayerModelType.WIDE;
        CatalogCategory fallback = null;
        for (CatalogCategory category : catalog.categories()) {
            if (!category.single() || category.items().isEmpty()) {
                continue;
            }
            if (fallback == null) {
                fallback = category;
            }
            for (CatalogItem item : category.items()) {
                if (item.id().equals(DEFAULT_SKIN)) {
                    add(category, item, languageCode);
                    this.selected = null;
                    return;
                }
            }
        }
        if (fallback != null) {
            add(fallback, fallback.items().get(0), languageCode);
            this.selected = null;
        }
        touch();
    }

    public void remove(Layer layer) {
        if (this.layers.remove(layer)) {
            if (this.selected == layer) {
                this.selected = null;
            }
            touch();
        }
    }

    /** Duplicates a layer straight above the one it was copied from. */
    public Layer duplicate(Layer layer) {
        int index = this.layers.indexOf(layer);
        if (index < 0 || this.layers.size() >= MAX_LAYERS) {
            return null;
        }
        Layer copy = layer.copy();
        this.layers.add(index + 1, copy);
        this.selected = copy;
        touch();
        return copy;
    }

    /**
     * Swaps a layer with its neighbour inside the same region.
     *
     * <p>Regions keep their order in the stack, so a hat can never end up below a
     * pair of trousers by dragging; {@code direction} is +1 towards the top of the
     * sheet, which is upwards on screen.
     */
    public boolean move(Layer layer, int direction) {
        int index = this.layers.indexOf(layer);
        if (index < 0) {
            return false;
        }
        for (int probe = index + direction; probe >= 0 && probe < this.layers.size(); probe += direction) {
            if (this.layers.get(probe).region().equals(layer.region())) {
                Collections.swap(this.layers, index, probe);
                touch();
                return true;
            }
        }
        return false;
    }

    public void clear() {
        this.layers.clear();
        this.selected = null;
        touch();
    }

    /**
     * Bumped by every mutation. Anything cached off the project — the composed
     * sheet, the preview texture — compares against it rather than recomputing
     * every frame.
     */
    public int revision() {
        return this.revision;
    }

    /** Call after changing a layer from outside, so the preview catches up. */
    public void touch() {
        this.revision++;
    }

    /** A detached copy of everything the history has to be able to put back. */
    public Snapshot snapshot() {
        List<Layer> copies = new ArrayList<>(this.layers.size());
        int selectedIndex = -1;
        for (int i = 0; i < this.layers.size(); i++) {
            copies.add(this.layers.get(i).copy());
            if (this.layers.get(i) == this.selected) {
                selectedIndex = i;
            }
        }
        return new Snapshot(copies, this.model, selectedIndex);
    }

    public void restore(Snapshot snapshot) {
        this.layers.clear();
        for (Layer layer : snapshot.layers()) {
            this.layers.add(layer.copy());
        }
        this.model = snapshot.model();
        this.selected = snapshot.selectedIndex() >= 0 && snapshot.selectedIndex() < this.layers.size()
                ? this.layers.get(snapshot.selectedIndex())
                : null;
        touch();
    }

    /** The topmost visible layer, which is what the preview falls back to. */
    public Layer topVisible() {
        for (int index = this.layers.size() - 1; index >= 0; index--) {
            if (this.layers.get(index).visible()) {
                return this.layers.get(index);
            }
        }
        return null;
    }

    /** An immutable copy of the whole project. */
    public record Snapshot(List<Layer> layers, PlayerModelType model, int selectedIndex) {
    }
}
