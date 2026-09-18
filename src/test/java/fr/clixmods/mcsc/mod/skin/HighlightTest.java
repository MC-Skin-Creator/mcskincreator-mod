/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.skin;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import fr.clixmods.mcsc.mod.catalog.CatalogCategory;
import fr.clixmods.mcsc.mod.catalog.CatalogItem;
import fr.clixmods.mcsc.mod.catalog.CatalogText;
import fr.clixmods.mcsc.mod.catalog.ThumbCrop;
import fr.clixmods.mcsc.mod.project.Layer;

/**
 * What the layer highlight paints, and when it stops painting it.
 *
 * <p>The colour is the part worth pinning down: a single flash colour disappears against
 * half the catalogue, so it is chosen per texel from what lies underneath. Getting that
 * backwards would make the pulse invisible on exactly the layers it was built for, and
 * nothing would say so.
 */
class HighlightTest {
    private static final int TEXELS = FrontSprite.SKIN_SIZE * FrontSprite.SKIN_SIZE;

    private static Layer layer() {
        CatalogItem item = new CatalogItem("cap", new CatalogText("Casquette", "Cap", "Gorra"),
                4, 5, null, "");
        CatalogCategory category = new CatalogCategory("hats", "head",
                new CatalogText("Chapeaux", "Hats", "Sombreros"),
                false, ThumbCrop.HEAD, "", List.of(item));
        return new Layer(category, item, "en");
    }

    private static byte[] filled(int red, int green, int blue, int alpha) {
        byte[] buffer = new byte[Composite.BYTES];
        for (int texel = 0; texel < TEXELS; texel++) {
            buffer[texel * 4] = (byte) red;
            buffer[texel * 4 + 1] = (byte) green;
            buffer[texel * 4 + 2] = (byte) blue;
            buffer[texel * 4 + 3] = (byte) alpha;
        }
        return buffer;
    }

    /** One opaque texel at the start of the sheet, and nothing anywhere else. */
    private static byte[] oneTexel() {
        byte[] buffer = new byte[Composite.BYTES];
        buffer[3] = (byte) 0xFF;
        return buffer;
    }

    @Test
    void theFlashIsGoldOverSomethingDark() {
        byte[] base = filled(20, 20, 24, 255);
        byte[] out = Highlight.blend(base, oneTexel(), 1.0F, new byte[Composite.BYTES]);

        assertTrue((out[0] & 0xFF) > 200, "gold has a strong red");
        assertTrue((out[1] & 0xFF) > 150, "gold has a strong green");
        assertTrue((out[2] & 0xFF) < 120, "gold has little blue");
    }

    @Test
    void theFlashIsNearlyBlackOverSomethingLight() {
        byte[] base = filled(240, 240, 235, 255);
        byte[] out = Highlight.blend(base, oneTexel(), 1.0F, new byte[Composite.BYTES]);

        assertTrue((out[0] & 0xFF) < 60, "the flash goes dark over a light backdrop");
        assertTrue((out[1] & 0xFF) < 60);
        assertTrue((out[2] & 0xFF) < 60);
    }

    @Test
    void onlyTheLayersOwnTexelsAreTouched() {
        byte[] base = filled(20, 20, 24, 255);
        byte[] out = Highlight.blend(base, oneTexel(), 1.0F, new byte[Composite.BYTES]);

        for (int offset = 4; offset < Composite.BYTES; offset += 4) {
            assertArrayEquals(
                    new byte[] {base[offset], base[offset + 1], base[offset + 2], base[offset + 3]},
                    new byte[] {out[offset], out[offset + 1], out[offset + 2], out[offset + 3]},
                    "texel " + offset / 4 + " is not the layer's and must be left alone");
        }
    }

    /**
     * The one case the whole thing exists for: a layer with nothing visible still shows
     * up, because the alpha is raised as well as the colour written.
     */
    @Test
    void aLayerWithNothingOverItStillShows() {
        byte[] base = new byte[Composite.BYTES];
        byte[] out = Highlight.blend(base, oneTexel(), 1.0F, new byte[Composite.BYTES]);

        assertTrue((out[3] & 0xFF) > 200, "an empty texel is lit by its alpha alone");
        assertTrue((out[0] & 0xFF) > 200, "and takes the flash colour whole");
    }

    @Test
    void halfwayThroughTheBeatTheColourIsHalfway() {
        byte[] base = filled(0, 0, 0, 255);
        byte[] out = Highlight.blend(base, oneTexel(), 0.5F, new byte[Composite.BYTES]);

        int red = out[0] & 0xFF;
        assertTrue(red > 110 && red < 145, "half of the way to gold, not all of it: " + red);
    }

    @Test
    void nothingIsPaintedWhenThereIsNothingToPaint() {
        byte[] base = filled(20, 20, 24, 255);
        byte[] out = new byte[Composite.BYTES];

        assertSame(base, Highlight.blend(base, oneTexel(), 0.0F, out),
                "a strength of zero cannot change a byte, so the base is handed back");
        assertSame(base, Highlight.blend(base, new byte[Composite.BYTES], 1.0F, out),
                "a layer with no opaque texel cannot change a byte either");
        assertSame(base, Highlight.blend(base, null, 1.0F, out));
    }

    @Test
    void theBeatRisesFromNothing() {
        Highlight highlight = new Highlight();
        Layer layer = layer();
        highlight.show(layer, 1_000);

        assertSame(layer, highlight.layer());
        assertTrue(highlight.strength(1_000) < 0.01F, "the beat starts from nothing");
        assertTrue(highlight.strength(1_300) > 0.9F, "and is full half a beat later");
    }

    @Test
    void leavingTheRowFadesRatherThanCutting() {
        Highlight highlight = new Highlight();
        highlight.show(layer(), 1_000);
        highlight.hide(1_300);

        float leaving = highlight.strength(1_300);
        assertTrue(leaving > 0.9F, "the fade starts from where the beat had got to");
        assertTrue(highlight.strength(1_390) < leaving, "and comes down from there");
        assertFalse(highlight.active(1_600), "and is over within the fade");
        assertNull(highlight.layer(), "which also lets go of the layer");
    }

    /**
     * A deleted layer's row is gone, so it will never report the pointer leaving it.
     * Nothing else would ever turn the pulse off.
     */
    @Test
    void droppingStopsAtOnce() {
        Highlight highlight = new Highlight();
        highlight.show(layer(), 1_000);
        highlight.drop();

        assertNull(highlight.layer());
        assertFalse(highlight.active(1_300));
    }
}
