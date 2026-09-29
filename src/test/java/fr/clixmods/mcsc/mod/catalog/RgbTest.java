/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.catalog;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.OptionalInt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The server takes {@code #rrggbb} and nothing else, and the colour window moves
 * through HSL: both directions have to hold.
 */
class RgbTest {
    @Test
    void theCataloguesFormIsRead() {
        assertEquals(OptionalInt.of(0x359E61), Rgb.parse("#359e61"));
        assertEquals(OptionalInt.of(0x359E61), Rgb.parse("#359E61"));
        assertEquals(OptionalInt.of(0x359E61), Rgb.parse("359e61"), "a pasted colour may lose its hash");
    }

    @ParameterizedTest(name = "\"{0}\" is not a colour")
    @ValueSource(strings = {"", "#", "red", "#fff", "#12345", "#1234567", "#12345g", "#ff00ff00"})
    void anythingElseIsRefused(String text) {
        assertTrue(Rgb.parse(text).isEmpty());
    }

    @Test
    void aColourIsWrittenTheOneWayTheServerAccepts() {
        assertEquals("#359e61", Rgb.format(0x359E61));
        assertEquals("#000000", Rgb.format(0));
        assertEquals("#0a0b0c", Rgb.format(0xFF0A0B0C), "the alpha is not the skin's");
    }

    @Test
    void thePrimariesLandWhereTheyShould() {
        assertArrayEquals(new int[] {0, 100, 50}, Rgb.toHsl(0xFF0000));
        assertArrayEquals(new int[] {120, 100, 50}, Rgb.toHsl(0x00FF00));
        assertArrayEquals(new int[] {240, 100, 50}, Rgb.toHsl(0x0000FF));
        assertArrayEquals(new int[] {0, 0, 100}, Rgb.toHsl(0xFFFFFF));
        assertEquals(0xFF0000, Rgb.fromHsl(0, 100, 50));
        assertEquals(0x00FF00, Rgb.fromHsl(120, 100, 50));
        assertEquals(0xFFFFFF, Rgb.fromHsl(200, 30, 100));
        assertEquals(0x000000, Rgb.fromHsl(200, 30, 0));
    }

    @ParameterizedTest(name = "#{0} survives a trip through HSL")
    @ValueSource(ints = {0x359E61, 0xF198BF, 0x493026, 0x145E9E, 0xDBD2D2, 0x808080, 0x0A0909})
    void aRoundTripStaysWithinOneStep(int rgb) {
        int[] hsl = Rgb.toHsl(rgb);
        int back = Rgb.fromHsl(hsl[0], hsl[1], hsl[2]);
        for (int shift = 0; shift <= 16; shift += 8) {
            int before = (rgb >> shift) & 0xFF;
            int after = (back >> shift) & 0xFF;
            // A whole percent of lightness is 2.55 levels of a channel, so that is the
            // most a round trip can move one.
            assertTrue(Math.abs(before - after) <= 4,
                    Rgb.format(rgb) + " came back as " + Rgb.format(back));
        }
    }
}
