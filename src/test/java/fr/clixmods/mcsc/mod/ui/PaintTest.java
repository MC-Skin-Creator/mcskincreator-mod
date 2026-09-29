/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * What the pointer is allowed to touch while something is open over the screen.
 *
 * <p>It was allowed to touch everything. An open dropdown drew over the layer column,
 * and the row underneath went on lighting up, offering its tooltip and pulling the
 * model's gaze — because every widget asked the frame whether the mouse was inside its
 * own rectangle, and nothing in the frame knew there was a menu in the way.
 */
class PaintTest {
    private static final class Box extends Element {
        private Box(int x, int y, int width, int height) {
            setBounds(x, y, width, height);
        }

        @Override
        public void draw(Paint paint) {
        }
    }

    private static Paint at(int mouseX, int mouseY, boolean blocked) {
        return new Paint(null, mouseX, mouseY, 0L, null, blocked);
    }

    @Test
    void anElementUnderThePointerIsHot() {
        assertTrue(at(15, 15, false).hot(new Box(10, 10, 20, 20)));
        assertFalse(at(5, 15, false).hot(new Box(10, 10, 20, 20)));
    }

    @Test
    void nothingIsHotWhileSomethingIsOpenOverTheScreen() {
        assertFalse(at(15, 15, true).hot(new Box(10, 10, 20, 20)),
                "a row under an open menu must not light up");
        assertFalse(at(15, 15, true).over(10, 10, 20, 20),
                "nor any part of one");
    }

    @Test
    void theThingThatIsOpenAsksTheSameFrameAndGetsRealAnswers() {
        Paint blocked = at(15, 15, true);
        assertTrue(blocked.unblocked().hot(new Box(10, 10, 20, 20)));
        assertTrue(blocked.unblocked().over(10, 10, 20, 20));
        assertFalse(blocked.unblocked().blocked());
    }

    /** The focus ring is a pointer too, as far as a widget is concerned. */
    @Test
    void aFocusedElementIsHotWithThePointerElsewhere() {
        Element box = new Box(10, 10, 20, 20);
        assertTrue(new Paint(null, 500, 500, 0L, box, false).hot(box));
        assertFalse(new Paint(null, 500, 500, 0L, box, true).hot(box));
    }
}
