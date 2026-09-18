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
import java.util.function.BooleanSupplier;

import fr.clixmods.mcsc.mod.project.History;
import fr.clixmods.mcsc.mod.style.Metrics;
import fr.clixmods.mcsc.mod.style.Palette;
import fr.clixmods.mcsc.mod.style.Surface;
import fr.clixmods.mcsc.mod.ui.Canvas;
import fr.clixmods.mcsc.mod.ui.Element;
import fr.clixmods.mcsc.mod.ui.Icons;
import fr.clixmods.mcsc.mod.ui.Paint;
import fr.clixmods.mcsc.mod.ui.widget.PixelButton;
import net.minecraft.network.chat.Component;

/**
 * The strip across the top: the mark, the beta badge, and the tools.
 *
 * <p>Export is the only green button up here, because it is the only thing this bar
 * is really for. When the window is too narrow for everything, the order changes
 * rather than the buttons wrapping: export, then undo and redo, go first — those are
 * the ones nobody should have to hunt for.
 *
 * <p>Two of the site's controls are deliberately absent. The random outfit has nothing
 * behind it yet, and a button whose target is empty disappears instead of opening onto
 * nothing — which is also why the models button only appears once the catalogue turns
 * out to carry some. The language picker is gone for a different reason: in the game
 * the language is the game's, and the mod follows it.
 */
public class TopBar extends Element {
    private final History history;
    private final Runnable onNew;
    private final Runnable onModels;
    private final Runnable onExport;
    private final Runnable onSkins;
    private final Runnable onAbout;
    private final BooleanSupplier hasModels;

    private final List<Element> children = new ArrayList<>();
    private final List<Integer> separators = new ArrayList<>();

    public TopBar(History history, Runnable onNew, Runnable onModels, BooleanSupplier hasModels,
                  Runnable onExport, Runnable onSkins, Runnable onAbout) {
        this.history = history;
        this.onNew = onNew;
        this.onModels = onModels;
        this.hasModels = hasModels;
        this.onExport = onExport;
        this.onSkins = onSkins;
        this.onAbout = onAbout;
        this.height = Metrics.TOP_BAR_HEIGHT;
    }

    public List<Element> children() {
        return this.children;
    }

    public void layout(Canvas canvas) {
        this.children.clear();
        this.separators.clear();

        Brand brand = new Brand(this.onAbout);
        brand.fit(canvas);
        int buttonY = this.y + (this.height - Metrics.BUTTON_HEIGHT_COMPACT) / 2;
        brand.setBounds(this.x + Metrics.PAD_TIGHT, this.y + (this.height - Icons.SIZE) / 2,
                brand.width(), Icons.SIZE);
        this.children.add(brand);

        int cursorX = brand.x() + brand.width() + Metrics.PAD_TIGHT;
        cursorX += betaWidth(canvas) + Metrics.PAD;

        PixelButton export = new PixelButton(Component.translatable("gui.mcskincreator.export"),
                PixelButton.Style.PRIMARY, this.onExport)
                .withTooltip(Component.translatable("gui.mcskincreator.export.tooltip"));
        PixelButton undo = new PixelButton(Component.translatable("gui.mcskincreator.undo"),
                PixelButton.Style.NORMAL, () -> this.history.undo())
                .withTooltip(Component.translatable("gui.mcskincreator.undo.tooltip"));
        PixelButton redo = new PixelButton(Component.translatable("gui.mcskincreator.redo"),
                PixelButton.Style.NORMAL, () -> this.history.redo())
                .withTooltip(Component.translatable("gui.mcskincreator.redo.tooltip"));
        PixelButton fresh = new PixelButton(Component.translatable("gui.mcskincreator.new"),
                PixelButton.Style.NORMAL, this.onNew)
                .withTooltip(Component.translatable("gui.mcskincreator.new.tooltip"));
        PixelButton skins = new PixelButton(Component.translatable("gui.mcskincreator.skins"),
                PixelButton.Style.NORMAL, this.onSkins)
                .withTooltip(Component.translatable("gui.mcskincreator.skins.tooltip"));
        PixelButton models = new PixelButton(Component.translatable("gui.mcskincreator.models"),
                PixelButton.Style.NORMAL, this.onModels)
                .withTooltip(Component.translatable("gui.mcskincreator.models.tooltip"));
        undo.setEnabled(this.history.canUndo());
        redo.setEnabled(this.history.canRedo());
        List<PixelButton> all = new ArrayList<>(List.of(export, undo, redo, fresh, skins));
        if (this.hasModels.getAsBoolean()) {
            all.add(models);
        }
        for (PixelButton button : all) {
            button.fit(canvas);
        }

        // Roomy: the site's order, grouped by separators. Cramped: the three controls
        // one must never hunt for come first, and the rest take what is left.
        List<PixelButton> starting = this.hasModels.getAsBoolean()
                ? List.of(fresh, models, skins) : List.of(fresh, skins);
        List<List<PixelButton>> groups = fits(cursorX, all)
                ? List.of(starting, List.of(undo, redo), List.of(export))
                : List.of(List.of(export), List.of(undo, redo), starting);

        int right = this.x + this.width - Metrics.PAD_TIGHT;
        boolean firstGroup = true;
        for (List<PixelButton> group : groups) {
            if (group.isEmpty()) {
                continue;
            }
            // A separator stands between two groups, so the leading one has none.
            if (!firstGroup) {
                this.separators.add(cursorX);
                cursorX += Metrics.PAD;
            }
            firstGroup = false;
            for (PixelButton button : group) {
                if (cursorX + button.width() > right) {
                    // No room left: the button is dropped rather than drawn off the edge
                    // or wrapped onto a second row the bar does not have.
                    continue;
                }
                button.setBounds(cursorX, buttonY, button.width(), Metrics.BUTTON_HEIGHT_COMPACT);
                this.children.add(button);
                cursorX += button.width() + Metrics.SEGMENT_GAP;
            }
        }
    }

    private boolean fits(int from, List<PixelButton> buttons) {
        int total = from;
        for (PixelButton button : buttons) {
            total += button.width() + Metrics.SEGMENT_GAP;
        }
        return total + Metrics.PAD * 3 <= this.x + this.width;
    }

    private int betaWidth(Canvas canvas) {
        return canvas.textWidth(Component.translatable("gui.mcskincreator.beta")) + Metrics.PAD;
    }

    @Override
    public void draw(Paint paint) {
        Canvas canvas = paint.canvas();
        Surface.dark(canvas, this.x, this.y, this.width, this.height, Palette.DARK);

        Element brand = this.children.get(0);
        int badgeX = brand.x() + brand.width() + Metrics.PAD_TIGHT;
        int badgeY = this.y + (this.height - canvas.lineHeight() - Metrics.PAD_TIGHT) / 2;
        Surface.slot(canvas, badgeX, badgeY, betaWidth(canvas),
                canvas.lineHeight() + Metrics.PAD_TIGHT, Palette.SLOT);
        canvas.textFlat(Component.translatable("gui.mcskincreator.beta"),
                badgeX + Metrics.PAD_TIGHT, badgeY + Metrics.PAD_TIGHT / 2, Palette.GOLD);

        // A separator is a black rule with a light reflection down its right side —
        // the same two-tone trick the bevels use, at one pixel.
        for (int separatorX : this.separators) {
            canvas.fill(separatorX, this.y + Metrics.PAD_TIGHT, Metrics.OUTLINE,
                    this.height - Metrics.PAD_TIGHT * 2, Palette.OUTLINE);
            canvas.fill(separatorX + Metrics.OUTLINE, this.y + Metrics.PAD_TIGHT, 1,
                    this.height - Metrics.PAD_TIGHT * 2, Palette.PANEL_MID);
        }

        for (Element child : this.children) {
            child.draw(paint);
        }
    }

    /**
     * The mark: the logo and the product name, together one button that opens About.
     *
     * <p>It carries no material and no bevel — it is not meant to look like a button —
     * but it lights up gold like everything else you can press.
     */
    private static final class Brand extends Element {
        private final Runnable action;

        private Brand(Runnable action) {
            this.action = action;
        }

        void fit(Canvas canvas) {
            this.width = Icons.SIZE + Metrics.PAD_TIGHT
                    + canvas.textWidth(Component.translatable("gui.mcskincreator.brand"));
        }

        @Override
        public void draw(Paint paint) {
            Canvas canvas = paint.canvas();
            boolean hot = paint.hot(this);
            Icons.draw(canvas, "logo", this.x, this.y, 1);
            canvas.text(Component.translatable("gui.mcskincreator.brand"),
                    this.x + Icons.SIZE + Metrics.PAD_TIGHT,
                    this.y + (Icons.SIZE - canvas.lineHeight()) / 2,
                    hot ? Palette.GOLD : Palette.INK);
        }

        @Override
        public boolean mouseDown(double mouseX, double mouseY, int button) {
            return button == 0 && contains(mouseX, mouseY) && activate();
        }

        @Override
        public boolean activate() {
            this.action.run();
            return true;
        }

        @Override
        public List<Component> tooltip() {
            return List.of(Component.translatable("gui.mcskincreator.brand.tooltip"));
        }
    }
}
