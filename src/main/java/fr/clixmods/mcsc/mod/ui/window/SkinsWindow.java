/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui.window;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import fr.clixmods.mcsc.mod.remote.SavedSkin;
import fr.clixmods.mcsc.mod.skin.SkinThumbnails;
import fr.clixmods.mcsc.mod.style.Metrics;
import fr.clixmods.mcsc.mod.style.Palette;
import fr.clixmods.mcsc.mod.style.Surface;
import fr.clixmods.mcsc.mod.ui.Canvas;
import fr.clixmods.mcsc.mod.ui.Element;
import fr.clixmods.mcsc.mod.ui.Marquee;
import fr.clixmods.mcsc.mod.ui.Paint;
import fr.clixmods.mcsc.mod.ui.widget.PixelButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * The player's library: the skins kept on the server, and what can be done with them.
 *
 * <p>The list is the server's, in the server's order — last created first — and the
 * picture on each row is the one the server composed when it stored that skin. Nothing
 * is recomposed to draw this window.
 *
 * <p>A row is opened by clicking it and removed by the cross on its right, which is
 * drawn at rest rather than revealed on hover: what can be pressed is visible, and a
 * removal that can be reached without being seen is how a skin goes missing.
 */
public class SkinsWindow extends ModalWindow {
    /** The height of a row: the front view at two pixels per texel, plus its padding. */
    private static final int ROW = 32 * SkinThumbnails.SCALE + Metrics.PAD_TIGHT * 2 + Metrics.SLOT_INSET * 2;
    private static final int PICTURE_WIDTH = 16 * SkinThumbnails.SCALE;
    private static final int PICTURE_HEIGHT = 32 * SkinThumbnails.SCALE;

    private static final DateTimeFormatter DAY = DateTimeFormatter.ISO_LOCAL_DATE;

    /**
     * What the window has to show right now.
     *
     * @param failure what went wrong, empty while nothing has. It stays on screen for
     *                as long as the problem lasts, where a notification would have gone
     *                — this is where one looks to find out what is still wrong.
     */
    public record Library(List<SavedSkin> skins, boolean loading, Component failure) {
        public static final Library LOADING = new Library(List.of(), true, Component.empty());
    }

    private final Supplier<Library> library;
    private final SkinThumbnails thumbnails;
    private final Consumer<SavedSkin> onOpen;
    private final Consumer<SavedSkin> onDelete;
    private final Runnable onSave;

    public SkinsWindow(Supplier<Library> library, SkinThumbnails thumbnails,
                       Consumer<SavedSkin> onOpen, Consumer<SavedSkin> onDelete, Runnable onSave) {
        super("window.mcskincreator.skins", null);
        this.library = library;
        this.thumbnails = thumbnails;
        this.onOpen = onOpen;
        this.onDelete = onDelete;
        this.onSave = onSave;
    }

    private List<SavedSkin> skins() {
        return this.library.get().skins();
    }

    @Override
    protected int contentHeight(Canvas canvas) {
        List<SavedSkin> skins = skins();
        if (skins.isEmpty()) {
            return canvas.lineHeight() + Metrics.PAD * 2;
        }
        return skins.size() * (ROW + Metrics.PAD_TIGHT);
    }

    @Override
    protected void layoutBody(Canvas canvas, int left, int top, int width) {
        int cursorY = top;
        for (SavedSkin skin : skins()) {
            SkinRow row = new SkinRow(skin, this.thumbnails, this.onOpen);
            row.setBounds(left, cursorY, width, ROW);
            addBodyChild(row);

            PixelButton remove = new PixelButton(Component.literal("x"), PixelButton.Style.NORMAL,
                    () -> this.onDelete.accept(skin))
                    .withTooltip(Component.translatable("skins.mcskincreator.delete.tooltip"));
            remove.fit(canvas);
            remove.setBounds(left + width - Metrics.PAD_TIGHT - remove.width(),
                    cursorY + (ROW - Metrics.BUTTON_HEIGHT_COMPACT) / 2,
                    remove.width(), Metrics.BUTTON_HEIGHT_COMPACT);
            addBodyChild(remove);
            row.setActionsWidth(remove.width() + Metrics.PAD_TIGHT * 2);

            cursorY += ROW + Metrics.PAD_TIGHT;
        }
    }

    @Override
    protected void drawBody(Paint paint, int left, int top, int width) {
        Library state = this.library.get();
        if (!state.skins().isEmpty()) {
            // The rows are children, so the window draws them with everything else.
            return;
        }
        Component message = !state.failure().getString().isEmpty()
                ? state.failure()
                : Component.translatable(state.loading()
                        ? "skins.mcskincreator.loading"
                        : "skins.mcskincreator.empty");
        paint.canvas().textWrapped(message, left, top + Metrics.PAD, width,
                state.failure().getString().isEmpty() ? Palette.INK_MUTED : Palette.INK_FAILURE);
    }

    @Override
    protected List<PixelButton> footer(Canvas canvas, Runnable close) {
        return List.of(new PixelButton(Component.translatable("skins.mcskincreator.save"),
                PixelButton.Style.NORMAL, this.onSave));
    }

    /** One entry: its picture, its name, the day it was saved. The row is the button. */
    private static final class SkinRow extends Element {
        private final SavedSkin skin;
        private final SkinThumbnails thumbnails;
        private final Consumer<SavedSkin> onOpen;
        private final Marquee marquee = new Marquee();

        private int actionsWidth;

        private SkinRow(SavedSkin skin, SkinThumbnails thumbnails, Consumer<SavedSkin> onOpen) {
            this.skin = skin;
            this.thumbnails = thumbnails;
            this.onOpen = onOpen;
        }

        void setActionsWidth(int actionsWidth) {
            this.actionsWidth = actionsWidth;
        }

        @Override
        public void draw(Paint paint) {
            Canvas canvas = paint.canvas();
            boolean hot = paint.hot(this);
            Surface.slot(canvas, this.x, this.y, this.width, this.height);
            if (hot) {
                Surface.slotHighlight(canvas, this.x, this.y, this.width, this.height);
            }

            int inset = Metrics.SLOT_INSET + Metrics.PAD_TIGHT;
            int pictureX = this.x + inset;
            int pictureY = this.y + inset;
            Surface.checker(canvas, pictureX, pictureY, PICTURE_WIDTH, PICTURE_HEIGHT);
            Identifier picture = this.thumbnails.of(this.skin.id());
            if (picture != null) {
                canvas.blit(picture, pictureX, pictureY, PICTURE_WIDTH, PICTURE_HEIGHT, 0, 0,
                        this.thumbnails.width(this.skin.id()), this.thumbnails.height(this.skin.id()),
                        this.thumbnails.width(this.skin.id()), this.thumbnails.height(this.skin.id()));
            }

            int textX = pictureX + PICTURE_WIDTH + Metrics.PAD;
            int textWidth = Math.max(0, this.x + this.width - textX - this.actionsWidth);
            int textY = this.y + (this.height - canvas.lineHeight() * 2 - Metrics.PAD_TIGHT) / 2;
            this.marquee.draw(paint, Component.literal(this.skin.name()), textX, textY, textWidth,
                    hot ? Palette.INK_HOVERED : Palette.INK, hot);
            canvas.textFlat(Component.literal(day(this.skin.at())), textX,
                    textY + canvas.lineHeight() + Metrics.PAD_TIGHT, Palette.INK_FAINT);
        }

        /**
         * The day the entry was saved.
         *
         * <p>Written the one way that reads the same in every language, since this is a
         * date the mod prints itself rather than a sentence it can translate.
         */
        private static String day(long at) {
            if (at <= 0) {
                return "";
            }
            return DAY.format(Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).toLocalDate());
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
            this.onOpen.accept(this.skin);
            return true;
        }

        @Override
        public List<Component> tooltip() {
            return List.of(Component.translatable("skins.mcskincreator.open.tooltip"));
        }
    }
}
