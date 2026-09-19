/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The scale the editor takes, checked against the windows people actually have.
 *
 * <p>This is the test that was missing. The first version of {@code EditorScale} asked
 * for the largest scale that still <em>fitted</em> a design size, and on a 1080p window
 * that is a scale of one: the editor came out drawn at one game pixel per screen pixel,
 * unreadable, and nothing failed. The rule is now a row count and the answers are
 * written down here, so the same class of mistake has somewhere to be caught.
 *
 * <p>The auto scale in each row is the game's own for that window — the largest
 * {@code i} with {@code i < width / 320} and {@code i < height / 240}, which is what
 * {@code Window#calculateScale(0, false)} returns.
 */
class EditorScaleTest {
    @ParameterizedTest(name = "{0}x{1} -> scale {3}")
    @CsvSource({
        // width, height, the game's auto scale, the scale the editor takes
        " 854,  480, 1, 1",
        "1280,  720, 3, 2",
        "1366,  768, 3, 2",
        "1600,  900, 3, 2",
        "1920, 1080, 4, 2",
        "1920, 1200, 5, 2",
        "2560, 1440, 6, 3",
        "3440, 1440, 6, 3",
        "3840, 2160, 9, 4",
    })
    void theEditorTakesTheScaleNearestItsDesignHeight(int width, int height, int auto,
                                                      int expected) {
        assertEquals(expected, EditorScale.scaleFor(height, auto));
    }

    /**
     * Whatever the window, the editor is never drawn at pixels the size the player's own
     * scale would give it, and never smaller than one screen pixel either.
     */
    @ParameterizedTest(name = "{0}x{1}")
    @CsvSource({
        "1280,  720, 3", "1366, 768, 3", "1600, 900, 3",
        "1920, 1080, 4", "2560, 1440, 6", "3840, 2160, 9",
    })
    void theEditorNeverTakesMoreThanTheWindowCanCarry(int width, int height, int auto) {
        int scale = EditorScale.scaleFor(height, auto);
        assertTrue(scale >= 1, "a scale is at least one");
        assertTrue(scale <= auto, "and never past what the game says this window holds");
    }

    /**
     * The rows the editor ends up with stay within reach of the size it is laid out for.
     *
     * <p>Half again is the fold: past it the columns start turning into drawers, and
     * below it the same layout is simply being drawn smaller. Both are handled, but a
     * window landing outside this band means the rule has drifted from the design.
     */
    @ParameterizedTest(name = "{0}x{1}")
    @CsvSource({
        "1280,  720, 3", "1366, 768, 3", "1600, 900, 3", "1920, 1080, 4",
        "1920, 1200, 5", "2560, 1440, 6", "3440, 1440, 6", "3840, 2160, 9",
    })
    void theEditorLandsWithinReachOfItsDesignHeight(int width, int height, int auto) {
        int rows = height / EditorScale.scaleFor(height, auto);
        assertTrue(rows >= EditorScale.DESIGN_ROWS * 2 / 3,
                width + "x" + height + " left the editor only " + rows + " rows");
        assertTrue(rows <= EditorScale.DESIGN_ROWS * 3 / 2,
                width + "x" + height + " left the editor " + rows + " rows, drawn small");
    }
}
