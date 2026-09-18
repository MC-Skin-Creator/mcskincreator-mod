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
import java.util.function.Predicate;
import java.util.function.Supplier;

import fr.clixmods.mcsc.mod.catalog.CatalogCategory;
import fr.clixmods.mcsc.mod.catalog.CatalogItem;
import fr.clixmods.mcsc.mod.skin.CategorySprites;
import fr.clixmods.mcsc.mod.style.Metrics;
import fr.clixmods.mcsc.mod.style.Palette;
import fr.clixmods.mcsc.mod.style.Surface;
import fr.clixmods.mcsc.mod.ui.Canvas;
import fr.clixmods.mcsc.mod.ui.Element;
import fr.clixmods.mcsc.mod.ui.Marquee;
import fr.clixmods.mcsc.mod.ui.Paint;
import net.minecraft.network.chat.Component;

/**
 * An element of the catalogue, in an inventory slot: the most used brick of the whole
 * interface.
 *
 * <p>Two marks can appear on it and they take opposite corners on purpose. Already in
 * the stack is a small green square, top right. Where it came from is an "i", top
 * left, and only while the tile is pointed at or focused — and it swallows its own
 * click, because asking where something came from must not also add it to the stack.
 * Stacking two marks in one corner hides one of them, which the site did once.
 */
public class ItemTile extends Element {
    /** The green "already used" mark, 7 site pixels square. */
    private static final int USED_MARK = Metrics.ui(7);
    /** The provenance mark, 15 site pixels square. */
    private static final int INFO_MARK = Metrics.ui(15);

    private final CatalogCategory category;
    private final CatalogItem item;
    private final Component label;
    private final Supplier<CategorySprites> sprites;
    private final Supplier<Boolean> slim;
    private final Predicate<CatalogItem> used;
    private final Consumer<ItemTile> onPick;
    private final Consumer<ItemTile> onInfo;
    private final Consumer<ItemTile> onHover;
    private final Marquee marquee = new Marquee();

    private boolean wasHot;

    public ItemTile(CatalogCategory category, CatalogItem item, Component label,
                    Supplier<CategorySprites> sprites, Supplier<Boolean> slim,
                    Predicate<CatalogItem> used, Consumer<ItemTile> onPick,
                    Consumer<ItemTile> onInfo, Consumer<ItemTile> onHover) {
        this.category = category;
        this.item = item;
        this.label = label;
        this.sprites = sprites;
        this.slim = slim;
        this.used = used;
        this.onPick = onPick;
        this.onInfo = onInfo;
        this.onHover = onHover;
    }

    public CatalogCategory category() {
        return this.category;
    }

    public CatalogItem item() {
        return this.item;
    }

    public Component label() {
        return this.label;
    }

    /** The height a tile needs: its render area, its label, and the padding around both. */
    public static int heightFor(Canvas canvas, int renderHeight) {
        return Metrics.OUTLINE * 2 + Metrics.PAD_TIGHT * 2 + renderHeight + canvas.lineHeight();
    }

    @Override
    public void draw(Paint paint) {
        if (!visible()) {
            return;
        }
        Canvas canvas = paint.canvas();
        boolean hot = paint.hot(this);

        // Hovering shows the element on the model and leaving takes it straight back
        // off. The edge is tracked here so the panel is told once, not every frame.
        if (hot != this.wasHot) {
            this.wasHot = hot;
            this.onHover.accept(hot ? this : null);
        }

        Surface.slot(canvas, this.x, this.y, this.width, this.height,
                hot ? Palette.SLOT_HOVER : Palette.SLOT);

        int inset = Metrics.OUTLINE + Metrics.PAD_TIGHT;
        int boxX = this.x + inset;
        int boxY = this.y + inset;
        int boxWidth = this.width - inset * 2;
        int boxHeight = this.height - inset * 2 - canvas.lineHeight();

        // The checker says "transparent here", so an element with holes does not read
        // as an element with black in it.
        Surface.checker(canvas, boxX, boxY, boxWidth, boxHeight);
        Thumbnail.draw(canvas, this.sprites.get(), this.item.atlasIndex(this.slim.get()),
                this.category.thumbCrop(this.item), boxX, boxY, boxWidth, boxHeight);

        int labelY = boxY + boxHeight + Metrics.PAD_TIGHT;
        this.marquee.draw(paint, this.label, boxX, labelY, boxWidth,
                hot ? Palette.GOLD : Palette.INK_DIM, hot);

        if (this.used.test(this.item)) {
            int markX = this.x + this.width - Metrics.OUTLINE - USED_MARK - 1;
            int markY = this.y + Metrics.OUTLINE + 1;
            canvas.fill(markX - 1, markY - 1, USED_MARK + 2, USED_MARK + 2, Palette.OUTLINE);
            canvas.fill(markX, markY, USED_MARK, USED_MARK, Palette.GREEN_LIGHT);
        }

        if (hot) {
            int[] mark = infoBounds();
            canvas.fill(mark[0], mark[1], mark[2], mark[3], Palette.OUTLINE);
            canvas.fill(mark[0] + 1, mark[1] + 1, mark[2] - 2, mark[3] - 2, Palette.PANEL_SUB);
            canvas.textCentered(Component.literal("i"), mark[0] + mark[2] / 2,
                    mark[1] + (mark[3] - canvas.lineHeight()) / 2 + 1, Palette.GOLD);
        }
    }

    private int[] infoBounds() {
        return new int[] {
            this.x + Metrics.OUTLINE + 1, this.y + Metrics.OUTLINE + 1, INFO_MARK, INFO_MARK,
        };
    }

    @Override
    public boolean mouseDown(double mouseX, double mouseY, int button) {
        if (button != 0 || !contains(mouseX, mouseY)) {
            return false;
        }
        int[] mark = infoBounds();
        if (mouseX >= mark[0] && mouseX < mark[0] + mark[2]
                && mouseY >= mark[1] && mouseY < mark[1] + mark[3]) {
            // The provenance mark stops the click. Without this, asking where an
            // element came from would also stack it.
            this.onInfo.accept(this);
            return true;
        }
        return activate();
    }

    @Override
    public boolean activate() {
        this.onPick.accept(this);
        return true;
    }

    @Override
    public List<Component> tooltip() {
        return List.of(this.label);
    }
}
