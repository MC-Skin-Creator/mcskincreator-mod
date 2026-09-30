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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.mojang.blaze3d.platform.InputConstants;

import fr.clixmods.mcsc.mod.project.SavedSkin;
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
 * The player's library: the skins kept on this machine, and what can be done with them.
 *
 * <p>Every entry is a project, and every project is one entry: nothing in the editor
 * files a second copy behind the player's back. The list comes most recently changed
 * first, and the picture on each row is the one kept beside that skin. Nothing is recomposed
 * to draw this window.
 *
 * <p>A row is opened by clicking it. Its actions sit on its right, drawn at rest rather
 * than revealed on hover — what can be pressed is visible, and a removal that can be
 * reached without being seen is how a skin goes missing: rename, duplicate, and the
 * cross that removes it. Duplicating is how a version is put aside before it is edited
 * further.
 *
 * <p>One row is the project in progress — the skin on the editor right now, which
 * follows the editing. It says so in place of its date, and it has no cross: there is
 * always a project in progress, and removing the one being edited would pull the stack
 * out from under the editor.
 */
public class SkinsWindow extends ModalWindow {
    /** The height of a row: the front view at two pixels per texel, plus its padding. */
    private static final int ROW = 32 * SkinThumbnails.SCALE + Metrics.PAD_TIGHT * 2 + Metrics.SLOT_INSET * 2;
    private static final int PICTURE_WIDTH = 16 * SkinThumbnails.SCALE;
    private static final int PICTURE_HEIGHT = 32 * SkinThumbnails.SCALE;

    private static final DateTimeFormatter CHANGED = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

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
    private final Supplier<String> currentId;
    private final SkinThumbnails thumbnails;
    private final Consumer<SavedSkin> onOpen;
    private final Consumer<SavedSkin> onRename;
    private final Consumer<SavedSkin> onDuplicate;
    private final Consumer<SavedSkin> onDelete;
    private final Runnable onNew;

    /** What each row, and the footer, can do. */
    public record Actions(Consumer<SavedSkin> open, Consumer<SavedSkin> rename,
                          Consumer<SavedSkin> duplicate, Consumer<SavedSkin> delete,
                          Runnable startNew) {
    }

    /**
     * @param currentId the identifier of the project in progress, or null while there
     *                  is none yet
     */
    public SkinsWindow(Supplier<Library> library, Supplier<String> currentId, SkinThumbnails thumbnails,
                       Actions actions) {
        super("window.mcskincreator.skins", null);
        this.library = library;
        this.currentId = currentId;
        this.thumbnails = thumbnails;
        this.onOpen = actions.open();
        this.onRename = actions.rename();
        this.onDuplicate = actions.duplicate();
        this.onDelete = actions.delete();
        this.onNew = actions.startNew();
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
        String current = this.currentId.get();
        for (SavedSkin skin : skins()) {
            boolean inProgress = skin.id().equals(current);
            SkinRow row = new SkinRow(skin, inProgress, this.thumbnails, this.onOpen);
            row.setBounds(left, cursorY, width, ROW);
            addBodyChild(row);

            // Right to left, so the cross stays on the far edge where it always was. The
            // project in progress has no cross: there is always one, and it is the one
            // on the editor.
            List<PixelButton> actions = new ArrayList<>();
            if (!inProgress) {
                actions.add(new PixelButton(Component.literal("x"), PixelButton.Style.NORMAL,
                        () -> this.onDelete.accept(skin))
                        .withTooltip(Component.translatable("skins.mcskincreator.delete.tooltip")));
            }
            actions.add(new PixelButton(Component.translatable("skins.mcskincreator.duplicate"),
                    PixelButton.Style.NORMAL, () -> this.onDuplicate.accept(skin))
                    .withTooltip(Component.translatable("skins.mcskincreator.duplicate.tooltip")));
            actions.add(new PixelButton(Component.translatable("skins.mcskincreator.rename"),
                    PixelButton.Style.NORMAL, () -> this.onRename.accept(skin))
                    .withTooltip(Component.translatable("skins.mcskincreator.rename.tooltip")));

            int right = left + width - Metrics.PAD_TIGHT;
            for (PixelButton action : actions) {
                action.fit(canvas);
                right -= action.width();
                action.setBounds(right, cursorY + (ROW - Metrics.BUTTON_HEIGHT_COMPACT) / 2,
                        action.width(), Metrics.BUTTON_HEIGHT_COMPACT);
                addBodyChild(action);
                right -= Metrics.PAD_TIGHT;
            }
            row.setActionsWidth(left + width - right + Metrics.PAD_TIGHT);

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
        return List.of(new PixelButton(Component.translatable("skins.mcskincreator.new"),
                PixelButton.Style.PRIMARY, () -> {
                    this.onNew.run();
                    close.run();
                }));
    }

    /** One entry: its picture, its name, when it last changed. The row is the button. */
    private static final class SkinRow extends Element {
        private final SavedSkin skin;
        private final boolean inProgress;
        private final SkinThumbnails thumbnails;
        private final Consumer<SavedSkin> onOpen;
        private final Marquee marquee = new Marquee();

        private int actionsWidth;

        private SkinRow(SavedSkin skin, boolean inProgress, SkinThumbnails thumbnails,
                        Consumer<SavedSkin> onOpen) {
            this.skin = skin;
            this.inProgress = inProgress;
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
            int subtitleY = textY + canvas.lineHeight() + Metrics.PAD_TIGHT;
            if (this.inProgress) {
                canvas.textFlat(Component.translatable("skins.mcskincreator.in_progress"), textX,
                        subtitleY, Palette.LIME);
            } else {
                canvas.textFlat(Component.literal(changed(this.skin.at())), textX, subtitleY,
                        Palette.INK_FAINT);
            }
        }

        /**
         * When the entry last changed, to the minute: two versions of the same skin
         * edited on the same day are exactly the ones this has to tell apart.
         *
         * <p>Written the one way that reads the same in every language, since this is a
         * date the mod prints itself rather than a sentence it can translate.
         */
        private static String changed(long at) {
            if (at <= 0) {
                return "";
            }
            return CHANGED.format(Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()));
        }

        @Override
        public boolean clickSound() {
            return true;
        }

        @Override
        public boolean mouseDown(double mouseX, double mouseY, int button) {
            // The remove button sits on top of the row, and the row is offered the press
            // first: the strip it occupies has to be left to the button.
            boolean onActions = mouseX >= this.x + this.width - this.actionsWidth;
            return button == InputConstants.MOUSE_BUTTON_LEFT && contains(mouseX, mouseY)
                    && !onActions && activate();
        }

        @Override
        public boolean activate() {
            this.onOpen.accept(this.skin);
            return true;
        }

        @Override
        public List<Component> tooltip() {
            return List.of(Component.translatable(this.inProgress
                    ? "skins.mcskincreator.in_progress.tooltip"
                    : "skins.mcskincreator.open.tooltip"));
        }
    }
}
