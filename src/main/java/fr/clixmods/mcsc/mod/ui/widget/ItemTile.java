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
 *
 * <p>A tile with no {@code onInfo} draws no "i" at all: the catalogue credits elements,
 * one work each, and there is nothing to open for a set of them.
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

    /**
     * A tile is a square, and its height is therefore its width.
     *
     * <p>It used to be its picture plus its label plus the padding, which came to a
     * different number every time the column changed width and to a grid of rows that
     * were not the same height as each other. The site's tiles are one size, and a grid
     * whose cells are one size is the difference between a catalogue and a heap.
     */
    public static int heightFor(int tileWidth) {
        return tileWidth;
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
                - Metrics.THUMB_PAD - canvas.smallLineHeight();

        // The checker says "transparent here", so an element with holes does not read
        // as an element with black in it.
        Surface.checker(canvas, boxX, boxY, boxWidth, boxHeight);
        canvas.pushScissor(boxX, boxY, boxWidth, boxHeight);
        Thumbnail.draw(canvas, this.sprites.get(), this.item.atlasIndex(this.slim.get()),
                this.category.thumbCrop(this.item), boxX, boxY, boxWidth, boxHeight);
        canvas.popScissor();

        // The label at the half size, which is what lets a name be a name. At the full
        // size a tile this wide held five letters, and a column of "Cheve..." tells you
        // nothing at all about which hair is which.
        int labelY = boxY + boxHeight + Metrics.THUMB_PAD;
        this.marquee.drawSmall(paint, this.label, boxX, labelY, boxWidth,
                hot ? Palette.INK_HOVERED : Palette.INK_MUTED, hot);

        if (hot) {
            Surface.slotHighlight(canvas, this.x, this.y, this.width, this.height);
        }

        // Already in the stack: a small square, top right, in the site's lime.
        if (this.used.test(this.item)) {
            int markX = this.x + this.width - Metrics.SLOT_INSET - Metrics.THUMB_MARK;
            int markY = this.y + Metrics.SLOT_INSET;
            canvas.fill(markX - 1, markY - 1, Metrics.THUMB_MARK + 2, Metrics.THUMB_MARK + 2,
                    Palette.OUTLINE);
            canvas.fill(markX, markY, Metrics.THUMB_MARK, Metrics.THUMB_MARK, Palette.LIME);
        }

        // Where it came from: an "i", top left, and only while the tile is pointed at.
        // Drawn as a button rather than as a letter on a rectangle — it is pressable,
        // it does something different from the tile it sits on, and two hand-drawn
        // rectangles with a glyph in them said neither.
        if (hot && this.onInfo != null) {
            int[] mark = infoBounds();
            boolean over = paint.over(mark[0], mark[1], mark[2], mark[3]);
            Surface.button(canvas, mark[0], mark[1], mark[2], mark[3],
                    Surface.Tone.NEUTRAL, over, false);
            canvas.textSmall(Component.literal("i"),
                    mark[0] + (mark[2] - canvas.smallTextWidth("i")) / 2,
                    mark[1] + (mark[3] - canvas.smallLineHeight()) / 2,
                    over ? Palette.INK_HOVERED : Palette.INK);
        }
    }

    private int[] infoBounds() {
        int size = Metrics.INFO_MARK;
        return new int[] {
            this.x + Metrics.SLOT_INSET, this.y + Metrics.SLOT_INSET, size, size,
        };
    }

    @Override
    public boolean mouseDown(double mouseX, double mouseY, int button) {
        if (!contains(mouseX, mouseY)) {
            return false;
        }
        int[] mark = infoBounds();
        if (this.onInfo != null
                && mouseX >= mark[0] && mouseX < mark[0] + mark[2]
                && mouseY >= mark[1] && mouseY < mark[1] + mark[3]) {
            // The provenance mark stops the click. Without this, asking where an
            // element came from would also stack it.
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
