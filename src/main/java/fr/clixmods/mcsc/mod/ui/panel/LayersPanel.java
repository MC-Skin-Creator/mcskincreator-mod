/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui.panel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import java.util.function.Function;
import java.util.function.Supplier;

import fr.clixmods.mcsc.mod.catalog.Catalog;
import fr.clixmods.mcsc.mod.project.History;
import fr.clixmods.mcsc.mod.project.Layer;
import fr.clixmods.mcsc.mod.project.SkinProject;
import fr.clixmods.mcsc.mod.skin.CategorySprites;
import fr.clixmods.mcsc.mod.style.Metrics;
import fr.clixmods.mcsc.mod.style.Palette;
import fr.clixmods.mcsc.mod.style.Surface;
import fr.clixmods.mcsc.mod.ui.Canvas;
import fr.clixmods.mcsc.mod.ui.Element;
import fr.clixmods.mcsc.mod.ui.Paint;
import fr.clixmods.mcsc.mod.ui.ScrollPane;
import fr.clixmods.mcsc.mod.ui.widget.LayerRow;
import fr.clixmods.mcsc.mod.ui.widget.PixelButton;
import fr.clixmods.mcsc.mod.ui.widget.Slider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerModelType;

/**
 * The right column: the stack, grouped by region, and the inspector pinned under it.
 *
 * <p>The stack is stored bottom to top, the order the sheet composes in, and shown
 * the other way up — the layer on top of the skin is the one at the top of the list.
 * Each region is its own group with a rule and a count, and dragging moves a row
 * within its own region only: a hat cannot be dragged below a pair of trousers,
 * because on the model it never is.
 *
 * <p>The inspector shows the selected layer and nothing else. With no selection it
 * says so in the middle of its own area rather than leaving an empty box, which is
 * what an empty state is for.
 */
public class LayersPanel extends Panel {
    private final SkinProject project;
    private final Supplier<Catalog> catalog;
    private final Function<String, CategorySprites> sprites;
    private final History history;
    private final Runnable relayout;
    private final Runnable onAddRequested;
    private final Runnable onImportRequested;
    private final java.util.function.Consumer<Layer> onPeek;

    private final ScrollPane scroll = new ScrollPane();
    private final List<PlacedRow> rows = new ArrayList<>();
    private final List<GroupTitle> titles = new ArrayList<>();
    private final List<Element> fixed = new ArrayList<>();

    private PixelButton foldButton;
    private int bodyTop;
    private int bodyHeight;
    private int inspectorTop;
    private Layer dragged;

    private record PlacedRow(LayerRow row, int contentY) {
    }

    private record GroupTitle(String region, int count, int contentY) {
    }

    public LayersPanel(SkinProject project, Supplier<Catalog> catalog,
                       Function<String, CategorySprites> sprites, History history,
                       Runnable relayout, Runnable onAddRequested, Runnable onImportRequested,
                       java.util.function.Consumer<Layer> onPeek) {
        super("panel.mcskincreator.layers", false);
        this.project = project;
        this.catalog = catalog;
        this.sprites = sprites;
        this.history = history;
        this.relayout = relayout;
        this.onAddRequested = onAddRequested;
        this.onImportRequested = onImportRequested;
        this.onPeek = onPeek;
    }

    @Override
    public void layout(Canvas canvas) {
        clearChildren();
        this.rows.clear();
        this.titles.clear();
        this.fixed.clear();

        int header = headerHeight(canvas);
        this.foldButton = new PixelButton(foldLabel(), PixelButton.Style.GHOST, () -> {
            toggleFolded();
            this.relayout.run();
        }).withTooltip(Component.translatable(foldTooltipKey()));
        this.foldButton.fit(canvas);

        if (folded()) {
            this.foldButton.setBounds(this.x + (this.width - this.foldButton.width()) / 2,
                    this.y + (header - Metrics.BUTTON_HEIGHT) / 2,
                    this.foldButton.width(), Metrics.BUTTON_HEIGHT);
            this.fixed.add(addChild(this.foldButton));
            return;
        }

        int right = this.x + this.width - Metrics.PAD_TIGHT;
        this.foldButton.setBounds(right - this.foldButton.width(),
                this.y + (header - Metrics.BUTTON_HEIGHT) / 2,
                this.foldButton.width(), Metrics.BUTTON_HEIGHT);
        this.fixed.add(addChild(this.foldButton));

        PixelButton add = new PixelButton(Component.literal("+"), PixelButton.Style.GHOST,
                this.onAddRequested);
        add.fit(canvas).withTooltip(Component.translatable("gui.mcskincreator.add.tooltip"));
        add.setBounds(this.foldButton.x() - add.width() - Metrics.PAD_TIGHT,
                this.foldButton.y(), add.width(), Metrics.BUTTON_HEIGHT);
        this.fixed.add(addChild(add));

        int left = this.x + Metrics.PAD_TIGHT;
        int cursorY = this.y + header + Metrics.PAD_TIGHT;

        int segmentX = left;
        for (PlayerModelType kind : new PlayerModelType[] {PlayerModelType.WIDE, PlayerModelType.SLIM}) {
            PixelButton button = new PixelButton(Component.translatable(modelLabelKey(kind)),
                    PixelButton.Style.NORMAL, () -> {
                        this.history.record();
                        this.project.setModel(kind);
                        this.relayout.run();
                    });
            button.fit(canvas).setActive(this.project.model() == kind);
            button.withTooltip(Component.translatable(modelLabelKey(kind) + ".tooltip"));
            button.setBounds(segmentX, cursorY, button.width(), Metrics.BUTTON_HEIGHT_COMPACT);
            this.fixed.add(addChild(button));
            segmentX += button.width() + Metrics.SEGMENT_GAP;
        }

        PixelButton importTexture = new PixelButton(
                Component.translatable("gui.mcskincreator.import"),
                PixelButton.Style.NORMAL, this.onImportRequested);
        importTexture.fit(canvas)
                .withTooltip(Component.translatable("gui.mcskincreator.import.tooltip"));
        if (segmentX + importTexture.width() <= right) {
            importTexture.setBounds(right - importTexture.width(), cursorY,
                    importTexture.width(), Metrics.BUTTON_HEIGHT_COMPACT);
            this.fixed.add(addChild(importTexture));
            cursorY += Metrics.BUTTON_HEIGHT_COMPACT + Metrics.PAD_TIGHT;
        } else {
            cursorY += Metrics.BUTTON_HEIGHT_COMPACT + Metrics.PAD_TIGHT;
            importTexture.setBounds(left, cursorY, importTexture.width(), Metrics.BUTTON_HEIGHT_COMPACT);
            this.fixed.add(addChild(importTexture));
            cursorY += Metrics.BUTTON_HEIGHT_COMPACT + Metrics.PAD_TIGHT;
        }

        int inspectorHeight = inspectorHeight(canvas);
        this.bodyTop = cursorY;
        this.bodyHeight = Math.max(Metrics.LAYER_ROW,
                this.y + this.height - inspectorHeight - cursorY);
        this.inspectorTop = this.y + this.height - inspectorHeight;

        layoutStack(canvas, left, right);
        layoutInspector(canvas, left, right);
    }

    private static String modelLabelKey(PlayerModelType kind) {
        return kind == PlayerModelType.SLIM
                ? "model.mcskincreator.slim"
                : "model.mcskincreator.classic";
    }

    private void layoutStack(Canvas canvas, int left, int right) {
        int gutter = ScrollPane.BAR_WIDTH + Metrics.PAD_TIGHT;
        int rowWidth = right - left - gutter;
        int titleHeight = canvas.lineHeight() + Metrics.PAD_TIGHT;
        int cursorY = 0;

        // Regions run top to bottom in the catalogue's own order, and each region shows
        // its own layers topmost first.
        for (String region : this.project.regionsInUse(this.catalog.get().regions())) {
            List<Layer> inRegion = this.project.displayOrder(region);
            if (inRegion.isEmpty()) {
                continue;
            }
            this.titles.add(new GroupTitle(region, inRegion.size(), cursorY));
            cursorY += titleHeight;
            for (Layer layer : inRegion) {
                LayerRow row = new LayerRow(layer,
                        () -> this.sprites.apply(layer.categoryId()), this.project::isSlim,
                        this.project::selected,
                        this::select, this::toggleVisible, this::duplicate, this::remove,
                        this::grab, this.onPeek);
                row.setBounds(left, cursorY, rowWidth, Metrics.LAYER_ROW);
                this.rows.add(new PlacedRow(addChild(row), cursorY));
                cursorY += Metrics.LAYER_ROW + Metrics.SEGMENT_GAP;
            }
        }
        this.scroll.setContent(cursorY, this.bodyHeight);
    }

    private int inspectorHeight(Canvas canvas) {
        int line = canvas.lineHeight() + Metrics.PAD_TIGHT;
        if (this.project.selected() == null) {
            return line * 3;
        }
        return Metrics.PAD_TIGHT * 2 + line
                + Metrics.SLIDER_KNOB_HEIGHT * 4 + Metrics.PAD_TIGHT * 4
                + Metrics.BUTTON_HEIGHT_COMPACT + Metrics.PAD_TIGHT;
    }

    private void layoutInspector(Canvas canvas, int left, int right) {
        Layer layer = this.project.selected();
        if (layer == null) {
            return;
        }
        int cursorY = this.inspectorTop + canvas.lineHeight() + Metrics.PAD_TIGHT * 2;
        int width = right - left;

        cursorY = addSlider(canvas, left, cursorY, width, "opacity", 0, 100,
                layer::opacity, layer::setOpacity, value -> Component.literal(value + "%"));
        cursorY = addSlider(canvas, left, cursorY, width, "hue", -180, 180,
                layer::hue, layer::setHue, value -> Component.literal(value + "°"));
        cursorY = addSlider(canvas, left, cursorY, width, "saturation", 0, 200,
                layer::saturation, layer::setSaturation, value -> Component.literal(value + "%"));
        cursorY = addSlider(canvas, left, cursorY, width, "brightness", -50, 50,
                layer::brightness, layer::setBrightness,
                value -> Component.literal(Integer.toString(value)));

        PixelButton reset = new PixelButton(Component.translatable("gui.mcskincreator.reset_colors"),
                PixelButton.Style.NORMAL, () -> {
                    this.history.record();
                    layer.resetAdjustments();
                    this.project.touch();
                });
        reset.fit(canvas).withTooltip(Component.translatable("gui.mcskincreator.reset_colors.tooltip"));
        reset.setBounds(left, cursorY, Math.min(reset.width(), right - left),
                Metrics.BUTTON_HEIGHT_COMPACT);
        this.fixed.add(addChild(reset));
    }

    private int addSlider(Canvas canvas, int left, int top, int width, String id,
                          int minimum, int maximum,
                          java.util.function.IntSupplier read,
                          java.util.function.IntConsumer write,
                          java.util.function.IntFunction<Component> format) {
        Slider slider = new Slider(Component.translatable("setting.mcskincreator." + id),
                minimum, maximum, read,
                value -> {
                    write.accept(value);
                    this.project.touch();
                },
                format,
                // One drag is one entry in the history, however many frames it lasts.
                () -> this.history.beginGesture(id), this.history::endGesture);
        slider.setBounds(left, top, width, Metrics.SLIDER_KNOB_HEIGHT);
        this.fixed.add(addChild(slider));
        return top + Metrics.SLIDER_KNOB_HEIGHT + Metrics.PAD_TIGHT;
    }

    private void select(Layer layer) {
        this.project.select(layer);
        this.relayout.run();
    }

    private void toggleVisible(Layer layer) {
        this.history.record();
        layer.setVisible(!layer.visible());
        this.project.touch();
    }

    private void duplicate(Layer layer) {
        this.history.record();
        this.project.duplicate(layer);
        this.relayout.run();
    }

    private void remove(Layer layer) {
        this.history.record();
        this.project.remove(layer);
        this.relayout.run();
    }

    private void grab(Layer layer) {
        this.dragged = layer;
        // One snapshot for the whole drag, taken before the first swap.
        this.history.beginGesture(layer);
    }

    @Override
    public void draw(Paint paint) {
        Canvas canvas = paint.canvas();
        drawFrame(paint);
        for (Element element : this.fixed) {
            if (!(element instanceof Slider)) {
                element.draw(paint);
            }
        }
        if (folded()) {
            return;
        }

        int left = this.x + Metrics.PAD_TIGHT;
        int right = this.x + this.width - Metrics.PAD_TIGHT;

        if (this.rows.isEmpty()) {
            canvas.textCentered(Component.translatable("empty.mcskincreator.layers"),
                    (left + right) / 2, this.bodyTop + this.bodyHeight / 2 - canvas.lineHeight(),
                    Palette.INK_FAINT);
        } else {
            placeRows();
            canvas.pushScissor(left, this.bodyTop, right - left, this.bodyHeight);
            int offset = this.bodyTop - this.scroll.offset();
            for (GroupTitle title : this.titles) {
                drawGroupTitle(canvas, title, left, right, offset + title.contentY());
            }
            for (PlacedRow placed : this.rows) {
                if (onScreen(placed.row())) {
                    placed.row().draw(paint);
                }
            }
            canvas.popScissor();
            this.scroll.drawBar(canvas, right, this.bodyTop, this.bodyHeight);
        }

        drawInspector(paint, left, right);
    }

    private void drawGroupTitle(Canvas canvas, GroupTitle title, int left, int right, int y) {
        String name = LibraryPanel.regionLabel(title.region()).getString().toUpperCase(Locale.ROOT);
        canvas.textTracked(name, left, y, Palette.INK_DIM, Metrics.TITLE_TRACKING);
        int nameWidth = canvas.trackedWidth(name, Metrics.TITLE_TRACKING);

        String count = Integer.toString(title.count());
        int countWidth = canvas.textWidth(count);
        int ruleX = left + nameWidth + Metrics.PAD_TIGHT;
        int ruleWidth = right - countWidth - Metrics.PAD_TIGHT - ruleX;
        if (ruleWidth > 0) {
            canvas.fill(ruleX, y + canvas.lineHeight() / 2, ruleWidth, 1, Palette.PANEL_MID);
        }
        canvas.textFlat(Component.literal(count), right - countWidth, y, Palette.INK_FAINT);
    }

    private void drawInspector(Paint paint, int left, int right) {
        Canvas canvas = paint.canvas();
        int height = this.y + this.height - this.inspectorTop - Metrics.OUTLINE;
        Surface.flat(canvas, this.x + Metrics.OUTLINE, this.inspectorTop,
                this.width - Metrics.OUTLINE * 2, height, Palette.PANEL_HEADER);
        canvas.fill(this.x + Metrics.OUTLINE, this.inspectorTop,
                this.width - Metrics.OUTLINE * 2, 1, Palette.PANEL_MID);

        Layer layer = this.project.selected();
        if (layer == null) {
            canvas.textCentered(Component.translatable("empty.mcskincreator.inspector"),
                    (left + right) / 2, this.inspectorTop + height / 2 - canvas.lineHeight() / 2,
                    Palette.INK_FAINT);
            return;
        }

        String title = Component.translatable("gui.mcskincreator.settings").getString()
                .toUpperCase(Locale.ROOT);
        canvas.textTracked(title, left, this.inspectorTop + Metrics.PAD_TIGHT,
                Palette.INK_DIM, Metrics.TITLE_TRACKING);
        int titleWidth = canvas.trackedWidth(title, Metrics.TITLE_TRACKING);
        canvas.text(layer.name(), left + titleWidth + Metrics.PAD_TIGHT,
                this.inspectorTop + Metrics.PAD_TIGHT, Palette.INK);

        for (Element element : this.fixed) {
            if (element instanceof Slider) {
                element.draw(paint);
            }
        }
    }

    private void placeRows() {
        int offset = this.bodyTop - this.scroll.offset();
        for (PlacedRow placed : this.rows) {
            LayerRow row = placed.row();
            row.setBounds(row.x(), offset + placed.contentY(), row.width(), row.height());
            row.setDragging(this.dragged == row.layer());
        }
    }

    private boolean onScreen(LayerRow row) {
        return row.y() + row.height() > this.bodyTop && row.y() < this.bodyTop + this.bodyHeight;
    }

    /** Everything clickable, rows scrolled out of the band excluded. */
    public List<Element> hitTargets() {
        List<Element> targets = new ArrayList<>(this.fixed);
        if (folded()) {
            return targets;
        }
        placeRows();
        for (PlacedRow placed : this.rows) {
            if (onScreen(placed.row())) {
                targets.add(placed.row());
            }
        }
        return targets;
    }

    @Override
    public boolean scroll(double mouseX, double mouseY, double amount) {
        if (folded() || mouseY < this.bodyTop || mouseY > this.bodyTop + this.bodyHeight) {
            return false;
        }
        return this.scroll.scroll(amount);
    }

    @Override
    public boolean mouseDown(double mouseX, double mouseY, int button) {
        if (folded()) {
            return false;
        }
        return this.scroll.barMouseDown(mouseX, mouseY,
                this.x + this.width - Metrics.PAD_TIGHT, this.bodyTop, this.bodyHeight);
    }

    /**
     * Dragging a row swaps it with its neighbour one step at a time, so the stack is
     * always in a state the composer could render — there is no in-between order.
     */
    @Override
    public void mouseDrag(double mouseX, double mouseY, double dragX, double dragY, int button) {
        this.scroll.barMouseDrag(mouseY, this.bodyTop, this.bodyHeight);
        if (this.dragged == null) {
            return;
        }
        for (PlacedRow placed : this.rows) {
            if (placed.row().layer() != this.dragged) {
                continue;
            }
            int centre = placed.row().y() + placed.row().height() / 2;
            if (mouseY < centre - Metrics.LAYER_ROW / 2 && this.project.move(this.dragged, 1)) {
                this.relayout.run();
            } else if (mouseY > centre + Metrics.LAYER_ROW / 2 && this.project.move(this.dragged, -1)) {
                this.relayout.run();
            }
            return;
        }
    }

    @Override
    public void mouseUp(double mouseX, double mouseY, int button) {
        this.scroll.barMouseUp();
        if (this.dragged != null) {
            this.dragged = null;
            this.history.endGesture();
        }
    }
}
