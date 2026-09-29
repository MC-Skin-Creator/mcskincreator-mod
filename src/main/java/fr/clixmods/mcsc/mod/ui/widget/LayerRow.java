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
 * <p>Left to right, the site's order: the grip, the tick, the picture, the name over
 * its subtitle, and the two actions. The actions keep their room at rest and are drawn
 * only when the row is lit — and they are out of reach the rest of the time, because
 * leaving them clickable is how someone deletes a layer by clicking a cross they never
 * saw.
 *
 * <p>The name is set at the full size and the category under it at the half. The name
 * is what the row is for and what has to read across the column; the category is read
 * once you are already looking at the row, and the site sets the two that way round too.
 *
 * <p>The selected row is a tinted slot with a green band down its flank. A hidden layer
 * drops to half opacity, and a row being dragged takes a shadow, fades, and leaves a
 * ghost of itself where it came from.
 */
public class LayerRow extends Element {
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

        // Pointing at a row lights the layer up on the model, so you can tell which of
        // three brown layers is the one you are about to change.
        if (hot != this.wasHot) {
            this.wasHot = hot;
            this.onPeek.accept(hot ? this.layer : null);
        }

        if (this.dragging) {
            // A dragged row is a row in the air: a shadow under it, and the row itself
            // faded, so what you are moving is plainly not where it came from yet.
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

        int cursorX = gripX();

        // The grip. It says the row can be pulled, which nothing else on the row does —
        // and a row you can reorder without knowing you can is a row nobody reorders.
        Surface.grip(canvas, cursorX, this.y + (this.height - Surface.GRIP_HEIGHT) / 2,
                lit ? Palette.INK_MUTED : Palette.INK_DISABLED);
        cursorX += Surface.GRIP_WIDTH + Metrics.PAD_HAIR;

        // Visibility is a yes or a no, so it is a tick box rather than an eye the game
        // has no icon for. At the ordinary tick size: the row's own size is set by the
        // two lines of text on it, and a box taller than both of them together was the
        // largest thing on a row whose subject is a name.
        int box = Metrics.CHECKBOX;
        Surface.checkbox(canvas, cursorX, this.y + (this.height - box) / 2, box,
                this.layer.visible(), paint.over(cursorX, this.y, box, this.height));
        cursorX += box + Metrics.PAD_HAIR;

        // The picture, in a slot of its own. It used to be blitted straight onto the
        // row at whatever size its crop came out, so a full-body element ran over the
        // name beside it; now it is letterboxed into one box, the same box every row.
        int preview = Metrics.LAYER_PREVIEW;
        int previewY = this.y + (this.height - preview) / 2;
        Surface.slot(canvas, cursorX, previewY, preview, preview);
        int inner = Metrics.SLOT_INSET;
        int picture = preview - inner * 2;
        Surface.checker(canvas, cursorX + inner, previewY + inner, picture, picture);
        // The layer's own crop, the same one the library tile used. Asking for the
        // whole body here drew the middle fifteen rows of it — the torso — so every
        // hat, every pair of eyes and every hairstyle came out blank.
        Thumbnail.draw(canvas, this.sprites.get(), this.layer.atlasIndex(this.slim.get()),
                this.layer.thumbCrop(), cursorX + inner, previewY + inner, picture, picture);
        cursorX += preview + Metrics.PAD_TIGHT;

        // The actions keep their room whether or not they are drawn, so a name does not
        // grow and shrink as the pointer crosses the row.
        int rightEdge = this.x + this.width - Metrics.SLOT_INSET - actionsWidth();
        int room = Math.max(0, rightEdge - Metrics.PAD_HAIR - cursorX);

        // Both lines are the small text. The site's layer name is 11 pixels in a column
        // of 318; ours at full size is 8 in a column of 161, half again as large against
        // everything around it — which is why this row read as three times the site's.
        int nameY = this.y + (this.height - canvas.smallLineHeight() * 2 - 2) / 2;
        this.marquee.drawSmall(paint, this.layer.name(), cursorX, nameY, room,
                hot ? Palette.INK_HOVERED : Palette.INK, hot);
        canvas.textSmall(Component.literal(Marquee.cutSmall(canvas, subtitle().getString(), room)),
                cursorX, nameY + canvas.smallLineHeight() + 2, Palette.INK_FAINT);

        if (lit) {
            int actionY = this.y + (this.height - Metrics.CROSS) / 2;
            int removeX = removeX();
            drawAction(canvas, paint, removeX, actionY, true);
            drawAction(canvas, paint, duplicateX(removeX), actionY, false);
        }

        if (!this.layer.visible() || this.dragging) {
            // Half opacity, laid over the finished row: what is off is dimmed rather
            // than recoloured, and what is being dragged is a ghost of where it was.
            canvas.fill(this.x, this.y, this.width, this.height,
                    Palette.withAlpha(0xFF000000, this.dragging ? 96 : 128));
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

    /** Both actions and the hair between them, always reserved and sometimes drawn. */
    private static int actionsWidth() {
        return actionWidth() * 2 + Metrics.PAD_HAIR;
    }

    private int removeX() {
        return this.x + this.width - Metrics.SLOT_INSET - actionWidth();
    }

    private static int duplicateX(int removeX) {
        return removeX - Metrics.PAD_HAIR - actionWidth();
    }

    /**
     * Where the row's contents start: past the frame, and past the band when the row is
     * the selected one — the band is drawn inside the frame, so everything shifts.
     */
    private int gripX() {
        return this.x + Metrics.SLOT_INSET
                + (this.selection.get() == this.layer ? Metrics.BAND : 0);
    }

    private int checkboxX() {
        return gripX() + Surface.GRIP_WIDTH + Metrics.PAD_HAIR;
    }

    /**
     * One of the two actions, both of them buttons.
     *
     * <p>Removing was a button and duplicating was a bare {@code +} floating beside it,
     * which is two answers to the same question on the same row: one of them looked
     * pressable and the other did not, and they do equally serious things.
     */
    private void drawAction(Canvas canvas, Paint paint, int x, int y, boolean remove) {
        int size = actionWidth();
        boolean over = paint.over(x, y, size, size);
        // Red only once the pointer is on it. A permanently red cross beside four quiet
        // rows reads as an alarm rather than as a control.
        Surface.button(canvas, x, y, size, size,
                remove && over ? Surface.Tone.RED : Surface.Tone.NEUTRAL, over, false);
        int ink = over ? Palette.INK : Palette.INK_MUTED;
        // Half the button, which is what every other mark in this interface takes. At
        // two thirds — where this was — the mark ran into the frame and the pair read as
        // one thick smudged border rather than as a button with a sign on it.
        int glyph = Math.max(3, size / 2 | 1);
        if (remove) {
            Surface.cross(canvas, x + (size - glyph) / 2, y + (size - glyph) / 2, glyph, ink);
        } else {
            Surface.copies(canvas, x + (size - glyph) / 2, y + (size - glyph) / 2, glyph, ink);
        }
    }

    @Override
    public boolean mouseDown(double mouseX, double mouseY, int button) {
        if (button != 0 || !contains(mouseX, mouseY)) {
            return false;
        }
        int checkbox = checkboxX();
        if (mouseX >= checkbox && mouseX < checkbox + Metrics.CHECKBOX) {
            this.onToggleVisible.accept(this.layer);
            return true;
        }

        // The actions keep their room at rest and are only drawn when the row is lit,
        // so they are only pressable then either: a cross nobody can see is how someone
        // deletes a layer they never meant to touch. A press is inside the row, which is
        // most of what lights it — the selection is the other half.
        boolean lit = this.wasHot || this.selection.get() == this.layer;
        int removeX = removeX();
        int duplicateX = duplicateX(removeX);
        if (lit && mouseX >= removeX) {
            this.onRemove.accept(this.layer);
            return true;
        }
        if (lit && mouseX >= duplicateX && mouseX < duplicateX + actionWidth()) {
            this.onDuplicate.accept(this.layer);
            return true;
        }

        // Anywhere else on the row selects it and begins a drag. The grip says so; the
        // rest of the row obliges anyway, because a row that only moves by its handle is
        // a row most people never discover moves.
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
