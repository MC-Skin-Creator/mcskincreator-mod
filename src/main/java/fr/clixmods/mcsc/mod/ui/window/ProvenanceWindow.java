/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui.window;

import java.net.URI;
import java.util.List;
import java.util.function.Supplier;

import fr.clixmods.mcsc.mod.MCSkinCreatorClient;
import fr.clixmods.mcsc.mod.style.Metrics;
import fr.clixmods.mcsc.mod.style.Palette;
import fr.clixmods.mcsc.mod.ui.Canvas;
import fr.clixmods.mcsc.mod.ui.Paint;
import fr.clixmods.mcsc.mod.ui.Prose;
import fr.clixmods.mcsc.mod.ui.widget.PixelButton;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

/**
 * Where one element comes from, laid out like the credits window: a line saying which
 * element this is, then one marked entry for the work it was cut from — the author in
 * bold, the title and licence under it, a button to the source — and one for the
 * starter models that use it.
 *
 * <p>Read again on every frame, so the models arrive in a window that is already open.
 * An element that names no work is an open question, not a fault, and is set in the
 * gold note the interface uses for exactly that.
 */
public class ProvenanceWindow extends ModalWindow {
    /** The work an element was cut from. */
    public record Work(String author, String title, String licence, String url) {
        public boolean hasUrl() {
            return this.url != null && (this.url.startsWith("https://")
                    || this.url.startsWith("http://"));
        }
    }

    /**
     * What the window shows right now.
     *
     * @param intro  which element this is about
     * @param work   the work it was cut from, or null when nobody has said
     * @param models what is known of the starter models using it, or null when that
     *               could not be found out
     */
    public record View(Component intro, Work work, Component models) {
    }

    private static final int MARK = 3;

    private final Supplier<View> view;

    public ProvenanceWindow(String titleKey, Supplier<View> view, ModalWindow returnsTo) {
        super(titleKey, returnsTo);
        this.view = view;
    }

    private static int indent() {
        return MARK + Metrics.PAD_TIGHT;
    }

    private static Component undocumented() {
        return Component.translatable("provenance.mcskincreator.undocumented");
    }

    private int introHeight(Canvas canvas, View view, int body) {
        return Prose.wrap(canvas, view.intro(), body).size() * (canvas.lineHeight() + 1) + Metrics.PAD;
    }

    private int workHeight(Canvas canvas, Work work, int body) {
        if (work == null) {
            return Prose.noteHeight(canvas, Prose.wrap(canvas, undocumented(), Prose.noteRoom(body)).size())
                    + Metrics.PAD_TIGHT;
        }
        int height = canvas.lineHeight() + 1 + canvas.smallLineHeight() + 1;
        if (work.hasUrl()) {
            height += Metrics.BUTTON_HEIGHT_COMPACT + Metrics.PAD_HAIR;
        }
        return height + Metrics.PAD_TIGHT;
    }

    private int modelsHeight(Canvas canvas, Component models, int body) {
        if (models == null) {
            return 0;
        }
        // Set at the half size, measured at the full one: double the room, same breaks.
        return canvas.lineHeight() + 1
                + Prose.wrap(canvas, models, (body - indent()) * 2).size() * (canvas.smallLineHeight() + 1)
                + Metrics.PAD_TIGHT;
    }

    @Override
    protected int contentHeight(Canvas canvas) {
        int body = width() - Metrics.PANEL_INSET * 2;
        View view = this.view.get();
        return introHeight(canvas, view, body) + workHeight(canvas, view.work(), body)
                + modelsHeight(canvas, view.models(), body);
    }

    @Override
    protected void layoutBody(Canvas canvas, int left, int top, int width) {
        View view = this.view.get();
        Work work = view.work();
        if (work == null || !work.hasUrl()) {
            return;
        }
        int cursorY = top + introHeight(canvas, view, width);
        int entryHeight = workHeight(canvas, work, width);
        PixelButton source = new PixelButton(Component.translatable("credits.mcskincreator.source"),
                PixelButton.Style.NORMAL, () -> open(work.url()));
        source.fit(canvas);
        source.withTooltip(Component.literal(work.url()));
        source.setBounds(left + indent(),
                cursorY + entryHeight - Metrics.PAD_TIGHT - Metrics.BUTTON_HEIGHT_COMPACT,
                source.width(), Metrics.BUTTON_HEIGHT_COMPACT);
        addBodyChild(source);
    }

    /** Hands a catalogue address to the browser; a refusal is logged, never thrown. */
    private static void open(String url) {
        try {
            Util.getPlatform().openUri(new URI(url));
        } catch (Exception failure) {
            MCSkinCreatorClient.LOGGER.warn("Could not open the credited source {}", url, failure);
        }
    }

    @Override
    protected void drawBody(Paint paint, int left, int top, int width) {
        Canvas canvas = paint.canvas();
        View view = this.view.get();
        int cursorY = top;
        for (String row : Prose.wrap(canvas, view.intro(), width)) {
            canvas.text(Component.literal(row), left, cursorY, Palette.INK_MUTED);
            cursorY += canvas.lineHeight() + 1;
        }
        cursorY += Metrics.PAD;

        Work work = view.work();
        int entryHeight = workHeight(canvas, work, width);
        if (work == null) {
            Prose.drawNote(canvas, left, cursorY, width,
                    Prose.wrap(canvas, undocumented(), Prose.noteRoom(width)));
        } else {
            canvas.fill(left, cursorY, MARK, entryHeight - Metrics.PAD_TIGHT, Palette.RULE);
            int textX = left + indent();
            canvas.text(Component.literal(work.author()).withStyle(ChatFormatting.BOLD),
                    textX, cursorY, Palette.INK);
            canvas.textSmall(Component.translatable("credits.mcskincreator.work_line",
                            work.title(), work.licence()),
                    textX, cursorY + canvas.lineHeight() + 1, Palette.INK_MUTED);
        }
        cursorY += entryHeight;

        if (view.models() != null) {
            int modelsHeight = modelsHeight(canvas, view.models(), width);
            canvas.fill(left, cursorY, MARK, modelsHeight - Metrics.PAD_TIGHT, Palette.RULE);
            int textX = left + indent();
            canvas.text(Component.translatable("provenance.mcskincreator.models_title")
                    .withStyle(ChatFormatting.BOLD), textX, cursorY, Palette.INK);
            cursorY += canvas.lineHeight() + 1;
            for (String row : Prose.wrap(canvas, view.models(), (width - indent()) * 2)) {
                canvas.textSmall(Component.literal(row), textX, cursorY, Palette.INK_FAINT);
                cursorY += canvas.smallLineHeight() + 1;
            }
        }
    }

    /** Nothing but the close cross: there is nothing to decide in here. */
    @Override
    protected List<PixelButton> footer(Canvas canvas, Runnable close) {
        return List.of();
    }
}
