/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui;

import fr.clixmods.mcsc.mod.style.Metrics;
import fr.clixmods.mcsc.mod.style.Sprites;

/**
 * The scrolling of one band, done by hand.
 *
 * <p>A Minecraft screen has no layout and no scrolling: positions are pixels worked
 * out in {@code init()} and everything else is drawn. So this holds an offset, clamps
 * it to whatever the content turned out to be tall, and draws the rail — a carved
 * groove with a handle in button material, like every other track in the interface.
 *
 * <p>The rail is the game's own scroller and track, at the width the game gives them.
 *
 * <p>It is not a widget. It has no bounds of its own and takes no focus: it belongs
 * to the band that scrolls, and that band decides where it sits.
 */
public final class ScrollPane {
    /**
     * The rail is this wide, and the bands that scroll reserve it whether or not there
     * is anything to scroll — a gutter that appears with the rail would shuffle every
     * tile sideways the moment a category grew by one element.
     */
    public static final int BAR_WIDTH = Sprites.SCROLLER_WIDTH;
    private static final int STEP = Metrics.ui(24);

    private int offset;
    private int contentHeight;
    private int viewportHeight;
    private boolean draggingBar;

    /** Tells the pane how much there is to scroll through, and clamps the offset. */
    public void setContent(int contentHeight, int viewportHeight) {
        this.contentHeight = contentHeight;
        this.viewportHeight = viewportHeight;
        this.offset = Math.max(0, Math.min(maxOffset(), this.offset));
    }

    public int offset() {
        return this.offset;
    }

    /** Back to the top — what a window does when it is reopened. */
    public void reset() {
        this.offset = 0;
    }

    public boolean scrollable() {
        return maxOffset() > 0;
    }

    public int maxOffset() {
        return Math.max(0, this.contentHeight - this.viewportHeight);
    }

    /** @return true when there was anywhere to scroll to */
    public boolean scroll(double amount) {
        if (!scrollable()) {
            return false;
        }
        this.offset = Math.max(0, Math.min(maxOffset(), this.offset - (int) Math.round(amount * STEP)));
        return true;
    }

    /** The rail, drawn down the right edge of the band. Absent when there is no overflow. */
    public void drawBar(Canvas canvas, int right, int top, int height) {
        if (!scrollable()) {
            return;
        }
        int x = right - BAR_WIDTH;
        canvas.sprite(Sprites.SCROLLER_TRACK, x, top, BAR_WIDTH, height);

        int handle = handleHeight();
        int travel = height - handle;
        int handleY = top + (travel * this.offset / Math.max(1, maxOffset()));
        canvas.sprite(Sprites.SCROLLER, x, handleY, BAR_WIDTH, handle);
    }

    /** @return true when the press landed on the rail and started a drag */
    public boolean barMouseDown(double mouseX, double mouseY, int right, int top, int height) {
        if (!scrollable()) {
            return false;
        }
        int x = right - BAR_WIDTH;
        if (mouseX < x || mouseX >= right || mouseY < top || mouseY >= top + height) {
            return false;
        }
        this.draggingBar = true;
        dragTo(mouseY, top, height);
        return true;
    }

    public void barMouseDrag(double mouseY, int top, int height) {
        if (this.draggingBar) {
            dragTo(mouseY, top, height);
        }
    }

    public void barMouseUp() {
        this.draggingBar = false;
    }

    /** The handle is as tall a share of the rail as the view is of the content. */
    private int handleHeight() {
        return Math.max(Metrics.ui(18),
                this.viewportHeight * this.viewportHeight / Math.max(1, this.contentHeight));
    }

    private void dragTo(double mouseY, int top, int height) {
        int travel = Math.max(1, height - handleHeight());
        double along = (mouseY - top - handleHeight() / 2.0) / travel;
        this.offset = Math.max(0, Math.min(maxOffset(), (int) Math.round(along * maxOffset())));
    }
}
