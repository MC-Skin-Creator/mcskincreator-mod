/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The rail, taken hold of with the pointer.
 *
 * <p>The band is 100 pixels tall and sits at 50, with its right edge at 200 — numbers
 * chosen only so that the band overflows, which is the condition the wheel and the
 * rail both need before they do anything at all.
 */
class ScrollPaneTest {
    private static final int TOP = 50;
    private static final int HEIGHT = 100;
    private static final int RIGHT = 200;
    /** Inside the rail, which is the last {@link ScrollPane#BAR_WIDTH} pixels. */
    private static final int ON_RAIL = RIGHT - 1;

    private final ScrollPane pane = new ScrollPane();

    ScrollPaneTest() {
        this.pane.setContent(300, HEIGHT);
    }

    private boolean press(double mouseY) {
        return this.pane.barMouseDown(ON_RAIL, mouseY, RIGHT, TOP, HEIGHT);
    }

    private void drag(double mouseY) {
        this.pane.barMouseDrag(mouseY, TOP, HEIGHT);
    }

    @Test
    void pressOnTheRailTakesTheGesture() {
        assertTrue(press(TOP + HEIGHT / 2.0));
    }

    @Test
    void pressBesideTheRailIsNotTheRails() {
        assertFalse(this.pane.barMouseDown(RIGHT - ScrollPane.BAR_WIDTH - 1, TOP + 10,
                RIGHT, TOP, HEIGHT));
        assertFalse(this.pane.barMouseDown(ON_RAIL, TOP - 1, RIGHT, TOP, HEIGHT));
        assertFalse(this.pane.barMouseDown(ON_RAIL, TOP + HEIGHT, RIGHT, TOP, HEIGHT));
    }

    @Test
    void aBandThatDoesNotOverflowHasNoRailToPress() {
        ScrollPane full = new ScrollPane();
        full.setContent(HEIGHT / 2, HEIGHT);
        assertFalse(full.barMouseDown(ON_RAIL, TOP + 10, RIGHT, TOP, HEIGHT));
    }

    @Test
    void draggingDownScrollsDownAndTheEndsAreTheEnds() {
        press(TOP);
        drag(TOP + HEIGHT / 2.0);
        assertTrue(this.pane.offset() > 0);

        drag(TOP + HEIGHT * 2.0);
        assertEquals(this.pane.maxOffset(), this.pane.offset());

        drag(TOP - HEIGHT);
        assertEquals(0, this.pane.offset());
    }

    /**
     * Taking hold of the handle leaves it where it was taken hold of.
     *
     * <p>120 pixels of content in a 100 pixel band is a handle 83 pixels tall over 17
     * pixels of travel: scrolled to the bottom it covers 67 to 150, so 88 is well
     * inside it and well above its middle. Snapping that middle under the pointer —
     * which is what a press on the empty rail does — would send the band back to the
     * top, and that is precisely what grabbing must not do.
     */
    @Test
    void grabbingTheHandleDoesNotMakeItJump() {
        ScrollPane held = new ScrollPane();
        held.setContent(120, HEIGHT);
        held.scroll(-100);
        assertEquals(held.maxOffset(), held.offset());

        assertTrue(held.barMouseDown(ON_RAIL, 88, RIGHT, TOP, HEIGHT));
        assertEquals(held.maxOffset(), held.offset());
    }

    /** A press on the rail away from the handle is the other gesture: it jumps there. */
    @Test
    void pressingTheRailBelowTheHandleMovesIt() {
        assertEquals(0, this.pane.offset());
        press(TOP + HEIGHT - 1);
        assertTrue(this.pane.offset() > 0);
    }

    @Test
    void releasingEndsTheDrag() {
        press(TOP);
        this.pane.barMouseUp();
        int released = this.pane.offset();

        drag(TOP + HEIGHT);
        assertEquals(released, this.pane.offset());
    }
}
