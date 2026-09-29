/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui.window;

import java.util.List;
import java.util.function.Supplier;

import fr.clixmods.mcsc.mod.style.Metrics;
import fr.clixmods.mcsc.mod.style.Palette;
import fr.clixmods.mcsc.mod.style.Surface;
import fr.clixmods.mcsc.mod.ui.Canvas;
import fr.clixmods.mcsc.mod.ui.Element;
import fr.clixmods.mcsc.mod.ui.Paint;
import fr.clixmods.mcsc.mod.ui.Prose;
import fr.clixmods.mcsc.mod.ui.panel.TopBar;
import net.minecraft.network.chat.Component;

/**
 * A window of cards: a bold title, a sentence of explanation, and the whole card is
 * the button.
 *
 * <p>This is the export menu's shape. Making the card itself the target rather than
 * putting a small button in the corner of it is what makes the choice readable: the
 * sentence is part of what you are choosing, not a caption beside it.
 *
 * <p>A window may single one card out as the lead. It goes first, in the green of the
 * primary action, with the mod's mark beside it, and a divider sets the other cards
 * apart under it. Three cards of the same grey said the three choices were equal, and
 * they are not: one of them is what the window is for, the others are ways round it.
 * The lead is the only card with a picture, because the mark is the one picture the
 * mod has and will not invent another.
 */
public class CardWindow extends ModalWindow {
    /** One choice: a bold title, a sentence under it, and the whole card is the button. */
    public record Card(String titleKey, String detailKey, Runnable action) {
    }

    /** The card the window is for, and the mark drawn beside it. */
    public record Lead(Card card, Supplier<TopBar.Mark> mark) {
    }

    /**
     * The mark on the lead card: a quarter of the 128 px icon, a whole fraction so it
     * comes out as crisp as the icon does in the game's mod list.
     */
    private static final int MARK = 32;
    /** From the edge of a card to its text. */
    private static final int CARD_PAD = Metrics.SLOT_INSET + Metrics.PAD_TIGHT;

    private final Lead lead;
    private final List<Card> cards;
    private final Component note;
    private final Component divider;

    public CardWindow(String titleKey, List<Card> cards, ModalWindow returnsTo) {
        this(titleKey, null, cards, null, null, returnsTo);
    }

    /**
     * @param lead    the card the window is for, or null for a window of equals
     * @param note    a paragraph set where the lead would be, or null for none. It is
     *                how a window says why a choice it is <em>not</em> offering is
     *                missing: a card that cannot work is left out rather than shown
     *                dead, and a choice that silently disappears is one nobody can ask
     *                about.
     * @param divider the caption on the rule between the lead and the other cards, or
     *                null for a bare rule. Only drawn under a lead: under the note it
     *                would be the alternative to nothing.
     */
    public CardWindow(String titleKey, Lead lead, List<Card> cards, Component note,
                      Component divider, ModalWindow returnsTo) {
        super(titleKey, returnsTo);
        this.lead = lead;
        this.cards = cards;
        this.note = note;
        this.divider = divider;
    }

    /** The lead card's detail, wrapped beside the mark. */
    private List<String> leadRows(Canvas canvas, int width) {
        int room = width - CARD_PAD * 2 - MARK - Metrics.PAD;
        return Prose.wrap(canvas, Component.translatable(this.lead.card().detailKey()), room);
    }

    private int leadHeight(Canvas canvas, int width) {
        int text = canvas.lineHeight() + Metrics.PAD_TIGHT
                + leadRows(canvas, width).size() * (canvas.lineHeight() + 1);
        return Math.max(MARK, text) + CARD_PAD * 2;
    }

    /** A card's detail, wrapped to the card. */
    private static List<String> rows(Canvas canvas, Card card, int width) {
        return Prose.wrap(canvas, Component.translatable(card.detailKey()), width - CARD_PAD * 2);
    }

    private static int cardHeight(Canvas canvas, Card card, int width) {
        return CARD_PAD * 2 + canvas.lineHeight() + Metrics.PAD_HAIR
                + rows(canvas, card, width).size() * (canvas.lineHeight() + 1);
    }

    private List<String> noteRows(Canvas canvas, int width) {
        return Prose.wrap(canvas, this.note, Prose.noteRoom(width));
    }

    /** What stands above the divider: the lead, or the note saying why there is none. */
    private int headHeight(Canvas canvas, int width) {
        if (this.lead != null) {
            return leadHeight(canvas, width);
        }
        if (this.note != null) {
            return Prose.noteHeight(canvas, noteRows(canvas, width).size());
        }
        return 0;
    }

    private boolean divided(Canvas canvas, int width) {
        return headHeight(canvas, width) > 0 && !this.cards.isEmpty();
    }

    /** The divider: the gap above it, its caption, and the gap below it. */
    private int dividerHeight(Canvas canvas, int width) {
        return divided(canvas, width) ? Metrics.PAD * 2 + canvas.lineHeight() : 0;
    }

    @Override
    protected int contentHeight(Canvas canvas) {
        int width = bodyWidth();
        int height = headHeight(canvas, width) + dividerHeight(canvas, width);
        for (Card card : this.cards) {
            height += cardHeight(canvas, card, width) + Metrics.PAD_TIGHT;
        }
        return height;
    }

    @Override
    protected void layoutBody(Canvas canvas, int left, int top, int width) {
        int cursorY = top;
        if (this.lead != null) {
            int height = leadHeight(canvas, width);
            LeadButton button = new LeadButton(this.lead, leadRows(canvas, width));
            button.setBounds(left, cursorY, width, height);
            addBodyChild(button);
        }
        cursorY += headHeight(canvas, width) + dividerHeight(canvas, width);
        for (Card card : this.cards) {
            int height = cardHeight(canvas, card, width);
            CardButton button = new CardButton(card, rows(canvas, card, width));
            button.setBounds(left, cursorY, width, height);
            addBodyChild(button);
            cursorY += height + Metrics.PAD_TIGHT;
        }
    }

    @Override
    protected void drawBody(Paint paint, int left, int top, int width) {
        // The cards are children, so the window draws them with everything else. The
        // note and the divider are not controls and so are drawn here.
        Canvas canvas = paint.canvas();
        if (this.lead == null && this.note != null) {
            Prose.drawNote(canvas, left, top, width, noteRows(canvas, width));
        }
        if (divided(canvas, width)) {
            drawDivider(canvas, left, top + headHeight(canvas, width) + Metrics.PAD, width);
        }
    }

    /** A rule across the body with the caption set in a gap in its middle. */
    private void drawDivider(Canvas canvas, int left, int top, int width) {
        int ruleY = top + canvas.lineHeight() / 2;
        // The caption reads as the alternative to the lead, so it goes with the lead: over
        // a note saying why there is no lead, "or" would be the alternative to nothing.
        if (this.divider == null || this.lead == null) {
            Surface.rule(canvas, left, ruleY, width);
            return;
        }
        int textWidth = canvas.textWidth(this.divider);
        int gap = Metrics.PAD;
        int side = (width - textWidth) / 2 - gap;
        if (side <= 0) {
            Surface.rule(canvas, left, ruleY, width);
            return;
        }
        Surface.rule(canvas, left, ruleY, side);
        Surface.rule(canvas, left + width - side, ruleY, side);
        canvas.textFlat(this.divider, left + side + gap, top + 1, Palette.INK_FAINT);
    }

    /** The lead: green, the mark on the left, and the whole card is the button. */
    private static final class LeadButton extends Element {
        private final Lead lead;
        private final List<String> rows;

        private LeadButton(Lead lead, List<String> rows) {
            this.lead = lead;
            this.rows = rows;
        }

        @Override
        public void draw(Paint paint) {
            Canvas canvas = paint.canvas();
            boolean hot = paint.hot(this);
            Surface.button(canvas, this.x, this.y, this.width, this.height,
                    Surface.Tone.GREEN, hot, false);

            int markX = this.x + CARD_PAD;
            int markY = this.y + (this.height - MARK) / 2;
            TopBar.Mark mark = this.lead.mark() == null ? null : this.lead.mark().get();
            if (mark != null && mark.texture() != null && mark.size() > 0) {
                int source = mark.size();
                canvas.blit(mark.texture(), markX, markY, MARK, MARK,
                        0.0F, 0.0F, source, source, source, source);
            }

            int textX = markX + MARK + Metrics.PAD;
            int textHeight = canvas.lineHeight() + Metrics.PAD_TIGHT
                    + this.rows.size() * (canvas.lineHeight() + 1);
            int textY = this.y + (this.height - textHeight) / 2;
            canvas.text(Component.translatable(this.lead.card().titleKey()), textX, textY,
                    hot ? Palette.INK_HOVERED : Palette.INK);
            int rowY = textY + canvas.lineHeight() + Metrics.PAD_TIGHT;
            for (String row : this.rows) {
                canvas.text(Component.literal(row), textX, rowY, Palette.INK);
                rowY += canvas.lineHeight() + 1;
            }
        }

        @Override
        public boolean mouseDown(double mouseX, double mouseY, int button) {
            return button == 0 && contains(mouseX, mouseY) && activate();
        }

        @Override
        public boolean activate() {
            this.lead.card().action().run();
            return true;
        }
    }

    /** A card. Its own element so it can be focused and reached without a mouse. */
    private static final class CardButton extends Element {
        private final Card card;
        private final List<String> rows;

        private CardButton(Card card, List<String> rows) {
            this.card = card;
            this.rows = rows;
        }

        @Override
        public void draw(Paint paint) {
            Canvas canvas = paint.canvas();
            boolean hot = paint.hot(this);
            Surface.slot(canvas, this.x, this.y, this.width, this.height);
            if (hot) {
                Surface.slotHighlight(canvas, this.x, this.y, this.width, this.height);
            }

            int textX = this.x + CARD_PAD;
            int textY = this.y + CARD_PAD;
            canvas.text(Component.translatable(this.card.titleKey()), textX, textY,
                    hot ? Palette.INK_HOVERED : Palette.INK);
            int rowY = textY + canvas.lineHeight() + Metrics.PAD_HAIR;
            for (String row : this.rows) {
                canvas.textFlat(Component.literal(row), textX, rowY, Palette.INK_MUTED);
                rowY += canvas.lineHeight() + 1;
            }
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
