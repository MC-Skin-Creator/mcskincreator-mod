/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui.widget;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import fr.clixmods.mcsc.mod.catalog.CatalogCategory;
import fr.clixmods.mcsc.mod.skin.CategorySprites;
import fr.clixmods.mcsc.mod.style.Metrics;
import fr.clixmods.mcsc.mod.style.Surface;
import fr.clixmods.mcsc.mod.ui.Canvas;
import fr.clixmods.mcsc.mod.ui.Element;
import fr.clixmods.mcsc.mod.ui.Paint;
import net.minecraft.network.chat.Component;

/**
 * One category of the library: a slot showing what is inside it.
 *
 * <p>The site draws a little icon per category. The mod has none to draw and will not
 * invent one, so the tab shows the category's own first element instead — which is a
 * truer picture of what is in there than any icon, and costs nothing: the pixels are
 * already on the graphics card for the grid below.
 *
 * <p>There is no label. A row of names would not fit, and would have to be measured
 * again in every language; the name is in the tooltip, where the game puts it.
 *
 * <p>The tab row never scrolls and never folds: it is the first navigation of the
 * panel, and the old version of the site, which stacked every open category inside
 * the scroll instead, made going from hair to hats a trip past forty thumbnails.
 */
public class CategoryTab extends Element {
    private final CatalogCategory category;
    private final Component label;
    private final Supplier<CatalogCategory> current;
    private final Supplier<CategorySprites> sprites;
    private final Consumer<CatalogCategory> onPick;

    public CategoryTab(CatalogCategory category, Component label,
                       Supplier<CatalogCategory> current,
                       Supplier<CategorySprites> sprites,
                       Consumer<CatalogCategory> onPick) {
        this.category = category;
        this.label = label;
        this.current = current;
        this.sprites = sprites;
        this.onPick = onPick;
        this.width = Metrics.CATEGORY_TAB;
        this.height = Metrics.CATEGORY_TAB;
    }

    public CatalogCategory category() {
        return this.category;
    }

    @Override
    public void draw(Paint paint) {
        if (!visible()) {
            return;
        }
        Canvas canvas = paint.canvas();
        boolean hot = paint.hot(this);
        boolean chosen = this.category.equals(this.current.get());

        Surface.slot(canvas, this.x, this.y, this.width, this.height);

        // Cropped the way the category asks its own thumbnails to be cropped, so a
        // hair tab shows hair rather than eight rows of it over an empty figure.
        int inset = Metrics.SLOT_INSET;
        Thumbnail.draw(canvas, this.sprites.get(), firstIndex(), this.category.thumbCrop(),
                this.x + inset, this.y + inset,
                this.width - inset * 2, this.height - inset * 2);

        // The chosen tab and the one under the pointer both take the frame the game
        // lays over a slot, so neither needs a colour the game does not use.
        if (chosen || hot) {
            Surface.slotHighlight(canvas, this.x, this.y, this.width, this.height);
        }
    }

    private int firstIndex() {
        return this.category.items().isEmpty() ? -1 : this.category.items().get(0).atlasIndex();
    }

    @Override
    public boolean mouseDown(double mouseX, double mouseY, int button) {
        return button == 0 && contains(mouseX, mouseY) && activate();
    }

    @Override
    public boolean activate() {
        this.onPick.accept(this.category);
        return true;
    }

    @Override
    public List<Component> tooltip() {
        // The category's own name, from the catalogue. There is no second line: the
        // catalogue says what a category is called, not what it holds.
        return List.of(this.label);
    }
}
