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
import java.util.function.Supplier;

import fr.clixmods.mcsc.mod.skin.FrontSprite;
import fr.clixmods.mcsc.mod.skin.PreviewSkin;
import fr.clixmods.mcsc.mod.style.Metrics;
import fr.clixmods.mcsc.mod.style.Palette;
import fr.clixmods.mcsc.mod.style.Surface;
import fr.clixmods.mcsc.mod.ui.Canvas;
import fr.clixmods.mcsc.mod.ui.Element;
import fr.clixmods.mcsc.mod.ui.Paint;
import fr.clixmods.mcsc.mod.ui.widget.Dropdown;
import fr.clixmods.mcsc.mod.ui.widget.PixelButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PlayerSkinWidget;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;

/**
 * The middle column: the player, and the few controls that belong on top of them.
 *
 * <p>This is the one panel that never goes away. Two other arrangements were tried on
 * the site and dropped — stacking the three panels left the preview 21 pixels, and
 * swapping them through a single slot made the model vanish the moment the library
 * opened. Whatever the width, the scene shrinks and stays.
 *
 * <p>The model is the game's own {@link PlayerSkinWidget}: it already renders a
 * player, already turns under the mouse, and already follows the classic or slim
 * model of the skin it is handed. It is drawn from here rather than added to the
 * screen as a widget, so that it lands in the middle of this interface's own paint
 * order — under the tool strips that float over it, and beside rather than beneath the
 * two panels.
 *
 * <p>The settings bar floats over the view when the model has the scene to itself,
 * lets the pointer through everywhere but its own controls, and rings its labels in
 * black instead of merely shadowing them: a shadow is enough over a flat panel and not
 * enough over a figure.
 */
public class ScenePanel extends Element {
    /** How the middle column is showing the skin. */
    public enum View {
        MODEL("model"),
        TEXTURE("texture"),
        BOTH("both");

        private final String id;

        View(String id) {
            this.id = id;
        }

        public String labelKey() {
            return "view.mcskincreator." + this.id;
        }

        public String tooltipKey() {
            return labelKey() + ".tooltip";
        }
    }

    /** Vanilla's proportions for a player portrait, from the skin customisation screen. */
    private static final int PORTRAIT_WIDTH = 85;
    private static final int PORTRAIT_HEIGHT = 120;

    private final PreviewSkin preview;
    private final Runnable relayout;
    private final Supplier<Component> hoveredLabel;
    private final List<Element> controls = new ArrayList<>();

    private View view = View.MODEL;
    private PlayerSkinWidget model;
    /** The bounds the current widget was built for, so it is only rebuilt when they move. */
    private int[] modelBounds = {0, 0, 0, 0};
    private int[] dock = {0, 0, 0, 0};
    private int[] viewport = {0, 0, 0, 0};

    public ScenePanel(PreviewSkin preview, Runnable relayout, Supplier<Component> hoveredLabel) {
        this.preview = preview;
        this.relayout = relayout;
        this.hoveredLabel = hoveredLabel;
    }

    public View view() {
        return this.view;
    }

    public List<Element> controls() {
        return this.controls;
    }

    /** True while the model has the scene to itself, which is when the bar floats. */
    private boolean barFloats() {
        return this.view == View.MODEL;
    }

    public void layout(Canvas canvas) {
        this.controls.clear();

        int barHeight = Metrics.BUTTON_HEIGHT_COMPACT + Metrics.PAD_TIGHT * 2;
        int cursorX = this.x + Metrics.PAD_TIGHT;
        int barY = this.y + Metrics.PAD_TIGHT;

        List<PixelButton> viewButtons = new ArrayList<>();
        int segmentedWidth = 0;
        for (View candidate : View.values()) {
            PixelButton button = new PixelButton(Component.translatable(candidate.labelKey()),
                    PixelButton.Style.NORMAL, () -> {
                        this.view = candidate;
                        this.relayout.run();
                    });
            button.fit(canvas).setActive(this.view == candidate);
            button.withTooltip(Component.translatable(candidate.tooltipKey()));
            viewButtons.add(button);
            segmentedWidth += button.width() + Metrics.SEGMENT_GAP;
        }

        // Three buttons side by side is the site's segmented group, and it is what this
        // shows whenever the scene is wide enough for it. Below that width the same
        // choice becomes a dropdown rather than spilling off the bar: a control that
        // runs past the edge of its strip is a control nobody can reach.
        if (segmentedWidth + Metrics.ui(90) <= this.width) {
            for (PixelButton button : viewButtons) {
                button.setBounds(cursorX, barY, button.width(), Metrics.BUTTON_HEIGHT_COMPACT);
                this.controls.add(button);
                cursorX += button.width() + Metrics.SEGMENT_GAP;
            }
        } else {
            Dropdown<View> chooser = new Dropdown<>(List.of(View.values()),
                    candidate -> Component.translatable(candidate.labelKey()),
                    () -> this.view,
                    candidate -> {
                        this.view = candidate;
                        this.relayout.run();
                    },
                    candidate -> true);
            int chooserWidth = Math.min(Metrics.ui(150), Math.max(1, this.width - Metrics.PAD_TIGHT * 2));
            chooser.setBounds(cursorX, barY, chooserWidth, Metrics.BUTTON_HEIGHT_COMPACT);
            chooser.inScreen(this.y + this.height);
            this.controls.add(chooser);
        }

        int viewTop = barFloats() ? this.y : this.y + barHeight;
        int viewHeight = Math.max(0, this.height - (viewTop - this.y));
        this.viewport = new int[] {this.x, viewTop, this.width, viewHeight};

        layoutModel(viewTop, viewHeight);
        layoutDock(canvas);
    }

    /**
     * Sizes the portrait so the player stays whole.
     *
     * <p>Whichever of width and height runs out first decides, so the figure keeps its
     * proportions at any window size and any of the four GUI scales.
     */
    private void layoutModel(int viewTop, int viewHeight) {
        int space = this.view == View.BOTH ? this.width / 2 : this.width;
        int inset = Metrics.PAD;
        int usableWidth = Math.max(0, space - inset * 2);
        int usableHeight = Math.max(0, viewHeight - inset * 2);

        int height = Math.min(usableHeight, usableWidth * PORTRAIT_HEIGHT / Math.max(1, PORTRAIT_WIDTH));
        int width = height * PORTRAIT_WIDTH / PORTRAIT_HEIGHT;
        if (width <= 0 || height <= 0) {
            this.model = null;
            return;
        }

        int left = this.x + (space - width) / 2;
        int top = viewTop + (viewHeight - height) / 2;
        int[] wanted = {left, top, width, height};

        // The widget carries the figure's rotation, and layout runs on every pick. Only
        // a change of size or position builds a new one, so stacking an element does
        // not quietly spin the player back to facing forward.
        if (this.model == null || !java.util.Arrays.equals(this.modelBounds, wanted)) {
            Minecraft client = Minecraft.getInstance();
            this.model = new PlayerSkinWidget(width, height,
                    client.getEntityModels(), this.preview::playerSkin);
            this.modelBounds = wanted;
        }
        this.model.setX(left);
        this.model.setY(top);
    }

    /**
     * The animation dock, bottom right.
     *
     * <p>It carries the one control that has something behind it: putting the figure
     * back where it started. The site also puts a play button and an animation chooser
     * here; there is nothing to play until the model can be posed, and a control whose
     * target is empty disappears rather than opening onto nothing.
     */
    private void layoutDock(Canvas canvas) {
        PixelButton recentre = new PixelButton(
                Component.translatable("gui.mcskincreator.recentre"),
                PixelButton.Style.GHOST, this::recentre);
        recentre.fit(canvas);
        recentre.withTooltip(Component.translatable("gui.mcskincreator.recentre.tooltip"));

        int dockWidth = recentre.width() + Metrics.PAD * 2;
        int dockHeight = Metrics.BUTTON_HEIGHT_COMPACT + Metrics.PAD * 2;
        int dockX = this.x + this.width - Metrics.PAD_TIGHT - dockWidth;
        int dockY = this.y + this.height - Metrics.PAD_TIGHT - dockHeight;

        recentre.setBounds(dockX + Metrics.PAD, dockY + Metrics.PAD,
                recentre.width(), Metrics.BUTTON_HEIGHT_COMPACT);
        this.controls.add(recentre);
        this.dock = new int[] {dockX, dockY, dockWidth, dockHeight};
    }

    /** Throws the widget away, which is what puts the figure back facing forward. */
    private void recentre() {
        this.model = null;
        this.relayout.run();
    }

    @Override
    public void draw(Paint paint) {
        draw(paint, 0.0F);
    }

    public void draw(Paint paint, float delta) {
        Canvas canvas = paint.canvas();
        Surface.dark(canvas, this.x, this.y, this.width, this.height, Palette.EMPTY);

        switch (this.view) {
            case MODEL -> drawModel(canvas, paint, delta);
            case TEXTURE -> drawTexture(canvas, this.x, this.viewport[1], this.width, this.viewport[3]);
            case BOTH -> {
                drawModel(canvas, paint, delta);
                int half = this.width / 2;
                canvas.fill(this.x + half, this.viewport[1], Metrics.OUTLINE, this.viewport[3],
                        Palette.OUTLINE);
                drawTexture(canvas, this.x + half, this.viewport[1], this.width - half, this.viewport[3]);
            }
        }

        if (!barFloats()) {
            Surface.dark(canvas, this.x, this.y, this.width,
                    Metrics.BUTTON_HEIGHT_COMPACT + Metrics.PAD_TIGHT * 2, Palette.DARK);
        }
        for (Element control : this.controls) {
            control.draw(paint);
        }

        drawDock(canvas);
        drawCorner(canvas);
    }

    private void drawModel(Canvas canvas, Paint paint, float delta) {
        if (this.model != null) {
            canvas.widget(this.model, paint.mouseX(), paint.mouseY(), delta);
        }
    }

    /** The 64x64 sheet itself, on a transparency checker, at a whole scale. */
    private void drawTexture(Canvas canvas, int left, int top, int width, int height) {
        if (!this.preview.hasTexture()) {
            return;
        }
        int size = FrontSprite.SKIN_SIZE;
        int scale = Math.max(1, Math.min((width - Metrics.PAD * 2) / size,
                (height - Metrics.PAD * 2) / size));
        int drawnX = left + (width - size * scale) / 2;
        int drawnY = top + (height - size * scale) / 2;
        Surface.checker(canvas, drawnX, drawnY, size * scale, size * scale);
        canvas.blit(this.preview.texture(), drawnX, drawnY, size * scale, size * scale,
                0, 0, size, size, size, size);
        Surface.outline(canvas, drawnX - Metrics.OUTLINE, drawnY - Metrics.OUTLINE,
                size * scale + Metrics.OUTLINE * 2, size * scale + Metrics.OUTLINE * 2);
    }

    private void drawDock(Canvas canvas) {
        Surface.dark(canvas, this.dock[0], this.dock[1], this.dock[2], this.dock[3], Palette.DARK);
        Surface.bevel(canvas, this.dock[0], this.dock[1], this.dock[2], this.dock[3],
                Palette.PANEL_TOP, Palette.PANEL_BOTTOM, Palette.PANEL_MID, Metrics.BEVEL);
    }

    /**
     * Bottom left: the preview label above the reminder of what the gestures are.
     *
     * <p>Both are laid straight on the scene, so the text is ringed in black on all
     * four sides. A shadow is enough over a flat panel and not enough over a figure.
     */
    private void drawCorner(Canvas canvas) {
        List<Component> lines = new ArrayList<>();
        Component hovered = this.hoveredLabel.get();
        if (hovered != null) {
            lines.add(hovered);
        }
        lines.add(Component.translatable("gesture.mcskincreator.turn"));

        int lineHeight = canvas.lineHeight() + Metrics.PAD_TIGHT;
        int boxHeight = lines.size() * lineHeight + Metrics.PAD_TIGHT;
        int boxWidth = 0;
        for (Component line : lines) {
            boxWidth = Math.max(boxWidth, canvas.textWidth(line));
        }
        boxWidth += Metrics.PAD * 2;

        int boxX = this.x + Metrics.PAD_TIGHT;
        int boxY = this.y + this.height - Metrics.PAD_TIGHT - boxHeight;
        Surface.dark(canvas, boxX, boxY, boxWidth, boxHeight, Palette.DARK);
        Surface.bevel(canvas, boxX, boxY, boxWidth, boxHeight,
                Palette.PANEL_TOP, Palette.PANEL_BOTTOM, Palette.PANEL_MID, Metrics.BEVEL);

        for (int index = 0; index < lines.size(); index++) {
            boolean isLabel = hovered != null && index == 0;
            canvas.textRinged(lines.get(index), boxX + Metrics.PAD,
                    boxY + Metrics.PAD_TIGHT + index * lineHeight,
                    isLabel ? Palette.GOLD : Palette.INK_MUTED);
        }
    }

    @Override
    public boolean mouseDown(double mouseX, double mouseY, int button) {
        // The floating bar lets the pointer through everywhere but its own controls,
        // so a drag that starts on the scene behind it still reaches the figure.
        return this.model != null && button == 0 && contains(mouseX, mouseY)
                && this.model.mouseClicked(mouseEvent(mouseX, mouseY, button), false);
    }

    @Override
    public void mouseDrag(double mouseX, double mouseY, double dragX, double dragY, int button) {
        if (this.model != null) {
            // The figure turns by how far the mouse moved, so the deltas are what
            // matter here rather than where the pointer ended up.
            this.model.mouseDragged(mouseEvent(mouseX, mouseY, button), dragX, dragY);
        }
    }

    @Override
    public void mouseUp(double mouseX, double mouseY, int button) {
        if (this.model != null) {
            this.model.mouseReleased(mouseEvent(mouseX, mouseY, button));
        }
    }

    private static MouseButtonEvent mouseEvent(double mouseX, double mouseY, int button) {
        return new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, 0));
    }
}
