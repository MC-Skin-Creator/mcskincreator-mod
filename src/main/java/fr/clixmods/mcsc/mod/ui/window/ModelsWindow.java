/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui.window;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import fr.clixmods.mcsc.mod.catalog.CatalogModel;
import fr.clixmods.mcsc.mod.catalog.CatalogText;
import fr.clixmods.mcsc.mod.catalog.ThumbCrop;
import fr.clixmods.mcsc.mod.skin.CategorySprites;
import fr.clixmods.mcsc.mod.skin.FrontSprite;
import fr.clixmods.mcsc.mod.style.Metrics;
import fr.clixmods.mcsc.mod.style.Palette;
import fr.clixmods.mcsc.mod.style.Surface;
import fr.clixmods.mcsc.mod.ui.Canvas;
import fr.clixmods.mcsc.mod.ui.Element;
import fr.clixmods.mcsc.mod.ui.Paint;
import fr.clixmods.mcsc.mod.ui.widget.Thumbnail;
import net.minecraft.network.chat.Component;

/**
 * The starter models the catalogue offers: whole characters, each replacing what is
 * being edited.
 *
 * <p>A window rather than a shelf in the library, because that is what choosing one
 * does — it throws the stack away and starts again, which is not a thing to put a
 * click away from the elements. Outfits are the opposite: they stack like any element,
 * so they live in the library, in a region of their own.
 *
 * <p>A catalogue carrying no model never opens this window at all — the top bar drops
 * the button.
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
    private final Function<CatalogText, Component> naming;
    private final Supplier<CategorySprites> sprites;
    private final Consumer<CatalogModel> onChoose;

    public ModelsWindow(Supplier<List<CatalogModel>> models, Function<CatalogText, Component> naming,
                        Supplier<CategorySprites> sprites, Consumer<CatalogModel> onChoose,
                        ModalWindow returnsTo) {
        super("window.mcskincreator.models", returnsTo);
        this.models = models;
        this.naming = naming;
        this.sprites = sprites;
        this.onChoose = onChoose;
    }

    private static int columns(int width) {
        return Math.max(2, (width + Metrics.PAD_TIGHT) / (TILE_PICTURE + Metrics.PAD_TIGHT));
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

    /** The line above the grid, saying the one thing that is expensive to learn by trying. */
    private int noticeHeight(Canvas canvas, int width) {
        return canvas.wrappedHeight(notice(), width) + Metrics.PAD_TIGHT;
    }

    private static Component notice() {
        return Component.translatable("window.mcskincreator.models.notice");
    }

    @Override
    protected int contentHeight(Canvas canvas) {
        int width = width() - Metrics.PAD * 2;
        int columns = columns(width);
        int rows = (this.models.get().size() + columns - 1) / columns;
        int tile = tileHeight(canvas, tileWidth(width, columns)) + Metrics.PAD_TIGHT;
        return noticeHeight(canvas, width) + rows * tile;
    }

    @Override
    protected void layoutBody(Canvas canvas, int left, int top, int width) {
        List<CatalogModel> entries = this.models.get();
        int columns = columns(width);
        int tileWidth = tileWidth(width, columns);
        int tileHeight = tileHeight(canvas, tileWidth);
        int gridTop = top + noticeHeight(canvas, width);

        for (int index = 0; index < entries.size(); index++) {
            CatalogModel model = entries.get(index);
            ModelTile tile = new ModelTile(model, index, this.naming.apply(model.name()),
                    this.sprites, this.onChoose);
            tile.setBounds(left + index % columns * (tileWidth + Metrics.PAD_TIGHT),
                    gridTop + index / columns * (tileHeight + Metrics.PAD_TIGHT),
                    tileWidth, tileHeight);
            addBodyChild(tile);
        }
    }

    @Override
    protected void drawBody(Paint paint, int left, int top, int width) {
        // The tiles are children, so the window draws them with everything else.
        paint.canvas().textWrapped(notice(), left, top, width, Palette.INK_DIM);
    }

    /** One starter model: its picture, its name, and the whole tile is the button. */
    private static final class ModelTile extends Element {
        private final CatalogModel model;
        private final int index;
        private final Component label;
        private final Supplier<CategorySprites> sprites;
        private final Consumer<CatalogModel> onChoose;

        private ModelTile(CatalogModel model, int index, Component label,
                          Supplier<CategorySprites> sprites, Consumer<CatalogModel> onChoose) {
            this.model = model;
            this.index = index;
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
            // Nothing while its categories are still downloading: a model drawn
            // half-dressed looks like a model that comes half-dressed.
            Thumbnail.draw(canvas, this.sprites.get(), this.index, ThumbCrop.ALL,
                    boxX, boxY, boxWidth, boxHeight);

            canvas.pushScissor(boxX, boxY + boxHeight, boxWidth, canvas.lineHeight());
            canvas.textCentered(this.label, boxX + boxWidth / 2, boxY + boxHeight,
                    hot ? Palette.GOLD : Palette.INK_DIM);
            canvas.popScissor();
        }

        @Override
        public List<Component> tooltip() {
            return List.of(this.label, notice());
        }

        @Override
        public boolean clickSound() {
            return true;
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
