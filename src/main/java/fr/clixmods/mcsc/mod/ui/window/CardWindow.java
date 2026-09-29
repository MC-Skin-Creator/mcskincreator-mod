/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui.window;

import java.util.List;

import fr.clixmods.mcsc.mod.style.Metrics;
import fr.clixmods.mcsc.mod.style.Palette;
import fr.clixmods.mcsc.mod.style.Surface;
import fr.clixmods.mcsc.mod.ui.Canvas;
import fr.clixmods.mcsc.mod.ui.Element;
import fr.clixmods.mcsc.mod.ui.Paint;
import fr.clixmods.mcsc.mod.ui.Prose;
import net.minecraft.network.chat.Component;

/**
 * A window of cards: a bold title, a sentence of explanation, and the whole card is
 * the button.
 *
 * <p>This is the export menu's shape. Making the card itself the target rather than
 * putting a small button in the corner of it is what makes the choice readable: the
 * sentence is part of what you are choosing, not a caption beside it.
 *
 * <p>The site puts an icon on each card. The mod has none to put there and will not
 * invent one, so the title carries the card.
 */
public class CardWindow extends ModalWindow {
    /** One choice: a bold title, a sentence under it, and the whole card is the button. */
    public record Card(String titleKey, String detailKey, Runnable action) {
    }

    private final List<Card> cards;
    private final Component note;

    public CardWindow(String titleKey, List<Card> cards, ModalWindow returnsTo) {
        this(titleKey, cards, null, returnsTo);
    }

    /**
     * @param note a paragraph set under the cards, or null for none. It is how a window
     *             says why a choice it is <em>not</em> offering is missing: a card that
     *             cannot work is left out rather than shown dead, and a choice that
     *             silently disappears is one nobody can ask about.
     */
    public CardWindow(String titleKey, List<Card> cards, Component note, ModalWindow returnsTo) {
        super(titleKey, returnsTo);
        this.cards = cards;
        this.note = note;
    }

    private int cardHeight(Canvas canvas) {
        return canvas.lineHeight() * 2 + Metrics.PAD_TIGHT + Metrics.SLOT_INSET * 2;
    }

    private int noteHeight(Canvas canvas) {
        if (this.note == null) {
            return 0;
        }
        int body = width() - Metrics.PAD * 2;
        return Prose.noteHeight(canvas, Prose.wrap(canvas, this.note, Prose.noteRoom(body)).size());
    }

    @Override
    protected int contentHeight(Canvas canvas) {
        return this.cards.size() * (cardHeight(canvas) + Metrics.PAD_TIGHT) + noteHeight(canvas);
    }

    @Override
    protected void layoutBody(Canvas canvas, int left, int top, int width) {
        int height = cardHeight(canvas);
        int cursorY = top;
        for (Card card : this.cards) {
            CardButton button = new CardButton(card);
            button.setBounds(left, cursorY, width, height);
            addBodyChild(button);
            cursorY += height + Metrics.PAD_TIGHT;
        }
    }

    @Override
    protected void drawBody(Paint paint, int left, int top, int width) {
        // The cards are children, so the window draws them with everything else. The
        // note is not a control and so is drawn here.
        if (this.note == null) {
            return;
        }
        Canvas canvas = paint.canvas();
        int noteTop = top + this.cards.size() * (cardHeight(canvas) + Metrics.PAD_TIGHT);
        Prose.drawNote(canvas, left, noteTop, width, Prose.wrap(canvas, this.note, Prose.noteRoom(width)));
    }

    /** A card. Its own element so it can be focused and reached without a mouse. */
    private static final class CardButton extends Element {
        private final Card card;

        private CardButton(Card card) {
            this.card = card;
        }

        @Override
        public void draw(Paint paint) {
            Canvas canvas = paint.canvas();
            boolean hot = paint.hot(this);
            Surface.slot(canvas, this.x, this.y, this.width, this.height);
            if (hot) {
                Surface.slotHighlight(canvas, this.x, this.y, this.width, this.height);
            }

            int textX = this.x + Metrics.SLOT_INSET;
            int textY = this.y + Metrics.SLOT_INSET;
            canvas.text(Component.translatable(this.card.titleKey()), textX, textY,
                    hot ? Palette.INK_HOVERED : Palette.INK);
            canvas.textFlat(Component.translatable(this.card.detailKey()), textX,
                    textY + canvas.lineHeight() + Metrics.PAD_TIGHT, Palette.INK_MUTED);
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
            this.card.action().run();
            return true;
        }
    }
}
