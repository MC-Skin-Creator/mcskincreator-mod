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
import java.util.function.Supplier;

import fr.clixmods.mcsc.mod.project.History;
import fr.clixmods.mcsc.mod.style.Metrics;
import fr.clixmods.mcsc.mod.style.Palette;
import fr.clixmods.mcsc.mod.style.Surface;
import fr.clixmods.mcsc.mod.ui.Canvas;
import fr.clixmods.mcsc.mod.ui.Element;
import fr.clixmods.mcsc.mod.ui.Paint;
import fr.clixmods.mcsc.mod.ui.widget.PixelButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * The strip across the top: the mark, the beta badge, and the tools.
 *
 * <p>Export sits at the right end and stays there: it is the control that finishes the
 * job, and one that finishes the job belongs at the end of the row rather than floating
 * in the middle of it, where it moved every time a neighbour appeared. Everything else
 * runs from the mark leftwards, and when the window is too narrow the buttons furthest
 * from either end are dropped rather than wrapped onto a second row the bar has not got.
 *
 * <p>Two of the site's controls are deliberately absent. The random outfit has nothing
 * behind it yet, and a button whose target is empty disappears instead of opening onto
 * nothing — which is also why the models button only appears once the catalogue turns
 * out to carry some. The language picker is gone for a different reason: in the game
 * the language is the game's, and the mod follows it.
 */
public class TopBar extends Element {
    /**
     * The mark's picture: its texture and the size of the file it came from.
     *
     * <p>The icon ships far larger than the 16 pixels it is drawn at, so whatever draws
     * it has to sample the whole image rather than a corner of it — which means the bar
     * needs the source size along with the texture.
     */
    public record Mark(Identifier texture, int size) {
    }

    /** The mark, drawn from the icon the mod already ships. */
    private static final int LOGO_SIZE = 16;

    private final History history;
    private final Supplier<Mark> logo;
    private final Runnable onNew;
    private final Runnable onModels;
    private final Runnable onExport;
    private final Runnable onSkins;
    private final Runnable onAbout;
    private final BooleanSupplier hasModels;

    private final List<Element> children = new ArrayList<>();
    private final List<Integer> separators = new ArrayList<>();

    /**
     * @param logo the mark to draw, or null for none. Asked for rather than looked up,
     *             so that the bar can be laid out and painted where there is no game to
     *             look it up in.
     */
    public TopBar(History history, Supplier<Mark> logo, Runnable onNew, Runnable onModels,
                  BooleanSupplier hasModels, Runnable onExport, Runnable onSkins,
                  Runnable onAbout) {
        this.history = history;
        this.logo = logo;
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

        Brand brand = new Brand(this.logo, this.onAbout);
        brand.fit(canvas);
        int buttonY = this.y + (this.height - Metrics.BUTTON_HEIGHT_COMPACT) / 2;
        brand.setBounds(this.x + Metrics.PAD_TIGHT, this.y + (this.height - LOGO_SIZE) / 2,
                brand.width(), LOGO_SIZE);
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

        // Export is pinned to the right end of the bar and nothing else goes there.
        // It is the one control that finishes the job, and a control that finishes the
        // job belongs at the end of the row rather than somewhere in the middle of it,
        // where it moved every time a neighbour appeared or went away.
        int right = this.x + this.width - Metrics.PAD_TIGHT;
        export.setBounds(right - export.width(), buttonY, export.width(),
                Metrics.BUTTON_HEIGHT_COMPACT);
        this.children.add(export);
        right -= export.width() + Metrics.PAD;
        this.separators.add(right + Metrics.PAD / 2);

        List<PixelButton> starting = new ArrayList<>(this.hasModels.getAsBoolean()
                ? List.of(fresh, models, skins) : List.of(fresh, skins));
        List<List<PixelButton>> groups = List.of(starting, List.of(undo, redo));

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

    /** The badge is small: it says what the product is, not what to do next. */
    private int betaWidth(Canvas canvas) {
        return canvas.smallTextWidth(Component.translatable("gui.mcskincreator.beta"))
                + Metrics.PAD_HAIR * 2;
    }

    @Override
    public void draw(Paint paint) {
        Canvas canvas = paint.canvas();
        // The site's header strip: one flat dark band, ruled off from the columns that
        // hang under it. Not the panel material — a panel's frame at this height is
        // frame all the way through.
        Surface.flat(canvas, this.x, this.y, this.width, this.height, Palette.PANEL_HEADER);
        canvas.fill(this.x, this.y + this.height - 1, this.width, 1, Palette.OUTLINE);

        Element brand = this.children.get(0);
        int badgeHeight = canvas.smallLineHeight() + Metrics.PAD_HAIR * 2;
        int badgeX = brand.x() + brand.width() + Metrics.PAD_TIGHT;
        int badgeY = this.y + (this.height - badgeHeight) / 2;
        Surface.slot(canvas, badgeX, badgeY, betaWidth(canvas), badgeHeight);
        canvas.textSmall(Component.translatable("gui.mcskincreator.beta"),
                badgeX + Metrics.PAD_HAIR, badgeY + Metrics.PAD_HAIR, Palette.INK_MUTED);

        // A separator is the hairline the game rules its own headings with, stood on end.
        for (int separatorX : this.separators) {
            canvas.fill(separatorX, this.y + Metrics.PAD_TIGHT, 1,
                    this.height - Metrics.PAD_TIGHT * 2, Palette.RULE);
        }

        for (Element child : this.children) {
            child.draw(paint);
        }
    }

    /**
     * The mark: the mod's own icon and the product name, together one button that opens
     * About.
     *
     * <p>The icon is the one in {@code assets/mcskincreator/icon.png} — the same mark
     * the game's mod list shows. There is no second logo drawn for this screen, because
     * there is no second MC Skin Creator.
     */
    private static final class Brand extends Element {
        private final Supplier<Mark> logo;
        private final Runnable action;

        private Brand(Supplier<Mark> logo, Runnable action) {
            this.logo = logo;
            this.action = action;
        }

        void fit(Canvas canvas) {
            this.width = LOGO_SIZE + Metrics.PAD_TIGHT
                    + canvas.textWidth(Component.translatable("gui.mcskincreator.brand"));
        }

        @Override
        public void draw(Paint paint) {
            Canvas canvas = paint.canvas();
            boolean hot = paint.hot(this);

            Mark mark = this.logo.get();
            if (mark != null && mark.texture() != null && mark.size() > 0) {
                // The whole icon, scaled down to the mark: the file is 128 px and the
                // mark is 16, so the source rectangle is the image, not a corner of it.
                int source = mark.size();
                canvas.blit(mark.texture(), this.x, this.y, LOGO_SIZE, LOGO_SIZE,
                        0.0F, 0.0F, source, source, source, source);
            }
            canvas.text(Component.translatable("gui.mcskincreator.brand"),
                    this.x + LOGO_SIZE + Metrics.PAD_TIGHT,
                    this.y + (LOGO_SIZE - canvas.lineHeight()) / 2,
                    hot ? Palette.INK_HOVERED : Palette.INK);
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
            this.action.run();
            return true;
        }

        @Override
        public List<Component> tooltip() {
            return List.of(Component.translatable("gui.mcskincreator.brand.tooltip"));
        }
    }
}
