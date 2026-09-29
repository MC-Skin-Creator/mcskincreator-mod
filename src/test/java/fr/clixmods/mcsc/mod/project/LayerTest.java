/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import fr.clixmods.mcsc.mod.catalog.CatalogCategory;
import fr.clixmods.mcsc.mod.catalog.CatalogItem;
import fr.clixmods.mcsc.mod.catalog.CatalogText;
import fr.clixmods.mcsc.mod.catalog.ThumbCrop;

/**
 * A layer holds the server validator's bounds, clamped on the way in: a slider dragged
 * past its end must not be able to build a project the server would refuse.
 */
class LayerTest {
    private static final CatalogText LABELS = new CatalogText("Casquette", "Cap", "Gorra");

    private static CatalogCategory category() {
        return new CatalogCategory("hats", "head", new CatalogText("Chapeaux", "Hats", "Sombreros"),
                false, ThumbCrop.HEAD, "", List.of(item()));
    }

    private static CatalogItem item() {
        return new CatalogItem("cap", LABELS, 4, 5, null, "");
    }

    private static Layer layer() {
        return new Layer(category(), item(), "en_us");
    }

    @ParameterizedTest(name = "opacity {0} is held at {1}")
    @CsvSource({"50, 50", "0, 0", "100, 100", "-1, 0", "-1000, 0", "101, 100", "1000, 100"})
    void opacityStaysBetweenZeroAndAHundred(int asked, int kept) {
        Layer layer = layer();

        layer.setOpacity(asked);

        assertEquals(kept, layer.opacity());
    }

    @ParameterizedTest(name = "hue {0} is held at {1}")
    @CsvSource({"0, 0", "90, 90", "-180, -180", "180, 180", "-181, -180", "181, 180", "9000, 180"})
    void hueStaysWithinHalfATurnEitherWay(int asked, int kept) {
        Layer layer = layer();

        layer.setHue(asked);

        assertEquals(kept, layer.hue());
    }

    @ParameterizedTest(name = "saturation {0} is held at {1}")
    @CsvSource({"100, 100", "0, 0", "200, 200", "-1, 0", "201, 200"})
    void saturationStaysBetweenZeroAndTwoHundred(int asked, int kept) {
        Layer layer = layer();

        layer.setSaturation(asked);

        assertEquals(kept, layer.saturation());
    }

    @ParameterizedTest(name = "brightness {0} is held at {1}")
    @CsvSource({"0, 0", "-50, -50", "50, 50", "-51, -50", "51, 50"})
    void brightnessStaysWithinFiftyEitherWay(int asked, int kept) {
        Layer layer = layer();

        layer.setBrightness(asked);

        assertEquals(kept, layer.brightness());
    }

    @Test
    void aFreshLayerIsVisibleOpaqueAndUnadjusted() {
        Layer layer = layer();

        assertTrue(layer.visible());
        assertEquals(100, layer.opacity());
        assertTrue(layer.isUnadjusted());
    }

    @Test
    void opacityIsNotAnAdjustment() {
        Layer layer = layer();

        layer.setOpacity(30);

        assertTrue(layer.isUnadjusted());
    }

    @Test
    void anyAdjustmentOffItsDefaultCountsAsAdjusted() {
        assertAdjusted(layer -> layer.setHue(1));
        assertAdjusted(layer -> layer.setSaturation(99));
        assertAdjusted(layer -> layer.setBrightness(-1));
    }

    private void assertAdjusted(Consumer<Layer> change) {
        Layer layer = layer();

        change.accept(layer);

        assertFalse(layer.isUnadjusted());

        layer.resetAdjustments();

        assertTrue(layer.isUnadjusted());
        assertEquals(0, layer.hue());
        assertEquals(100, layer.saturation());
        assertEquals(0, layer.brightness());
    }

    @Test
    void aCopyCarriesTheStateAndThenGoesItsOwnWay() {
        Layer layer = layer();
        layer.setVisible(false);
        layer.setOpacity(40);
        layer.setHue(12);
        layer.setSaturation(150);
        layer.setBrightness(-20);

        Layer copy = layer.copy();

        assertEquals(layer.categoryId(), copy.categoryId());
        assertEquals(layer.region(), copy.region());
        assertEquals(layer.itemId(), copy.itemId());
        assertEquals(layer.visible(), copy.visible());
        assertEquals(layer.opacity(), copy.opacity());
        assertEquals(layer.hue(), copy.hue());
        assertEquals(layer.saturation(), copy.saturation());
        assertEquals(layer.brightness(), copy.brightness());

        copy.setOpacity(90);

        assertEquals(40, layer.opacity());
    }

    @Test
    void theSlimBufferIsUsedOnlyForTheSlimModel() {
        Layer layer = layer();

        assertEquals(4, layer.atlasIndex(false));
        assertEquals(5, layer.atlasIndex(true));
    }

    @Test
    void anElementWithoutASlimBufferKeepsTheClassicOne() {
        Layer layer = new Layer(category(), new CatalogItem("cap", LABELS, 4, CatalogItem.NONE, null, ""), "en_us");

        assertEquals(4, layer.atlasIndex(false));
        assertEquals(4, layer.atlasIndex(true));
    }

    @Test
    void theLabelsAreResolvedOnceForThePlayersLanguage() {
        assertEquals("Casquette", new Layer(category(), item(), "fr_fr").name().getString());
        assertEquals("Gorra", new Layer(category(), item(), "es_es").name().getString());
        assertEquals("Cap", new Layer(category(), item(), "en_us").name().getString());
        assertEquals("Chapeaux", new Layer(category(), item(), "fr_fr").categoryName().getString());
    }

    private static Layer colored() {
        java.util.Map<String, Integer> colors = new java.util.LinkedHashMap<>();
        colors.put("main", 0x359E61);
        colors.put("band", 0xFFFFFF);
        CatalogItem hat = new CatalogItem("cap", LABELS, 4, 5, null, "", colors);
        return new Layer(category(), hat, "en_us");
    }

    @Test
    void theKeysAreTheElementsInTheCataloguesOrder() {
        Layer layer = colored();

        assertEquals(List.of("main", "band"), layer.colorKeys());
        assertEquals(0x359E61, layer.color("main"));
        assertTrue(layer.changedColors().isEmpty(), "a fresh layer has changed nothing");
    }

    @Test
    void aKeyTheElementDoesNotDeclareIsRefused() {
        Layer layer = colored();

        assertFalse(layer.setColor("cape", 0xFF0000));
        assertEquals(List.of("main", "band"), layer.colorKeys());
        assertTrue(layer().colorKeys().isEmpty(), "an element without keys has none to offer");
    }

    @Test
    void onlyTheKeysMovedOffTheirOwnColourAreCarried() {
        Layer layer = colored();

        layer.setColor("band", 0xFF0000);
        assertEquals(java.util.Map.of("band", 0xFF0000), layer.changedColors());

        layer.setColor("band", 0xFFFFFF);
        assertTrue(layer.changedColors().isEmpty(), "put back is not a change");
    }

    @Test
    void theOriginalColoursButtonPutsTheKeysBackToo() {
        Layer layer = colored();
        layer.setColor("main", 0x123456);
        layer.setHue(40);

        layer.resetAdjustments();

        assertEquals(0x359E61, layer.color("main"));
        assertTrue(layer.isUnadjusted());
    }

    @Test
    void aCopyDoesNotShareItsColours() {
        Layer layer = colored();
        Layer copy = layer.copy();

        layer.setColor("main", 0x123456);

        assertEquals(0x359E61, copy.color("main"), "a history snapshot must not move with the layer");
    }
}
