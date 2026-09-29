/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.skin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import fr.clixmods.mcsc.mod.Fixtures;
import fr.clixmods.mcsc.mod.catalog.CatalogModel;

/** Recognising a skin by its pixels, which is how the account's skin finds its model. */
class SkinPixelsTest {

    private static byte[] sheet(int red) {
        byte[] rgba = new byte[Composite.BYTES];
        for (int offset = 0; offset < rgba.length; offset += 4) {
            rgba[offset] = (byte) red;
            rgba[offset + 3] = (byte) 0xFF;
        }
        return rgba;
    }

    @Test
    void aSkinIsItself() {
        assertTrue(SkinPixels.same(sheet(10), sheet(10)));
    }

    @Test
    void oneTexelIsEnoughToTellTwoSkinsApart() {
        byte[] other = sheet(10);
        other[4 * 100 + 1] = 1;

        assertFalse(SkinPixels.same(sheet(10), other));
    }

    @Test
    void whatAnInvisibleTexelHoldsDoesNotCount() {
        byte[] first = sheet(10);
        byte[] second = sheet(10);
        first[3] = 0;
        second[3] = 0;
        second[0] = 99;

        assertTrue(SkinPixels.same(first, second));
        assertEquals(SkinPixels.fingerprint(first), SkinPixels.fingerprint(second));
    }

    @Test
    void differentSkinsHaveDifferentFingerprints() {
        assertNotEquals(SkinPixels.fingerprint(sheet(1)), SkinPixels.fingerprint(sheet(2)));
    }

    @Test
    void aSheetThatIsNotASkinMatchesNothing() {
        assertFalse(SkinPixels.same(sheet(1), new byte[12]));
        assertFalse(SkinPixels.same(null, sheet(1)));
    }

    @Test
    void theModelWhosePictureIsTheSkinIsFound() {
        CatalogModel rose = Fixtures.readyMade("rose", CatalogModel.Kind.MODEL, false, "skin/rose");
        CatalogModel ash = Fixtures.readyMade("ash", CatalogModel.Kind.MODEL, false, "skin/ash");

        Optional<CatalogModel> found = SkinPixels.match(List.of(rose, ash),
                List.of(sheet(1), sheet(2)), sheet(2));

        assertEquals(Optional.of(ash), found);
    }

    @Test
    void aSkinThatIsNoModelFindsNone() {
        CatalogModel rose = Fixtures.readyMade("rose", CatalogModel.Kind.MODEL, false, "skin/rose");

        assertEquals(Optional.empty(), SkinPixels.match(List.of(rose), List.of(sheet(1)), sheet(3)));
        assertEquals(Optional.empty(), SkinPixels.match(List.of(rose), List.of(sheet(1)), null));
    }

    @Test
    void aModelStillWaitingOnItsPiecesMatchesNothing() {
        CatalogModel rose = Fixtures.readyMade("rose", CatalogModel.Kind.MODEL, false, "skin/rose");

        assertEquals(Optional.empty(), SkinPixels.match(List.of(rose),
                List.of(new byte[Composite.BYTES]), sheet(1)));
    }
}
