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
import fr.clixmods.mcsc.mod.style.Surface;
import fr.clixmods.mcsc.mod.ui.Canvas;
import fr.clixmods.mcsc.mod.ui.Element;
import fr.clixmods.mcsc.mod.ui.Marquee;
import fr.clixmods.mcsc.mod.ui.Paint;
import net.minecraft.network.chat.Component;

/**
 * One line of the layer stack: a slot holding a grip, an eye, a thumbnail, the name
 * with its subtitle, and the actions.
 *
 * <p>The actions are not merely invisible when the row is at rest — they are out of
 * reach. Leaving them clickable is how someone deletes a layer by clicking a cross
 * they never saw. They come back when the row is pointed at, when it has the focus,
 * and when it is the selected layer.
 *
 * <p>Selection changes the material, not the border: a dark green fill, a light green
 * name. A hidden layer drops to half opacity, and a row being dragged takes a shadow
 * and a gold band down its left flank.
 */
public class LayerRow extends Element {
    private static final int BAND = Metrics.ui(6);
    /** Six dots of two pixels: the grip, drawn rather than typed. */
    private static final int GRIP_DOT = 2;

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
                    Palette.withAlpha(Palette.OUTLINE, 140));
        }

        Surface.slot(canvas, this.x, this.y, this.width, this.height,
                selected ? Palette.SELECTED_FILL : hot ? Palette.SLOT_HOVER : Palette.SLOT);
        if (selected) {
            Surface.bevel(canvas, this.x, this.y, this.width, this.height,
                    Palette.GREEN_BOTTOM, Palette.GREEN_LIGHT, Palette.PANEL_MID, Metrics.BEVEL);
        }
        if (this.dragging) {
            canvas.fill(this.x + Metrics.OUTLINE, this.y + Metrics.OUTLINE,
                    BAND, this.height - Metrics.OUTLINE * 2, Palette.GOLD);
        }

        int cursorX = this.x + Metrics.OUTLINE + Metrics.PAD_TIGHT;
        drawGrip(canvas, cursorX, this.y + this.height / 2 - GRIP_DOT * 2, lit);
        cursorX += GRIP_DOT * 2 + Metrics.PAD_TIGHT;

        drawEye(canvas, cursorX, this.y + (this.height - Metrics.ui(9)) / 2);
        cursorX += Metrics.ui(9) + Metrics.PAD_TIGHT;

        int preview = Metrics.LAYER_PREVIEW;
        Thumbnail.draw(canvas, this.sprites.get(), this.layer.atlasIndex(this.slim.get()),
                ThumbCrop.ALL, cursorX, this.y + (this.height - preview) / 2, preview, preview);
        cursorX += preview + Metrics.PAD_TIGHT;

        int actionsWidth = lit ? actionWidth() * 2 + Metrics.PAD_TIGHT : 0;
        int room = this.x + this.width - Metrics.OUTLINE - Metrics.PAD_TIGHT - actionsWidth - cursorX;

        int nameY = this.y + Metrics.OUTLINE + 1;
        this.marquee.draw(paint, this.layer.name(), cursorX, nameY, room,
                selected ? Palette.GREEN_LIGHT : hot ? Palette.GOLD : Palette.INK, hot);
        canvas.textFlat(subtitle(), cursorX, nameY + canvas.lineHeight() + 1, Palette.INK_FAINT);

        if (lit) {
            int actionX = this.x + this.width - Metrics.OUTLINE - Metrics.PAD_TIGHT - actionWidth();
            drawAction(canvas, paint, actionX, "x", Palette.RED_INK);
            drawAction(canvas, paint, actionX - actionWidth() - Metrics.PAD_TIGHT, "+", Palette.INK_MUTED);
        }

        if (!this.layer.visible()) {
            // Half opacity, laid over the finished row in the panel's own colour.
            canvas.fill(this.x, this.y, this.width, this.height,
                    Palette.withAlpha(Palette.PANEL, 128));
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
        return Metrics.ui(14);
    }

    private void drawGrip(Canvas canvas, int x, int y, boolean lit) {
        int ink = lit ? Palette.INK_DIM : Palette.INK_FAINT;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 2; column++) {
                canvas.fill(x + column * (GRIP_DOT + 1), y + row * (GRIP_DOT + 1),
                        GRIP_DOT, GRIP_DOT, ink);
            }
        }
    }

    private void drawEye(Canvas canvas, int x, int y) {
        int size = Metrics.ui(9);
        int ink = this.layer.visible() ? Palette.INK : Palette.INK_FAINT;
        canvas.fill(x, y + size / 2 - 1, size, 1, ink);
        canvas.fill(x + 1, y + size / 2 - 2, size - 2, 1, ink);
        canvas.fill(x + 1, y + size / 2, size - 2, 1, ink);
        if (this.layer.visible()) {
            canvas.fill(x + size / 2 - 1, y + size / 2 - 2, 2, 3, Palette.GREEN_LIGHT);
        }
    }

    private void drawAction(Canvas canvas, Paint paint, int x, String glyph, int ink) {
        int size = actionWidth();
        int y = this.y + (this.height - size) / 2;
        boolean over = paint.over(x, y, size, size);
        canvas.textCentered(Component.literal(glyph), x + size / 2,
                y + (size - paint.canvas().lineHeight()) / 2, over ? Palette.GOLD : ink);
    }

    @Override
    public boolean mouseDown(double mouseX, double mouseY, int button) {
        if (button != 0 || !contains(mouseX, mouseY)) {
            return false;
        }
        int cursorX = this.x + Metrics.OUTLINE + Metrics.PAD_TIGHT;

        if (mouseX < cursorX + GRIP_DOT * 2 + Metrics.PAD_TIGHT) {
            this.onSelect.accept(this.layer);
            this.onGrab.accept(this.layer);
            return true;
        }
        cursorX += GRIP_DOT * 2 + Metrics.PAD_TIGHT;
        if (mouseX < cursorX + Metrics.ui(9)) {
            this.onToggleVisible.accept(this.layer);
            return true;
        }

        // Reaching this point means the press landed inside the row, which is the
        // same condition that drew the actions a frame ago — so there is never a
        // target here without a glyph on it. A row that is not pointed at draws
        // nothing here and cannot be pressed here either, because the press would
        // not be inside it.
        int removeX = this.x + this.width - Metrics.OUTLINE - Metrics.PAD_TIGHT - actionWidth();
        int duplicateX = removeX - actionWidth() - Metrics.PAD_TIGHT;
        if (mouseX >= removeX) {
            this.onRemove.accept(this.layer);
            return true;
        }
        if (mouseX >= duplicateX) {
            this.onDuplicate.accept(this.layer);
            return true;
        }

        this.onSelect.accept(this.layer);
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
