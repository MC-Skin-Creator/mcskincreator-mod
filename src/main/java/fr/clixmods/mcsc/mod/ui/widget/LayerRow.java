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

import fr.clixmods.mcsc.mod.catalog.ThumbCrop;
import fr.clixmods.mcsc.mod.project.Layer;
import fr.clixmods.mcsc.mod.skin.CategorySprites;
import fr.clixmods.mcsc.mod.style.Metrics;
import fr.clixmods.mcsc.mod.style.Palette;
import fr.clixmods.mcsc.mod.style.Sprites;
import fr.clixmods.mcsc.mod.style.Surface;
import fr.clixmods.mcsc.mod.ui.Canvas;
import fr.clixmods.mcsc.mod.ui.Element;
import fr.clixmods.mcsc.mod.ui.Marquee;
import fr.clixmods.mcsc.mod.ui.Paint;
import net.minecraft.network.chat.Component;

/**
 * One line of the layer stack: a slot holding a visibility box, a thumbnail, the name
 * with its subtitle, and the actions.
 *
 * <p>The actions are not merely invisible when the row is at rest — they are out of
 * reach. Leaving them clickable is how someone deletes a layer by clicking a cross
 * they never saw. They come back when the row is pointed at, when it has the focus,
 * and when it is the selected layer.
 *
 * <p>The selected row is the game's selected tab, which is what a Minecraft menu uses
 * to say "this one of several"; the site's green fill would be a colour the game does
 * not speak. A hidden layer drops to half opacity, and a row being dragged takes a
 * shadow and a band down its left flank.
 */
public class LayerRow extends Element {
    private static final int BAND = Metrics.ui(6);

    private final Layer layer;
    private final Supplier<CategorySprites> sprites;
    private final Supplier<Boolean> slim;
    private final java.util.function.Supplier<Layer> selection;
    private final Consumer<Layer> onSelect;
    private final Consumer<Layer> onToggleVisible;
    private final Consumer<Layer> onDuplicate;
    private final Consumer<Layer> onRemove;
    private final Consumer<Layer> onGrab;
    private final Consumer<Layer> onPeek;
    private final Marquee marquee = new Marquee();

    private boolean dragging;
    private boolean wasHot;

    public LayerRow(Layer layer, Supplier<CategorySprites> sprites, Supplier<Boolean> slim,
                    java.util.function.Supplier<Layer> selection,
                    Consumer<Layer> onSelect, Consumer<Layer> onToggleVisible,
                    Consumer<Layer> onDuplicate, Consumer<Layer> onRemove,
                    Consumer<Layer> onGrab, Consumer<Layer> onPeek) {
        this.layer = layer;
        this.sprites = sprites;
        this.slim = slim;
        this.selection = selection;
        this.onSelect = onSelect;
        this.onToggleVisible = onToggleVisible;
        this.onDuplicate = onDuplicate;
        this.onRemove = onRemove;
        this.onGrab = onGrab;
        this.onPeek = onPeek;
        this.height = Metrics.LAYER_ROW;
    }

    public Layer layer() {
        return this.layer;
    }

    public void setDragging(boolean dragging) {
        this.dragging = dragging;
    }

    @Override
    public void draw(Paint paint) {
        if (!visible()) {
            return;
        }
        Canvas canvas = paint.canvas();
        boolean selected = this.selection.get() == this.layer;
        boolean hot = paint.hot(this);
        boolean lit = hot || selected;

        // Pointing at a row makes the layer blink on the model, so you can tell which
        // of three brown layers is the one you are about to change.
        if (hot != this.wasHot) {
            this.wasHot = hot;
            this.onPeek.accept(hot ? this.layer : null);
        }

        if (this.dragging) {
            canvas.fill(this.x + Metrics.ui(4), this.y + Metrics.ui(4), this.width, this.height,
                    Palette.SHADOW);
        }

        if (selected) {
            Surface.tab(canvas, this.x, this.y, this.width, this.height, true, hot);
        } else {
            Surface.slot(canvas, this.x, this.y, this.width, this.height);
            if (hot) {
                Surface.slotHighlight(canvas, this.x, this.y, this.width, this.height);
            }
        }
        if (this.dragging) {
            canvas.fill(this.x + Metrics.SLOT_INSET / 2, this.y + Metrics.SLOT_INSET / 2,
                    BAND, this.height - Metrics.SLOT_INSET, Palette.INK_HOVERED);
        }

        int cursorX = this.x + Metrics.SLOT_INSET;

        // Visibility is a yes or a no, so it is the game's checkbox rather than an eye
        // the game has no sprite for.
        Surface.checkbox(canvas, cursorX, this.y + (this.height - Sprites.CHECKBOX_SIZE) / 2,
                this.layer.visible(), paint.over(cursorX, this.y, Sprites.CHECKBOX_SIZE, this.height));
        cursorX += Sprites.CHECKBOX_SIZE + Metrics.PAD_TIGHT;

        int preview = Metrics.LAYER_PREVIEW;
        Thumbnail.draw(canvas, this.sprites.get(), this.layer.atlasIndex(this.slim.get()),
                ThumbCrop.ALL, cursorX, this.y + (this.height - preview) / 2, preview, preview);
        cursorX += preview + Metrics.PAD_TIGHT;

        int actionsWidth = lit ? actionWidth() * 2 + Metrics.PAD_TIGHT : 0;
        int room = this.x + this.width - Metrics.SLOT_INSET - actionsWidth - cursorX;

        int nameY = this.y + Metrics.SLOT_INSET / 2 + 1;
        this.marquee.draw(paint, this.layer.name(), cursorX, nameY, room,
                hot ? Palette.INK_HOVERED : Palette.INK, hot);
        canvas.textFlat(subtitle(), cursorX, nameY + canvas.lineHeight() + 1, Palette.INK_FAINT);

        if (lit) {
            int actionX = this.x + this.width - Metrics.SLOT_INSET - actionWidth();
            drawCross(canvas, paint, actionX, this.y + (this.height - Sprites.CROSS_SIZE) / 2);
            drawAction(canvas, paint, actionX - actionWidth() - Metrics.PAD_TIGHT, "+",
                    Palette.INK_MUTED);
        }

        if (!this.layer.visible()) {
            // Half opacity, laid over the finished row: the game dims what is off
            // rather than recolouring it.
            canvas.fill(this.x, this.y, this.width, this.height,
                    Palette.withAlpha(0xFF000000, 128));
        }
    }

    /** Category, and the opacity when it is not full — nothing else fits, or needs to. */
    private Component subtitle() {
        if (this.layer.opacity() >= 100) {
            return this.layer.categoryName();
        }
        return Component.translatable("layer.mcskincreator.subtitle",
                this.layer.categoryName(), this.layer.opacity());
    }

    private static int actionWidth() {
        return Sprites.CROSS_SIZE;
    }

    /** Removing a layer is the game's close cross, which is what it means everywhere. */
    private void drawCross(Canvas canvas, Paint paint, int x, int y) {
        boolean over = paint.over(x, y, Sprites.CROSS_SIZE, Sprites.CROSS_SIZE);
        canvas.sprite(over ? Sprites.CROSS_HOVERED : Sprites.CROSS,
                x, y, Sprites.CROSS_SIZE, Sprites.CROSS_SIZE);
    }

    private void drawAction(Canvas canvas, Paint paint, int x, String glyph, int ink) {
        int size = actionWidth();
        int y = this.y + (this.height - size) / 2;
        boolean over = paint.over(x, y, size, size);
        canvas.textCentered(Component.literal(glyph), x + size / 2,
                y + (size - paint.canvas().lineHeight()) / 2,
                over ? Palette.INK_HOVERED : ink);
    }

    @Override
    public boolean mouseDown(double mouseX, double mouseY, int button) {
        if (button != 0 || !contains(mouseX, mouseY)) {
            return false;
        }
        int cursorX = this.x + Metrics.SLOT_INSET;
        if (mouseX < cursorX + Sprites.CHECKBOX_SIZE) {
            this.onToggleVisible.accept(this.layer);
            return true;
        }

        // Reaching this point means the press landed inside the row, which is the
        // same condition that drew the actions a frame ago — so there is never a
        // target here without a glyph on it. A row that is not pointed at draws
        // nothing here and cannot be pressed here either, because the press would
        // not be inside it.
        int removeX = this.x + this.width - Metrics.SLOT_INSET - actionWidth();
        int duplicateX = removeX - actionWidth() - Metrics.PAD_TIGHT;
        if (mouseX >= removeX) {
            this.onRemove.accept(this.layer);
            return true;
        }
        if (mouseX >= duplicateX) {
            this.onDuplicate.accept(this.layer);
            return true;
        }

        // Anywhere else on the row selects it and begins a drag. The site puts a grip
        // on the left for that; the game draws no grip, and a row that moves when you
        // pull it needs no handle to say so.
        this.onSelect.accept(this.layer);
        this.onGrab.accept(this.layer);
        return true;
    }

    @Override
    public boolean activate() {
        this.onSelect.accept(this.layer);
        return true;
    }

    @Override
    public List<Component> tooltip() {
        return List.of(this.layer.name());
    }
}
