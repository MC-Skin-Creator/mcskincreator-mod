/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui;

import fr.clixmods.mcsc.mod.style.Metrics;
import fr.clixmods.mcsc.mod.style.Palette;
import fr.clixmods.mcsc.mod.style.Surface;

/**
 * The scrolling of one band, done by hand.
 *
 * <p>A Minecraft screen has no layout and no scrolling: positions are pixels worked
 * out in {@code init()} and everything else is drawn. So this holds an offset, clamps
 * it to whatever the content turned out to be tall, and draws the rail — a carved
 * groove with a handle in button material, like every other track in the interface.
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
    public static final int BAR_WIDTH = Metrics.ui(9);
    private static final int STEP = Metrics.ui(24);

    private int offset;
    private int contentHeight;
    private int viewportHeight;
    private boolean draggingBar;
    /**
     * Where inside the handle the press landed, so that dragging moves the handle with
     * the pointer instead of snapping its middle under it.
     */
    private int grabWithinHandle;

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
        Surface.slot(canvas, x, top, BAR_WIDTH, height, Palette.FIELD);
        // Lit while it is being held, so a grab that took is visible before the
        // content has moved far enough to say so on its own.
        Surface.button(canvas, x, handleY(top, height), BAR_WIDTH, handleHeight(height),
                Surface.Tone.NEUTRAL, this.draggingBar, false);
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

        // Taking hold of the handle keeps it where it was taken hold of: pressing its
        // lower edge and moving down by ten pixels moves the handle by ten pixels.
        // Pressing the rail above or below it is the other gesture — the handle jumps
        // to the pointer, and is then dragged from its middle.
        int handle = handleHeight(height);
        int handleY = handleY(top, height);
        this.grabWithinHandle = mouseY >= handleY && mouseY < handleY + handle
                ? (int) (mouseY - handleY)
                : handle / 2;

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

    /** True from the press on the rail until the release, wherever the pointer went. */
    public boolean draggingBar() {
        return this.draggingBar;
    }

    private void dragTo(double mouseY, int top, int height) {
        int travel = Math.max(1, inner(height) - handleHeight(height));
        double along = (mouseY - top - Metrics.OUTLINE - this.grabWithinHandle) / travel;
        this.offset = Math.max(0, Math.min(maxOffset(), (int) Math.round(along * maxOffset())));
    }

    /** The rail inside its outline: the whole travel of the handle, handle included. */
    private int inner(int height) {
        return height - Metrics.OUTLINE * 2;
    }

    /**
     * The handle is as tall a share of the rail as the viewport is of the content, down
     * to a floor that stays big enough to aim at — and never taller than the rail.
     */
    private int handleHeight(int height) {
        int inner = inner(height);
        int share = inner * this.viewportHeight / Math.max(1, this.contentHeight);
        return Math.min(inner, Math.max(Metrics.ui(18), share));
    }

    private int handleY(int top, int height) {
        int travel = Math.max(0, inner(height) - handleHeight(height));
        return top + Metrics.OUTLINE + travel * this.offset / Math.max(1, maxOffset());
    }
}
