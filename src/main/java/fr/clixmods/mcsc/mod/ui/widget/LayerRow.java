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
    /**
     * Below this, the name is an ellipsis and the row stops being worth reading.
     *
     * <p>Wide enough for a short word and not for a long one, which is the honest
     * threshold: "Bandana" is 42 pixels, and a row that can show it in full is a row
     * worth giving the picture up for.
     */
    private static final int MIN_NAME_ROOM = 45;

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

        // Selected is a tinted slot with a green band down its flank, not a green row:
        // a whole row of the accent colour shouts down the four that are not selected,
        // and the name on it stops being the thing you read first.
        if (selected) {
            Surface.slot(canvas, this.x, this.y, this.width, this.height, Palette.GREEN_FILL);
            canvas.fill(this.x + Metrics.OUTLINE, this.y + Metrics.OUTLINE,
                    Metrics.BAND, this.height - Metrics.OUTLINE * 2, Palette.GREEN);
        } else {
            Surface.slot(canvas, this.x, this.y, this.width, this.height,
                    hot ? Palette.SLOT_HOVER : Palette.SLOT);
        }
        if (this.dragging) {
            canvas.fill(this.x + Metrics.SLOT_INSET / 2, this.y + Metrics.SLOT_INSET / 2,
                    BAND, this.height - Metrics.SLOT_INSET, Palette.INK_HOVERED);
        }

        int cursorX = checkboxX();

        // Visibility is a yes or a no, so it is the game's checkbox rather than an eye
        // the game has no sprite for.
        Surface.checkbox(canvas, cursorX, this.y + (this.height - Metrics.CHECKBOX) / 2,
                this.layer.visible(), paint.over(cursorX, this.y, Metrics.CHECKBOX, this.height));
        cursorX += Metrics.CHECKBOX + Metrics.PAD_TIGHT;

        boolean duplicate = showsDuplicate(contentX());
        int actionsWidth = !lit ? 0
                : duplicate ? actionWidth() * 2 + Metrics.PAD_TIGHT : actionWidth();
        int rightEdge = this.x + this.width - Metrics.SLOT_INSET - actionsWidth;

        // The thumbnail is dropped rather than shrunk when the row is narrow. A tick
        // box and a picture take 39 of a hundred pixels, and what was left was a name
        // cut after six letters — "Braided c…" identifies a layer no better than no
        // picture and the whole word do.
        int preview = Metrics.LAYER_PREVIEW;
        if (rightEdge - cursorX - preview - Metrics.PAD_TIGHT >= MIN_NAME_ROOM) {
            Thumbnail.draw(canvas, this.sprites.get(), this.layer.atlasIndex(this.slim.get()),
                    ThumbCrop.ALL, cursorX, this.y + (this.height - preview) / 2, preview, preview);
            cursorX += preview + Metrics.PAD_TIGHT;
        }

        int room = rightEdge - cursorX;

        int nameY = this.y + Metrics.SLOT_INSET;
        this.marquee.draw(paint, this.layer.name(), cursorX, nameY, room,
                hot ? Palette.INK_HOVERED : Palette.INK, hot);
        canvas.textFlat(Component.literal(Marquee.cut(canvas, subtitle().getString(), room)),
                cursorX, nameY + canvas.lineHeight() + 1, Palette.INK_MUTED);

        if (lit) {
            int actionX = this.x + this.width - Metrics.SLOT_INSET - actionWidth();
            drawCross(canvas, paint, actionX, this.y + (this.height - Metrics.CROSS) / 2);
            if (duplicate) {
                drawAction(canvas, paint, actionX - actionWidth() - Metrics.PAD_TIGHT, "+",
                        Palette.INK_MUTED);
            }
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
        return Metrics.CROSS;
    }

    /**
     * Whether the row is wide enough to also offer duplication.
     *
     * <p>In a narrow column the two actions and the thumbnail leave the name a handful
     * of pixels, and a stack of rows all reading "..." is a stack you cannot use.
     * Removing is the one that must always be there, so duplication is what goes.
     */
    private boolean showsDuplicate(int contentX) {
        int room = this.x + this.width - Metrics.SLOT_INSET - contentX;
        return room - (actionWidth() * 2 + Metrics.PAD_TIGHT) >= MIN_NAME_ROOM;
    }

    /** Where the row's own content starts, past the visibility box. */
    /**
     * Where the row's contents start: past the frame, and past the band when the row is
     * the selected one — the band is drawn inside the frame, so everything shifts.
     */
    private int checkboxX() {
        return this.x + Metrics.SLOT_INSET
                + (this.selection.get() == this.layer ? Metrics.BAND : 0);
    }

    private int contentX() {
        return checkboxX() + Metrics.CHECKBOX + Metrics.PAD_TIGHT
                + Metrics.LAYER_PREVIEW + Metrics.PAD_TIGHT;
    }

    /** Removing a layer is the game's close cross, which is what it means everywhere. */
    private void drawCross(Canvas canvas, Paint paint, int x, int y) {
        boolean over = paint.over(x, y, Metrics.CROSS, Metrics.CROSS);
        // Red only once the pointer is on it. A permanently red cross beside four
        // quiet rows reads as an alarm rather than as a control.
        Surface.button(canvas, x, y, Metrics.CROSS, Metrics.CROSS,
                over ? Surface.Tone.RED : Surface.Tone.NEUTRAL, over, false);
        canvas.textCentered(Component.literal("x"), x + Metrics.CROSS / 2,
                y + (Metrics.CROSS - canvas.lineHeight()) / 2 + 1, Palette.INK);
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
        if (mouseX < checkboxX() + Metrics.CHECKBOX) {
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
        // Only where the glyph was actually drawn: a narrow row has no duplicate
        // action, and must not have an invisible one either.
        if (showsDuplicate(contentX()) && mouseX >= duplicateX) {
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
