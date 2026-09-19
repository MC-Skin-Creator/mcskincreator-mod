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
import fr.clixmods.mcsc.mod.ui.widget.Dropdown;
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
 * <p>The list scrolls and the settings do not. They are a fixed panel at the foot of
 * the column: what is being adjusted stays under the hand while the list moves, and a
 * slider that can scroll out from under a drag is a slider nobody can use. What makes
 * that safe is that the editor now takes its own scale, so the column has some 450
 * pixels rather than 360 and the settings fit in what is left with room to spare —
 * they are clipped to their own band regardless, so they can never draw over the list
 * the way they did before.
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
    /** The header's controls and the settings, none of which scroll. */
    private final List<Element> fixed = new ArrayList<>();
    /** The settings only, so the band they live in can be drawn and clipped on its own. */
    private final List<Element> settings = new ArrayList<>();

    private PixelButton foldButton;
    private int bodyTop;
    private int bodyHeight;
    /** The top of the settings band, which is where the scrolling list stops. */
    private int settingsTop;
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
        this.settings.clear();
        this.titles.clear();
        this.fixed.clear();

        int header = headerHeight(canvas);
        this.foldButton = new PixelButton(foldLabel(), PixelButton.Style.NORMAL, () -> {
            toggleFolded();
            this.relayout.run();
        });
        this.foldButton.withTooltip(Component.translatable(foldTooltipKey()));
        this.foldButton.fit(canvas);

        if (folded()) {
            this.foldButton.setBounds(this.x + (this.width - this.foldButton.width()) / 2,
                    this.y + (header - Metrics.HEADER_BUTTON) / 2,
                    this.foldButton.width(), Metrics.HEADER_BUTTON);
            this.fixed.add(addChild(this.foldButton));
            return;
        }

        int right = contentRight();
        this.foldButton.setBounds(right - this.foldButton.width(),
                this.y + (header - Metrics.HEADER_BUTTON) / 2,
                this.foldButton.width(), Metrics.HEADER_BUTTON);
        this.fixed.add(addChild(this.foldButton));

        // A button, not a bare glyph. The site's header actions are buttons, and two of
        // the interface's most used controls had no edge to aim at.
        PixelButton add = new PixelButton(Component.literal("+"), PixelButton.Style.NORMAL,
                this.onAddRequested);
        add.fit(canvas).withTooltip(Component.translatable("gui.mcskincreator.add.tooltip"));
        add.setBounds(this.foldButton.x() - add.width() - Metrics.PAD_HAIR,
                this.foldButton.y(), add.width(), Metrics.HEADER_BUTTON);
        this.fixed.add(addChild(add));

        int left = contentLeft();
        int cursorY = this.y + header + Metrics.PAD_TIGHT;

        // The model is a property of the skin, not a view of this panel — nothing below
        // it changes when it changes — so it is not a tab bar and was never going to
        // read as one. A tab that opens onto nothing is exactly what looked wrong about
        // the pair: one drawn as a black box and the other as an open frame, with no
        // body under either to make sense of them. It is one control that says what the
        // model is.
        Dropdown<PlayerModelType> model = new Dropdown<>(
                List.of(PlayerModelType.WIDE, PlayerModelType.SLIM),
                kind -> Component.translatable(modelLabelKey(kind)),
                this.project::model,
                kind -> {
                    this.history.record();
                    this.project.setModel(kind);
                    this.relayout.run();
                },
                kind -> true);
        model.setBounds(left, cursorY, right - left, Metrics.BUTTON_HEIGHT_COMPACT);
        model.inScreen(this.y + this.height);
        this.fixed.add(addChild(model));
        cursorY += Metrics.BUTTON_HEIGHT_COMPACT + Metrics.PAD_TIGHT;

        PixelButton importTexture = new PixelButton(
                Component.translatable("gui.mcskincreator.import"),
                PixelButton.Style.NORMAL, this.onImportRequested);
        importTexture.withTooltip(Component.translatable("gui.mcskincreator.import.tooltip"));
        importTexture.fitWithin(canvas, right - left);
        importTexture.setBounds(left, cursorY, right - left, Metrics.BUTTON_HEIGHT_COMPACT);
        this.fixed.add(addChild(importTexture));
        cursorY += Metrics.BUTTON_HEIGHT_COMPACT + Metrics.PAD;

        int bottom = this.y + this.height - Metrics.PANEL_INSET;
        int available = Math.max(0, bottom - cursorY);
        // The list keeps at least one row whatever the settings want, and the settings
        // take what is left over. At the sizes the editor gives itself this never
        // bites; the clamp is what stops it ever biting the list instead.
        int band = Math.min(settingsHeight(canvas), Math.max(0, available - Metrics.LAYER_ROW));

        this.bodyTop = cursorY;
        this.bodyHeight = Math.max(0, available - band);
        this.settingsTop = bottom - band;

        layoutStack(canvas, left, right);
        layoutSettings(canvas, left, right);
        // The band is clipped when it is drawn, so it is clipped for the pointer too:
        // on a window too short to hold every setting, the ones that fell off the
        // bottom are out of reach rather than merely invisible.
        for (Element control : this.settings) {
            control.clipTo(this.x, this.settingsTop, this.width, band);
        }
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

    /** How tall the settings band wants to be: its heading, four sliders and a button. */
    private int settingsHeight(Canvas canvas) {
        if (this.project.isEmpty()) {
            // No layers, so nothing to select and nothing to say about a selection. The
            // list's own empty state already says what to do.
            return 0;
        }
        // The rule that separates the band from the list is part of the band, or the
        // band's own clip cuts it off.
        int heading = 1 + Metrics.PAD_TIGHT + canvas.lineHeight() + Metrics.PAD;
        if (this.project.selected() == null) {
            return heading + canvas.lineHeight() + Metrics.PAD;
        }
        return heading + (Slider.heightFor(canvas) + Metrics.PAD_TIGHT) * 4
                + Metrics.BUTTON_HEIGHT_COMPACT + Metrics.PAD_TIGHT;
    }

    /** The settings of the selected layer, pinned at the foot of the column. */
    private void layoutSettings(Canvas canvas, int left, int right) {
        Layer layer = this.project.selected();
        if (layer == null) {
            // Nothing to set, so nothing is laid out: the band says why in its own place.
            return;
        }
        int cursorY = this.settingsTop + 1 + Metrics.PAD_TIGHT
                + canvas.lineHeight() + Metrics.PAD;
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
        reset.withTooltip(Component.translatable("gui.mcskincreator.reset_colors.tooltip"));
        reset.fitWithin(canvas, width);
        reset.setBounds(left, cursorY, width, Metrics.BUTTON_HEIGHT_COMPACT);
        this.settings.add(addChild(reset));
        this.fixed.add(reset);
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
        this.settings.add(addChild(slider));
        this.fixed.add(slider);
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
            if (!this.settings.contains(element)) {
                element.draw(paint);
            }
        }
        if (folded()) {
            return;
        }
        int left = contentLeft();
        int right = contentRight();
        int railRight = right - ScrollPane.BAR_WIDTH - Metrics.PAD_TIGHT;

        place();
        canvas.pushScissor(left, this.bodyTop, right - left, this.bodyHeight);
        int offset = this.bodyTop - this.scroll.offset();

        if (this.rows.isEmpty()) {
            // One sentence, not two. With no layers there is nothing to select either,
            // so "choose a layer to adjust it" says the same thing a second time — and
            // said it in the same place, one on top of the other.
            drawEmptyState(canvas, Component.translatable("empty.mcskincreator.layers"),
                    left, railRight, this.bodyTop, this.bodyHeight);
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
        canvas.popScissor();
        this.scroll.drawBar(canvas, right, this.bodyTop, this.bodyHeight);

        // Its own band, and clipped to it: the settings are drawn after the list and
        // must never be able to reach into it, whatever the column's height turned out
        // to be.
        // Clipped vertically only. Sideways it gets the whole panel: a value is right
        // aligned on the content's own edge, and a scissor drawn to that same edge
        // takes the last column of the last glyph with it.
        canvas.pushScissor(this.x, this.settingsTop, this.width,
                this.y + this.height - Metrics.PANEL_INSET - this.settingsTop);
        drawSettings(paint, left, right, this.settingsTop);
        canvas.popScissor();
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
        // The list runs under this band rather than stopping at it, so the band needs a
        // hard edge. A row cut off by a grey hairline alone reads as a broken row; cut
        // off by black, it reads as a row that carries on underneath.
        canvas.fill(this.x + Metrics.OUTLINE, top, this.width - Metrics.OUTLINE * 2, 1,
                Palette.OUTLINE);
            Surface.rule(canvas, left, top + 1, Math.max(0, right - left));
            canvas.textWrapped(Component.translatable("empty.mcskincreator.inspector"),
                    left, top + Metrics.PAD, Math.max(1, right - left), Palette.INK_MUTED);
            return;
        }

        // Ruled off from the list above it: these settings belong to one row of that
        // list, and without a line the heading reads as one more group of it.
        // The list runs under this band rather than stopping at it, so the band needs a
        // hard edge. A row cut off by a grey hairline alone reads as a broken row; cut
        // off by black, it reads as a row that carries on underneath.
        canvas.fill(this.x + Metrics.OUTLINE, top, this.width - Metrics.OUTLINE * 2, 1,
                Palette.OUTLINE);
        Surface.rule(canvas, left, top + 1, Math.max(0, right - left));

        int titleY = top + 1 + Metrics.PAD_TIGHT;
        String title = Component.translatable("gui.mcskincreator.settings").getString()
                .toUpperCase(Locale.ROOT);
        canvas.textTracked(title, left, titleY, Palette.INK, Metrics.TITLE_TRACKING);
        int titleWidth = canvas.trackedWidth(title, Metrics.TITLE_TRACKING);
        int nameRoom = Math.max(0, right - left - titleWidth - Metrics.PAD);
        canvas.text(Component.literal(fr.clixmods.mcsc.mod.ui.Marquee.cut(
                        canvas, layer.name().getString(), nameRoom)),
                left + titleWidth + Metrics.PAD, titleY, Palette.INK);

        for (Element element : this.settings) {
            element.draw(paint);
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

    /** Moves the rows to where the offset now puts them. */
    private void place() {
        int offset = this.bodyTop - this.scroll.offset();
        for (PlacedRow placed : this.rows) {
            LayerRow row = placed.row();
            row.setBounds(row.x(), offset + placed.contentY(), row.width(), row.height());
            row.clipTo(this.x, this.bodyTop, this.width, this.bodyHeight);
            row.setDragging(this.dragged == row.layer());
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
        return targets;
    }

    @Override
    public boolean scroll(double mouseX, double mouseY, double amount) {
        if (!inBody(mouseX, mouseY, this.bodyTop, this.bodyHeight)) {
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
