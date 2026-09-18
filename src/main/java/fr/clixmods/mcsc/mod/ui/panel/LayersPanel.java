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
 * The right column: the stack, grouped by region, with the selected layer's settings
 * under it — all of it in one scroll.
 *
 * <p>The stack is stored bottom to top, the order the sheet composes in, and shown the
 * other way up — the layer on top of the skin is the one at the top of the list. Each
 * region is its own group with a rule and a count, and dragging moves a row within its
 * own region only: a hat cannot be dragged below a pair of trousers, because on the
 * model it never is.
 *
 * <p>One scroll rather than a list above a pinned inspector. Pinned, the settings
 * wanted 137 pixels of a column that on a 720p window has 163 to give, and the list
 * was left one row that drew straight over them. A band that has to fit in whatever is
 * left is a band that will one day not fit; a band that scrolls fits every screen the
 * game has. The settings follow the selected layer down the list, which is also where
 * one is looking when changing them.
 *
 * <p>With no selection the settings area says so rather than leaving an empty box,
 * which is what an empty state is for.
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
    /** The header's controls, which do not scroll. */
    private final List<Element> fixed = new ArrayList<>();
    /** The settings, which do: they are the tail of the same content as the rows. */
    private final List<Placed> scrolled = new ArrayList<>();

    private PixelButton foldButton;
    /** Where the rule under the model tabs goes: they are a tab bar, so they have one. */
    private int modelTabsBottom;
    private int bodyTop;
    private int bodyHeight;
    /** Where the settings begin, measured from the top of the scrolling content. */
    private int settingsContentY;
    private Layer dragged;

    private record PlacedRow(LayerRow row, int contentY) {
    }

    /** Anything else that scrolls with the rows, and how far down the content it sits. */
    private record Placed(Element element, int contentY) {
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
        this.scrolled.clear();
        this.titles.clear();
        this.fixed.clear();

        int header = headerHeight(canvas);
        this.foldButton = new PixelButton(foldLabel(), PixelButton.Style.GHOST, () -> {
            toggleFolded();
            this.relayout.run();
        });
        this.foldButton.withTooltip(Component.translatable(foldTooltipKey()));
        this.foldButton.fit(canvas);

        if (folded()) {
            this.foldButton.setBounds(this.x + (this.width - this.foldButton.width()) / 2,
                    this.y + (header - Metrics.TAB_HEIGHT) / 2,
                    this.foldButton.width(), Metrics.TAB_HEIGHT);
            this.fixed.add(addChild(this.foldButton));
            return;
        }

        int right = contentRight();
        this.foldButton.setBounds(right - this.foldButton.width(),
                this.y + (header - Metrics.TAB_HEIGHT) / 2,
                this.foldButton.width(), Metrics.TAB_HEIGHT);
        this.fixed.add(addChild(this.foldButton));

        PixelButton add = new PixelButton(Component.literal("+"), PixelButton.Style.GHOST,
                this.onAddRequested);
        add.fit(canvas).withTooltip(Component.translatable("gui.mcskincreator.add.tooltip"));
        add.setBounds(this.foldButton.x() - add.width() - Metrics.PAD_TIGHT,
                this.foldButton.y(), add.width(), Metrics.TAB_HEIGHT);
        this.fixed.add(addChild(add));

        int left = contentLeft();
        int cursorY = this.y + header + Metrics.PAD_TIGHT;

        int segmentX = left;
        for (PlayerModelType kind : new PlayerModelType[] {PlayerModelType.WIDE, PlayerModelType.SLIM}) {
            PixelButton button = new PixelButton(Component.translatable(modelLabelKey(kind)),
                    PixelButton.Style.TAB, () -> {
                        this.history.record();
                        this.project.setModel(kind);
                        this.relayout.run();
                    });
            button.fit(canvas).setActive(this.project.model() == kind);
            button.withTooltip(Component.translatable(modelLabelKey(kind) + ".tooltip"));
            button.setBounds(segmentX, cursorY, button.width(), Metrics.TAB_HEIGHT);
            this.fixed.add(addChild(button));
            segmentX += button.width() + Metrics.SEGMENT_GAP;
        }

        // The two model tabs stand on a rule, the way the game's tabs stand on the thing
        // they open; the import button is an action rather than one of that pair, so it
        // takes a row of its own rather than sharing theirs.
        this.modelTabsBottom = cursorY + Metrics.TAB_HEIGHT;
        cursorY = this.modelTabsBottom + Metrics.PAD;

        PixelButton importTexture = new PixelButton(
                Component.translatable("gui.mcskincreator.import"),
                PixelButton.Style.NORMAL, this.onImportRequested);
        importTexture.withTooltip(Component.translatable("gui.mcskincreator.import.tooltip"));
        importTexture.fitWithin(canvas, right - left);
        importTexture.setBounds(left, cursorY, right - left, Metrics.BUTTON_HEIGHT_COMPACT);
        this.fixed.add(addChild(importTexture));
        cursorY += Metrics.BUTTON_HEIGHT_COMPACT + Metrics.PAD;

        this.bodyTop = cursorY;
        this.bodyHeight = Math.max(0, this.y + this.height - Metrics.PANEL_INSET - cursorY);

        layoutStack(canvas, left, right);
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

        if (cursorY > 0) {
            cursorY += Metrics.PAD;
        }
        this.settingsContentY = cursorY;
        cursorY = layoutSettings(canvas, left, right, cursorY);
        this.scroll.setContent(cursorY, this.bodyHeight);
    }

    /**
     * The settings of the selected layer, at the foot of the scrolling content.
     *
     * @return where the content ends
     */
    private int layoutSettings(Canvas canvas, int left, int right, int top) {
        int cursorY = top + canvas.lineHeight() + Metrics.PAD_TIGHT;
        Layer layer = this.project.selected();
        if (layer == null) {
            // Nothing to set, so nothing is laid out: the band says why in its own place.
            return cursorY;
        }
        int gutter = ScrollPane.BAR_WIDTH + Metrics.PAD_TIGHT;
        int width = right - left - gutter;

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
        reset.withTooltip(Component.translatable("gui.mcskincreator.reset_colors.tooltip"));
        reset.fitWithin(canvas, width);
        reset.setBounds(left, cursorY, width, Metrics.BUTTON_HEIGHT_COMPACT);
        this.scrolled.add(new Placed(addChild(reset), cursorY));
        return cursorY + Metrics.BUTTON_HEIGHT_COMPACT;
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
        slider.setBounds(left, top, width, Slider.heightFor(canvas));
        this.scrolled.add(new Placed(addChild(slider), top));
        return top + Slider.heightFor(canvas) + Metrics.PAD_TIGHT;
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
            element.draw(paint);
        }
        if (folded()) {
            return;
        }
        Surface.rule(canvas, contentLeft(), this.modelTabsBottom, contentWidth());

        int left = contentLeft();
        int right = contentRight();
        int railRight = right - ScrollPane.BAR_WIDTH - Metrics.PAD_TIGHT;

        place();
        canvas.pushScissor(left, this.bodyTop, right - left, this.bodyHeight);
        int offset = this.bodyTop - this.scroll.offset();

        if (this.rows.isEmpty()) {
            drawEmptyState(canvas, Component.translatable("empty.mcskincreator.layers"),
                    left, railRight, this.bodyTop, this.settingsContentY);
        } else {
            for (GroupTitle title : this.titles) {
                drawGroupTitle(canvas, title, left, railRight, offset + title.contentY());
            }
            for (PlacedRow placed : this.rows) {
                if (onScreen(placed.row())) {
                    placed.row().draw(paint);
                }
            }
        }

        drawSettings(paint, left, railRight, offset + this.settingsContentY);
        canvas.popScissor();
        this.scroll.drawBar(canvas, right, this.bodyTop, this.bodyHeight);
    }

    private void drawGroupTitle(Canvas canvas, GroupTitle title, int left, int right, int y) {
        String name = LibraryPanel.regionLabel(title.region()).getString().toUpperCase(Locale.ROOT);
        canvas.textTracked(name, left, y, Palette.INK, Metrics.TITLE_TRACKING);
        int nameWidth = canvas.trackedWidth(name, Metrics.TITLE_TRACKING);

        String count = Integer.toString(title.count());
        int countWidth = canvas.textWidth(count);
        int ruleX = left + nameWidth + Metrics.PAD_TIGHT;
        int ruleWidth = right - countWidth - Metrics.PAD_TIGHT - ruleX;
        if (ruleWidth > 0) {
            Surface.rule(canvas, ruleX, y + canvas.lineHeight() / 2, ruleWidth);
        }
        canvas.textFlat(Component.literal(count), right - countWidth, y, Palette.INK_MUTED);
    }

    /** The settings, drawn where the scroll has put them: a heading, then the controls. */
    private void drawSettings(Paint paint, int left, int right, int top) {
        Canvas canvas = paint.canvas();
        Layer layer = this.project.selected();
        if (layer == null) {
            canvas.textWrapped(Component.translatable("empty.mcskincreator.inspector"),
                    left, top, Math.max(1, right - left), Palette.INK_MUTED);
            return;
        }

        // Ruled off from the stack above it: these settings belong to one row of that
        // list, and without a line the heading reads as one more group of it.
        Surface.rule(canvas, left, top - Metrics.PAD_TIGHT, Math.max(0, right - left));

        String title = Component.translatable("gui.mcskincreator.settings").getString()
                .toUpperCase(Locale.ROOT);
        canvas.textTracked(title, left, top, Palette.INK, Metrics.TITLE_TRACKING);
        int titleWidth = canvas.trackedWidth(title, Metrics.TITLE_TRACKING);
        int nameRoom = Math.max(0, right - left - titleWidth - Metrics.PAD_TIGHT);
        canvas.text(Component.literal(fr.clixmods.mcsc.mod.ui.Marquee.cut(
                        canvas, layer.name().getString(), nameRoom)),
                left + titleWidth + Metrics.PAD_TIGHT, top, Palette.INK);

        for (Placed placed : this.scrolled) {
            if (onScreen(placed.element())) {
                placed.element().draw(paint);
            }
        }
    }

    /**
     * An empty state, wrapped to the panel and centred in the band it explains.
     *
     * <p>Wrapped rather than centred on one line: these are sentences, the panel is
     * narrow, and a sentence centred on one line runs off both ends of it.
     */
    private static void drawEmptyState(Canvas canvas, Component message,
                                       int left, int right, int top, int height) {
        int width = Math.max(1, right - left);
        canvas.textWrapped(message, left,
                top + (height - canvas.wrappedHeight(message, width)) / 2,
                width, Palette.INK_MUTED);
    }

    /** Moves everything that scrolls to where the offset now puts it. */
    private void place() {
        int offset = this.bodyTop - this.scroll.offset();
        for (PlacedRow placed : this.rows) {
            LayerRow row = placed.row();
            row.setBounds(row.x(), offset + placed.contentY(), row.width(), row.height());
            row.clipTo(this.x, this.bodyTop, this.width, this.bodyHeight);
            row.setDragging(this.dragged == row.layer());
        }
        for (Placed placed : this.scrolled) {
            Element element = placed.element();
            element.setBounds(element.x(), offset + placed.contentY(),
                    element.width(), element.height());
            element.clipTo(this.x, this.bodyTop, this.width, this.bodyHeight);
        }
    }

    private boolean onScreen(Element element) {
        return element.y() + element.height() > this.bodyTop
                && element.y() < this.bodyTop + this.bodyHeight;
    }

    /** Everything clickable, rows scrolled out of the band excluded. */
    public List<Element> hitTargets() {
        List<Element> targets = new ArrayList<>(this.fixed);
        if (folded()) {
            return targets;
        }
        place();
        for (PlacedRow placed : this.rows) {
            if (onScreen(placed.row())) {
                targets.add(placed.row());
            }
        }
        for (Placed placed : this.scrolled) {
            if (onScreen(placed.element())) {
                targets.add(placed.element());
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
        if (folded() || button != 0) {
            return false;
        }
        return this.scroll.barMouseDown(mouseX, mouseY,
                contentRight(), this.bodyTop, this.bodyHeight);
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
