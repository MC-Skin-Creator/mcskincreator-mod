/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui.window;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import fr.clixmods.mcsc.mod.catalog.CatalogModel;
import fr.clixmods.mcsc.mod.catalog.CatalogText;
import fr.clixmods.mcsc.mod.skin.FrontSprite;
import fr.clixmods.mcsc.mod.skin.ModelSprites;
import fr.clixmods.mcsc.mod.style.Metrics;
import fr.clixmods.mcsc.mod.style.Palette;
import fr.clixmods.mcsc.mod.style.Surface;
import fr.clixmods.mcsc.mod.ui.Canvas;
import fr.clixmods.mcsc.mod.ui.Element;
import fr.clixmods.mcsc.mod.ui.Paint;
import net.minecraft.network.chat.Component;

/**
 * The ready-made stacks the catalogue offers, in two batches.
 *
 * <p>Models first — whole characters, which replace what is being edited — then
 * outfits, which dress whoever is already there. The two are kept apart and each says
 * what choosing it will do, because they differ in the one way that is expensive to
 * discover by trying: one of them throws away the stack.
 *
 * <p>A batch with nothing in it is left out rather than shown empty. A catalogue that
 * carries neither never opens this window at all — the top bar drops the button.
 */
public class ModelsWindow extends ModalWindow {
    /**
     * The narrowest a tile may be. A ready-made stack is a whole character rather than
     * a hat, so it is shown as a full-length portrait and wants more room than a
     * library thumbnail: packing more, smaller tiles into the row would save scrolling
     * and cost the very thing the window is for.
     */
    private static final int TILE_PICTURE = Metrics.ui(72);

    private final Supplier<List<CatalogModel>> models;
    private final Supplier<List<CatalogModel>> outfits;
    private final Function<CatalogText, Component> naming;
    private final Supplier<ModelSprites> sprites;
    private final Consumer<CatalogModel> onChoose;

    private final List<Batch> batches = new ArrayList<>();

    /** A titled run of tiles. Where it sits is worked out by the walk, never stored. */
    private record Batch(Component title, Component detail, List<CatalogModel> entries) {
    }

    public ModelsWindow(Supplier<List<CatalogModel>> models, Supplier<List<CatalogModel>> outfits,
                        Function<CatalogText, Component> naming, Supplier<ModelSprites> sprites,
                        Consumer<CatalogModel> onChoose, ModalWindow returnsTo) {
        super("window.mcskincreator.models", returnsTo);
        this.models = models;
        this.outfits = outfits;
        this.naming = naming;
        this.sprites = sprites;
        this.onChoose = onChoose;
    }

    private static int columns(int width) {
        int tile = TILE_PICTURE + Metrics.PAD_TIGHT;
        return Math.max(2, (width + Metrics.PAD_TIGHT) / tile);
    }

    private static int tileWidth(int width, int columns) {
        return (width - Metrics.PAD_TIGHT * (columns - 1)) / columns;
    }

    private int tileHeight(Canvas canvas, int tileWidth) {
        int inset = Metrics.OUTLINE + Metrics.PAD_TIGHT;
        // A character is twice as tall as it is wide, so the box follows the picture
        // rather than the other way round. Floored at a pixel: a window narrow enough
        // to make this negative would otherwise lay the whole body out upside down.
        int picture = Math.max(1, (tileWidth - inset * 2) * FrontSprite.HEIGHT / FrontSprite.WIDTH);
        return inset * 2 + picture + canvas.lineHeight();
    }

    /** Walks the batches, handing each its own top; the same walk lays out and measures. */
    private int walk(Canvas canvas, int width, RowVisitor visitor) {
        int columns = columns(width);
        int tileWidth = tileWidth(width, columns);
        int tileHeight = tileHeight(canvas, tileWidth);
        int headerHeight = canvas.lineHeight() * 2 + Metrics.PAD_TIGHT;

        int cursorY = 0;
        for (Batch batch : this.batches) {
            visitor.header(batch, cursorY);
            cursorY += headerHeight + Metrics.PAD_TIGHT;
            for (int index = 0; index < batch.entries().size(); index++) {
                int column = index % columns;
                int row = index / columns;
                visitor.tile(batch.entries().get(index),
                        column * (tileWidth + Metrics.PAD_TIGHT),
                        cursorY + row * (tileHeight + Metrics.PAD_TIGHT),
                        tileWidth, tileHeight);
            }
            int rows = (batch.entries().size() + columns - 1) / columns;
            cursorY += rows * (tileHeight + Metrics.PAD_TIGHT) + Metrics.PAD;
        }
        return cursorY;
    }

    private interface RowVisitor {
        void header(Batch batch, int y);

        void tile(CatalogModel model, int x, int y, int width, int height);
    }

    /** Rebuilt on every layout, so a catalogue arriving mid-window is picked up. */
    private void refreshBatches() {
        this.batches.clear();
        List<CatalogModel> asModels = this.models.get();
        List<CatalogModel> asOutfits = this.outfits.get();
        if (!asModels.isEmpty()) {
            this.batches.add(new Batch(
                    Component.translatable("window.mcskincreator.models.models"),
                    Component.translatable("window.mcskincreator.models.models.detail"),
                    asModels));
        }
        if (!asOutfits.isEmpty()) {
            this.batches.add(new Batch(
                    Component.translatable("window.mcskincreator.models.outfits"),
                    Component.translatable("window.mcskincreator.models.outfits.detail"),
                    asOutfits));
        }
    }

    @Override
    protected int contentHeight(Canvas canvas) {
        refreshBatches();
        return walk(canvas, width() - Metrics.PAD * 2, new RowVisitor() {
            @Override
            public void header(Batch batch, int y) {
                // measuring only
            }

            @Override
            public void tile(CatalogModel model, int x, int y, int tileWidth, int tileHeight) {
                // measuring only
            }
        });
    }

    @Override
    protected void layoutBody(Canvas canvas, int left, int top, int width) {
        refreshBatches();
        walk(canvas, width, new RowVisitor() {
            @Override
            public void header(Batch batch, int y) {
                // Titles are drawn, not placed: they hold nothing that can be clicked.
            }

            @Override
            public void tile(CatalogModel model, int x, int y, int tileWidth, int tileHeight) {
                ModelTile tile = new ModelTile(model, ModelsWindow.this.naming.apply(model.name()),
                        ModelsWindow.this.sprites, ModelsWindow.this.onChoose);
                tile.setBounds(left + x, top + y, tileWidth, tileHeight);
                addBodyChild(tile);
            }
        });
    }

    @Override
    protected void drawBody(Paint paint, int left, int top, int width) {
        Canvas canvas = paint.canvas();
        // The same walk the layout used, so a title cannot drift from the tiles under
        // it whatever the scroll is doing. The tiles themselves are children, and the
        // window draws those with everything else.
        walk(canvas, width, new RowVisitor() {
            @Override
            public void header(Batch batch, int y) {
                canvas.text(batch.title(), left, top + y, Palette.GOLD);
                canvas.textFlat(batch.detail(), left,
                        top + y + canvas.lineHeight() + Metrics.PAD_TIGHT, Palette.INK_DIM);
            }

            @Override
            public void tile(CatalogModel model, int x, int y, int tileWidth, int tileHeight) {
                // drawn as a child
            }
        });
    }

    /** One ready-made stack: its picture, its name, and the whole tile is the button. */
    private static final class ModelTile extends Element {
        private final CatalogModel model;
        private final Component label;
        private final Supplier<ModelSprites> sprites;
        private final Consumer<CatalogModel> onChoose;

        private ModelTile(CatalogModel model, Component label, Supplier<ModelSprites> sprites,
                          Consumer<CatalogModel> onChoose) {
            this.model = model;
            this.label = label;
            this.sprites = sprites;
            this.onChoose = onChoose;
        }

        @Override
        public void draw(Paint paint) {
            Canvas canvas = paint.canvas();
            boolean hot = paint.hot(this);
            Surface.slot(canvas, this.x, this.y, this.width, this.height,
                    hot ? Palette.SLOT_HOVER : Palette.SLOT);

            int inset = Metrics.OUTLINE + Metrics.PAD_TIGHT;
            int boxX = this.x + inset;
            int boxY = this.y + inset;
            int boxWidth = this.width - inset * 2;
            int boxHeight = this.height - inset * 2 - canvas.lineHeight();
            Surface.checker(canvas, boxX, boxY, boxWidth, boxHeight);
            drawPicture(canvas, boxX, boxY, boxWidth, boxHeight);

            canvas.pushScissor(boxX, boxY + boxHeight, boxWidth, canvas.lineHeight());
            canvas.textCentered(this.label, boxX + boxWidth / 2, boxY + boxHeight,
                    hot ? Palette.GOLD : Palette.INK_DIM);
            canvas.popScissor();
        }

        private void drawPicture(Canvas canvas, int boxX, int boxY, int boxWidth, int boxHeight) {
            ModelSprites sheet = this.sprites.get();
            if (sheet == null || !sheet.has(this.model)) {
                // Its categories are still downloading. Nothing is better than a
                // half-dressed character that looks like the model itself.
                return;
            }
            int scale = Math.max(1, Math.min(boxWidth / FrontSprite.WIDTH, boxHeight / FrontSprite.HEIGHT));
            int drawnWidth = FrontSprite.WIDTH * scale;
            int drawnHeight = FrontSprite.HEIGHT * scale;
            canvas.blit(sheet.texture(),
                    boxX + (boxWidth - drawnWidth) / 2, boxY + (boxHeight - drawnHeight) / 2,
                    drawnWidth, drawnHeight,
                    sheet.spriteU(this.model), sheet.spriteV(this.model),
                    FrontSprite.WIDTH, FrontSprite.HEIGHT,
                    sheet.sheetWidth(), sheet.sheetHeight());
        }

        @Override
        public List<Component> tooltip() {
            return List.of(this.label, Component.translatable(
                    this.model.kind() == CatalogModel.Kind.MODEL
                            ? "window.mcskincreator.models.replaces"
                            : "window.mcskincreator.models.stacks"));
        }

        @Override
        public boolean mouseDown(double mouseX, double mouseY, int button) {
            return button == 0 && contains(mouseX, mouseY) && activate();
        }

        @Override
        public boolean activate() {
            this.onChoose.accept(this.model);
            return true;
        }
    }
}
