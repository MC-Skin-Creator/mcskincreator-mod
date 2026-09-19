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

    /**
     * The left edge of everything the panel puts inside itself.
     *
     * <p>Named once, here, because the panel sprite carries a six pixel border and
     * anything laid out closer than that to the panel's own edge is drawn <em>under</em>
     * its frame. That is not a subtle mistake — it is what was clipping the last letter
     * off every value in the inspector — but it is an invisible one until something
     * long enough reaches the edge.
     */
    protected int contentLeft() {
        return this.x + Metrics.PANEL_INSET;
    }

    /** The right edge of the same box: the frame, and not a pixel past it. */
    protected int contentRight() {
        return this.x + this.width - Metrics.PANEL_INSET;
    }

    protected int contentWidth() {
        return Math.max(0, contentRight() - contentLeft());
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
    /**
     * True when the pointer is inside this panel's scrolling band.
     *
     * <p>Both columns used to answer the wheel on the strength of its <em>height</em>
     * alone, and the screen offers the wheel to the library first — so with the pointer
     * over the layers, the library took it and scrolled instead. A band has two
     * dimensions, and a panel is not the only panel on the screen.
     */
    protected boolean inBody(double mouseX, double mouseY, int bodyTop, int bodyHeight) {
        return !folded()
                && mouseX >= this.x && mouseX < this.x + this.width
                && mouseY >= bodyTop && mouseY < bodyTop + bodyHeight;
    }

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
        canvas.textTracked(title, contentLeft(),
                this.y + Metrics.PANEL_INSET, Palette.INK, Metrics.TITLE_TRACKING);
        Surface.rule(canvas, contentLeft(), this.y + header - Metrics.PAD_TIGHT, contentWidth());
    }
}
