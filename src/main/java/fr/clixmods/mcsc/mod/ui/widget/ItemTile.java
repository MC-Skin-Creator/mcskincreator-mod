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
 * <p>An element already in the stack keeps the frame the game lays over a slot, so
 * "this one is in play" is said the way the game says it rather than with a badge of
 * the mod's own. Where the element came from is on the right button, which is where
 * the game puts a second action — the site uses a little "i" that appears on hover,
 * and a mark that is only sometimes there is a mark that is sometimes missed.
 */
public class ItemTile extends Element {
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
        return Metrics.SLOT_INSET * 2 + Metrics.THUMB_PAD * 2 + renderHeight
                + Metrics.THUMB_PAD + canvas.lineHeight();
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

        Surface.slot(canvas, this.x, this.y, this.width, this.height);

        int inset = Metrics.SLOT_INSET + Metrics.THUMB_PAD;
        int boxX = this.x + inset;
        int boxY = this.y + inset;
        int boxWidth = this.width - inset * 2;
        int boxHeight = this.height - inset - Metrics.SLOT_INSET
                - Metrics.THUMB_PAD - canvas.lineHeight();

        // The checker says "transparent here", so an element with holes does not read
        // as an element with black in it.
        Surface.checker(canvas, boxX, boxY, boxWidth, boxHeight);
        Thumbnail.draw(canvas, this.sprites.get(), this.item.atlasIndex(this.slim.get()),
                this.category.thumbCrop(this.item), boxX, boxY, boxWidth, boxHeight);

        int labelY = boxY + boxHeight + Metrics.THUMB_PAD;
        this.marquee.draw(paint, this.label, boxX, labelY, boxWidth,
                hot ? Palette.INK_HOVERED : Palette.INK_MUTED, hot);

        if (hot || this.used.test(this.item)) {
            Surface.slotHighlight(canvas, this.x, this.y, this.width, this.height);
        }
    }

    @Override
    public boolean mouseDown(double mouseX, double mouseY, int button) {
        if (!contains(mouseX, mouseY)) {
            return false;
        }
        if (button == 1) {
            // The right button asks where the element came from. Keeping it off the
            // left one is the point: asking must never also stack the element.
            this.onInfo.accept(this);
            return true;
        }
        return button == 0 && activate();
    }

    @Override
    public boolean activate() {
        this.onPick.accept(this);
        return true;
    }

    @Override
    public List<Component> tooltip() {
        return List.of(this.label, Component.translatable("gui.mcskincreator.provenance_hint"));
    }
}
