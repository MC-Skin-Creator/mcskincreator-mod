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

import fr.clixmods.mcsc.mod.style.Metrics;
import fr.clixmods.mcsc.mod.style.Palette;
import fr.clixmods.mcsc.mod.style.Surface;
import fr.clixmods.mcsc.mod.ui.Canvas;
import fr.clixmods.mcsc.mod.ui.Element;
import fr.clixmods.mcsc.mod.ui.Paint;
import net.minecraft.network.chat.Component;

/**
 * A side column: stone panel, a header with the title and the button that folds it,
 * and whatever the subclass puts under that.
 *
 * <p>Folding leaves the header with nothing but the unfold button. That is not a
 * detail: an earlier arrangement kept the title as well, the header overflowed its
 * 27 pixels, and the button that would have brought the panel back was the part that
 * got pushed out of sight.
 */
public abstract class Panel extends Element {
    private final String titleKey;
    private final boolean foldsLeft;
    private final List<Element> children = new ArrayList<>();

    private boolean folded;

    protected Panel(String titleKey, boolean foldsLeft) {
        this.titleKey = titleKey;
        this.foldsLeft = foldsLeft;
        // A band is clicked — its rail is dragged — but it is never a stop on the focus
        // ring: Tab walks the controls inside it, not the column they sit in.
        setFocusable(false);
    }

    public boolean folded() {
        return this.folded;
    }

    public void setFolded(boolean folded) {
        this.folded = folded;
    }

    public void toggleFolded() {
        this.folded = !this.folded;
    }

    /**
     * The chevron the fold button shows, pointing the way the panel would go.
     *
     * <p>Drawn in the game's own font. Vanilla's page arrow is the only arrow it
     * ships, and at 23 by 13 pixels of near-white it swamps a header this size — the
     * font's chevron is the same typeface as everything else on the row.
     */
    public Component foldLabel() {
        return Component.literal(this.folded != this.foldsLeft ? "<" : ">");
    }

    public String foldTooltipKey() {
        return this.folded ? "gui.mcskincreator.unfold.tooltip" : "gui.mcskincreator.fold.tooltip";
    }

    protected int headerHeight(Canvas canvas) {
        return canvas.lineHeight() + Metrics.PANEL_INSET + Metrics.PAD_TIGHT;
    }

    /** The children the screen walks for focus and hit testing, in that order. */
    public List<Element> children() {
        return this.children;
    }

    protected void clearChildren() {
        this.children.clear();
    }

    protected <T extends Element> T addChild(T child) {
        this.children.add(child);
        return child;
    }

    /** Positions everything. Called on resize and whenever the contents change. */
    public abstract void layout(Canvas canvas);

    protected void drawFrame(Paint paint) {
        Canvas canvas = paint.canvas();
        Surface.panel(canvas, this.x, this.y, this.width, this.height);

        if (this.folded) {
            return;
        }

        // A heading and a rule under it, which is how the game titles its own lists.
        int header = headerHeight(canvas);
        String title = Component.translatable(this.titleKey).getString().toUpperCase(Locale.ROOT);
        canvas.textTracked(title, this.x + Metrics.PANEL_INSET,
                this.y + Metrics.PANEL_INSET, Palette.INK_MUTED, Metrics.TITLE_TRACKING);
        Surface.rule(canvas, this.x + Metrics.PANEL_INSET, this.y + header - Metrics.PAD_TIGHT,
                this.width - Metrics.PANEL_INSET * 2);
    }
}
