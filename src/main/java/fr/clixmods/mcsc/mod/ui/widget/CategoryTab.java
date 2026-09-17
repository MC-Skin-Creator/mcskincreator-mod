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
import fr.clixmods.mcsc.mod.catalog.ThumbCrop;
import fr.clixmods.mcsc.mod.skin.CategorySprites;
import fr.clixmods.mcsc.mod.style.Metrics;
import fr.clixmods.mcsc.mod.style.Palette;
import fr.clixmods.mcsc.mod.style.Surface;
import fr.clixmods.mcsc.mod.ui.Canvas;
import fr.clixmods.mcsc.mod.ui.Element;
import fr.clixmods.mcsc.mod.ui.Icons;
import fr.clixmods.mcsc.mod.ui.Paint;
import net.minecraft.network.chat.Component;

/**
 * One category of the library: an inventory slot with the category's icon in it.
 *
 * <p>The icon is drawn at exactly twice its size, never at one and a half, which is
 * what fixes the tab at 38 pixels while everything else around it shrank with the
 * font. There is no label — the name is in the tooltip. A row of names would not fit
 * and, worse, would have to be re-measured in every language.
 *
 * <p>Chosen turns the slot green. The tab row itself never scrolls and never folds:
 * it is the first navigation of the panel, and the old version of the site, which
 * stacked every open category inside the scroll instead, made going from hair to hats
 * a trip past forty thumbnails.
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

        int fill = chosen ? Palette.GREEN : hot ? Palette.SLOT_HOVER : Palette.SLOT;
        Surface.slot(canvas, this.x, this.y, this.width, this.height, fill);

        int icon = Metrics.CATEGORY_ICON;
        int iconX = this.x + (this.width - icon) / 2;
        int iconY = this.y + (this.height - icon) / 2;

        // The catalogue names its categories but does not illustrate them, so the tab
        // looks for a drawing of its own id first. Failing that it shows the category's
        // first element, which is the truest picture of what is inside; and until that
        // category's sheet has arrived, a plain folder rather than an empty slot.
        if (Icons.has(this.category.id())) {
            Icons.draw(canvas, this.category.id(), iconX, iconY, 2);
        } else if (!Thumbnail.draw(canvas, this.sprites.get(), firstIndex(), ThumbCrop.ALL,
                iconX, iconY, icon, icon)) {
            Icons.draw(canvas, "folder", iconX, iconY, 2);
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
