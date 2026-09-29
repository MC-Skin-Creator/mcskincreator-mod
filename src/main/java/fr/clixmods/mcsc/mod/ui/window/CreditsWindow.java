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
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

/**
 * Who drew what, one entry at a time.
 *
 * <p>This was a run of paragraphs, each one a sentence with a name, a title, a licence
 * and a list of elements folded into it — so four works read as one wall of prose and
 * the names, which are the whole point of a credits window, were the hardest thing in
 * it to find. Here each work is an entry: a mark, the author's name in bold on its own
 * line, what they made under it, and a button to the source where there is one.
 *
 * <p>The button opens the address in the player's browser. That address comes from the
 * catalogue, so it is not the mod's to vouch for — which is why the whole of it is in
 * the button's tooltip, and why nothing is opened until the button is pressed.
 */
public class CreditsWindow extends ModalWindow {
    /**
     * One credited work.
     *
     * @param elements what this stack takes from it, already named in the player's
     *                 language
     */
    public record Entry(String author, String title, String licence, String url,
                        List<String> elements) {
        public boolean hasUrl() {
            return this.url != null && (this.url.startsWith("https://")
                    || this.url.startsWith("http://"));
        }
    }

    private final Supplier<Component> notice;
    private final Supplier<List<Entry>> entries;

    public CreditsWindow(String titleKey, Supplier<Component> notice,
                         Supplier<List<Entry>> entries, ModalWindow returnsTo) {
        super(titleKey, returnsTo);
        this.notice = notice;
        this.entries = entries;
    }

    /** The mark down the left of an entry, and the room it takes with its gutter. */
    private static final int MARK = 3;

    private static int indent() {
        return MARK + Metrics.PAD_TIGHT;
    }

    @Override
    protected int contentHeight(Canvas canvas) {
        int body = bodyWidth();
        int height = Prose.wrap(canvas, this.notice.get(), body).size()
                * (canvas.lineHeight() + 1) + Metrics.PAD;
        for (Entry entry : this.entries.get()) {
            height += heightOf(canvas, entry, body);
        }
        return height;
    }

    private int heightOf(Canvas canvas, Entry entry, int body) {
        int room = body - indent();
        int height = canvas.lineHeight() + 1 + canvas.smallLineHeight() + 1;
        // Wrapped against twice the room, because these rows are set at the half size
        // and Prose measures at the full one: double the room, same break points.
        height += Prose.wrap(canvas, elementsOf(entry), room * 2).size()
                * (canvas.smallLineHeight() + 1);
        if (entry.hasUrl()) {
            height += Metrics.BUTTON_HEIGHT_COMPACT + Metrics.PAD_HAIR;
        }
        return height + Metrics.PAD_TIGHT;
    }

    private static Component elementsOf(Entry entry) {
        return Component.translatable("credits.mcskincreator.elements",
                String.join(", ", entry.elements()));
    }

    @Override
    protected void layoutBody(Canvas canvas, int left, int top, int width) {
        int cursorY = top + Prose.wrap(canvas, this.notice.get(), width).size()
                * (canvas.lineHeight() + 1) + Metrics.PAD;
        for (Entry entry : this.entries.get()) {
            int entryHeight = heightOf(canvas, entry, width);
            if (entry.hasUrl()) {
                PixelButton source = new PixelButton(
                        Component.translatable("credits.mcskincreator.source"),
                        PixelButton.Style.NORMAL, () -> open(entry.url()));
                source.fit(canvas);
                source.withTooltip(Component.literal(entry.url()));
                source.setBounds(left + indent(),
                        cursorY + entryHeight - Metrics.PAD_TIGHT
                                - Metrics.BUTTON_HEIGHT_COMPACT,
                        source.width(), Metrics.BUTTON_HEIGHT_COMPACT);
                addBodyChild(source);
            }
            cursorY += entryHeight;
        }
    }

    /**
     * Hands the address to the player's browser.
     *
     * <p>It came from the catalogue, so it is checked for a scheme the mod is willing
     * to open before anything is handed over, and a refusal is logged rather than
     * thrown: a credits window is not worth a crash.
     */
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
        int cursorY = top;
        for (String row : Prose.wrap(canvas, this.notice.get(), width)) {
            canvas.text(Component.literal(row), left, cursorY, Palette.INK_MUTED);
            cursorY += canvas.lineHeight() + 1;
        }
        cursorY += Metrics.PAD;

        for (Entry entry : this.entries.get()) {
            int entryHeight = heightOf(canvas, entry, width);
            // A mark down the whole entry rather than a bullet beside its first line:
            // it is what says where one work stops and the next begins, and a stack of
            // them reads as a list at a glance.
            canvas.fill(left, cursorY, MARK, entryHeight - Metrics.PAD_TIGHT, Palette.RULE);

            int textX = left + indent();
            canvas.text(Component.literal(entry.author()).withStyle(ChatFormatting.BOLD),
                    textX, cursorY, Palette.INK);
            cursorY += canvas.lineHeight() + 1;

            canvas.textSmall(Component.translatable("credits.mcskincreator.work_line",
                            entry.title(), entry.licence()),
                    textX, cursorY, Palette.INK_MUTED);
            cursorY += canvas.smallLineHeight() + 1;

            for (String row : Prose.wrap(canvas, elementsOf(entry), (width - indent()) * 2)) {
                canvas.textSmall(Component.literal(row), textX, cursorY, Palette.INK_FAINT);
                cursorY += canvas.smallLineHeight() + 1;
            }
            if (entry.hasUrl()) {
                cursorY += Metrics.BUTTON_HEIGHT_COMPACT + Metrics.PAD_HAIR;
            }
            cursorY += Metrics.PAD_TIGHT;
        }
    }

    /**
     * The credits as plain text, one work per line, source address last where there is
     * one.
     */
    private String plainText() {
        StringBuilder text = new StringBuilder();
        for (Entry entry : this.entries.get()) {
            if (text.length() > 0) {
                text.append('\n');
            }
            text.append(Component.translatable("credits.mcskincreator.work",
                    entry.title(), entry.author(), entry.licence(),
                    String.join(", ", entry.elements())).getString());
            if (entry.hasUrl()) {
                text.append(" — ").append(entry.url());
            }
        }
        return text.toString();
    }

    /**
     * Copy, because nothing in a window can be selected. Disabled rather than absent
     * when there is nothing to credit, so the window still says what it offers.
     */
    @Override
    protected List<PixelButton> footer(Canvas canvas, Runnable close) {
        String text = plainText();
        PixelButton copy = new PixelButton(Component.translatable("credits.mcskincreator.copy"),
                PixelButton.Style.NORMAL,
                () -> Minecraft.getInstance().keyboardHandler.setClipboard(text));
        copy.setEnabled(!text.isEmpty());
        return List.of(copy);
    }
}
