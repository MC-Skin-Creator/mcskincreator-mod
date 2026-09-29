/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.skin;

import static fr.clixmods.mcsc.mod.Fixtures.catalog;
import static fr.clixmods.mcsc.mod.Fixtures.category;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;

import fr.clixmods.mcsc.mod.catalog.Catalog;
import fr.clixmods.mcsc.mod.catalog.CatalogCategory;
import fr.clixmods.mcsc.mod.project.Layer;
import fr.clixmods.mcsc.mod.project.SkinProject;
import net.minecraft.world.entity.player.PlayerModelType;

/**
 * The one document the server accepts, written and read back.
 *
 * <p>Three of its rules are silent when broken, and each has a test of its own here:
 * the model is a {@code slim} boolean rather than a {@code model} string, a layer names
 * its element with {@code cat} and {@code preset}, and the numbers are factors rather
 * than the whole percentages the sliders of this mod work in.
 */
class ProjectJsonTest {
    private final SkinProject project = new SkinProject();
    private final CatalogCategory hats = category("hats", "head", "cap", "crown", "helm");
    private final Catalog catalog = catalog(this.hats);

    private Layer add(String itemId) {
        return this.project.add(this.hats, this.hats.items().stream()
                .filter(item -> item.id().equals(itemId))
                .findFirst()
                .orElseThrow(), "en_us");
    }

    private static JsonObject parse(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    private static JsonArray layersOf(String json) {
        return parse(json).getAsJsonArray("layers");
    }

    private static List<String> presetsOf(String json) {
        List<String> presets = new ArrayList<>();
        for (var entry : layersOf(json)) {
            presets.add(entry.getAsJsonObject().get("preset").getAsString());
        }
        return presets;
    }

    @Test
    void theStackIsWrittenBottomToTop() {
        add("cap");
        add("crown");
        add("helm");

        assertEquals(List.of("cap", "crown", "helm"), presetsOf(ProjectJson.project(this.project)));
    }

    @Test
    void aHiddenLayerIsWrittenOutAndSaidToBeHidden() {
        add("cap");
        add("crown").setVisible(false);
        add("helm");

        List<String> presets = presetsOf(ProjectJson.project(this.project));
        JsonObject hidden = layersOf(ProjectJson.project(this.project)).get(1).getAsJsonObject();

        assertEquals(List.of("cap", "crown", "helm"), presets,
                "dropping it would lose the layer the moment the project is stored");
        assertFalse(hidden.get("visible").getAsBoolean());
    }

    @Test
    void anEmptyProjectStillCarriesItsModel() {
        String json = ProjectJson.project(this.project);

        assertEquals(0, layersOf(json).size());
        assertEquals(ProjectJson.VERSION, parse(json).get("v").getAsInt());
        assertFalse(parse(json).get("slim").getAsBoolean());
    }

    @Test
    void theModelIsABooleanCalledSlim() {
        this.project.setModel(PlayerModelType.SLIM);

        JsonObject document = parse(ProjectJson.project(this.project));

        assertTrue(document.get("slim").getAsBoolean());
        assertFalse(document.has("model"), "\"model\" is a name the server does not read");
    }

    @Test
    void aLayerNamesItsElementWithCatAndPreset() {
        add("cap");

        JsonObject entry = layersOf(ProjectJson.project(this.project)).get(0).getAsJsonObject();

        assertEquals("preset", entry.get("kind").getAsString());
        assertEquals("hats", entry.get("cat").getAsString());
        assertEquals("cap", entry.get("preset").getAsString());
        assertFalse(entry.has("category"), "the validator refuses a layer without \"cat\"");
        assertFalse(entry.has("item"));
    }

    @Test
    void anUntouchedLayerCarriesNothingItDidNotMove() {
        add("cap");

        JsonObject entry = layersOf(ProjectJson.project(this.project)).get(0).getAsJsonObject();

        assertFalse(entry.has("opacity"), "an opacity left at 100 has nothing to say");
        assertFalse(entry.has("adj"), "adjustments left alone have nothing to say");
        assertFalse(entry.has("visible"), "a visible layer is the default");
    }

    @Test
    void anOpacityIsWrittenAsAFactor() {
        add("cap").setOpacity(40);

        JsonObject entry = layersOf(ProjectJson.project(this.project)).get(0).getAsJsonObject();

        assertEquals(0.4, entry.get("opacity").getAsDouble(), 1e-9,
                "the server bounds opacity to 0..1, not to 0..100");
    }

    @Test
    void oneAdjustmentOffItsDefaultWritesAllThreeInTheServersUnits() {
        add("cap").setHue(30);

        JsonObject adjustments = layersOf(ProjectJson.project(this.project))
                .get(0).getAsJsonObject().getAsJsonObject("adj");

        assertEquals(30, adjustments.get("hue").getAsInt(), "hue is in degrees on both sides");
        assertEquals(1.0, adjustments.get("sat").getAsDouble(), 1e-9);
        assertEquals(0.0, adjustments.get("lum").getAsDouble(), 1e-9);
        assertFalse(adjustments.has("saturation"), "\"saturation\" is a name the server ignores");
        assertFalse(adjustments.has("brightness"));
    }

    @Test
    void everyAdjustmentStaysInsideTheBoundsTheServerAccepts() {
        Layer layer = add("cap");
        layer.setHue(500);
        layer.setSaturation(500);
        layer.setBrightness(500);

        JsonObject adjustments = layersOf(ProjectJson.project(this.project))
                .get(0).getAsJsonObject().getAsJsonObject("adj");

        assertEquals(180, adjustments.get("hue").getAsInt());
        assertEquals(2.0, adjustments.get("sat").getAsDouble(), 1e-9);
        assertEquals(0.5, adjustments.get("lum").getAsDouble(), 1e-9);
    }

    @Test
    void aRegionTheServerDoesNotKnowIsLeftOut() {
        CatalogCategory elsewhere = category("odd", "somewhere", "thing");
        this.project.add(elsewhere, elsewhere.items().get(0), "en_us");

        JsonObject entry = layersOf(ProjectJson.project(this.project)).get(0).getAsJsonObject();

        assertFalse(entry.has("region"), "an unknown region is refused, and defaults to base");
    }

    @Test
    void aKnownRegionTravelsWithTheLayer() {
        add("cap");

        JsonObject entry = layersOf(ProjectJson.project(this.project)).get(0).getAsJsonObject();

        assertEquals("head", entry.get("region").getAsString());
    }

    @Test
    void aSinglePresetIsOneLayerAndNothingElse() {
        String json = ProjectJson.singlePreset("hats", "cap", false);
        JsonArray layers = layersOf(json);

        assertEquals(1, layers.size());
        JsonObject entry = layers.get(0).getAsJsonObject();
        assertEquals("preset", entry.get("kind").getAsString());
        assertEquals("hats", entry.get("cat").getAsString());
        assertEquals("cap", entry.get("preset").getAsString());
        assertEquals(3, entry.size());
        assertFalse(parse(json).get("slim").getAsBoolean());
    }

    @Test
    void aSinglePresetCanBeAskedForTheSlimModel() {
        assertTrue(parse(ProjectJson.singlePreset("hats", "cap", true)).get("slim").getAsBoolean());
    }

    @Test
    void aProjectComesBackAsItWentOut() {
        this.project.setModel(PlayerModelType.SLIM);
        add("cap").setOpacity(40);
        Layer crown = add("crown");
        crown.setVisible(false);
        crown.setHue(-30);
        crown.setSaturation(150);
        crown.setBrightness(-20);

        String written = ProjectJson.project(this.project);
        SkinProject reopened = new SkinProject();
        int dropped = ProjectJson.read(parse(written), reopened, this.catalog, "en_us");

        assertEquals(0, dropped);
        assertTrue(reopened.isSlim());
        assertEquals(2, reopened.layers().size());
        Layer first = reopened.layers().get(0);
        Layer second = reopened.layers().get(1);
        assertEquals("cap", first.itemId());
        assertEquals(40, first.opacity());
        assertTrue(first.visible());
        assertFalse(second.visible());
        assertEquals(-30, second.hue());
        assertEquals(150, second.saturation());
        assertEquals(-20, second.brightness());
    }

    @Test
    void aLayerTheCatalogueNoLongerCarriesIsCountedAndLeftBehind() {
        String stored = """
                {"v": 1, "slim": false, "layers": [
                  {"kind": "preset", "cat": "hats", "preset": "cap"},
                  {"kind": "preset", "cat": "hats", "preset": "gone"},
                  {"kind": "preset", "cat": "vanished", "preset": "cap"},
                  {"kind": "paint", "paint": "AAAA"}
                ]}""";

        int dropped = ProjectJson.read(parse(stored), this.project, this.catalog, "en_us");

        assertEquals(3, dropped, "a removed element, a removed category, and a drawing layer");
        assertEquals(List.of("cap"), this.project.layers().stream().map(Layer::itemId).toList());
    }

    @Test
    void readingReplacesWhateverWasBeingEdited() {
        add("cap");
        add("crown");

        ProjectJson.read(parse("{\"v\": 1, \"slim\": false, \"layers\": []}"),
                this.project, this.catalog, "en_us");

        assertTrue(this.project.isEmpty(), "opening a saved skin is not stacking it on this one");
    }

    private final CatalogCategory coloredHats = new CatalogCategory("hats", "head",
            new fr.clixmods.mcsc.mod.catalog.CatalogText("Hats", "Hats", "Hats"), false,
            fr.clixmods.mcsc.mod.catalog.ThumbCrop.HEAD, "", List.of(
                    new fr.clixmods.mcsc.mod.catalog.CatalogItem("cap",
                            new fr.clixmods.mcsc.mod.catalog.CatalogText("Cap", "Cap", "Cap"),
                            0, fr.clixmods.mcsc.mod.catalog.CatalogItem.NONE, null, "",
                            java.util.Map.of("main", 0x359E61))));

    @Test
    void aRecolouredKeyIsWrittenAsTheServerReadsIt() {
        Layer layer = this.project.add(this.coloredHats, this.coloredHats.items().get(0), "en_us");
        String untouched = ProjectJson.project(this.project);
        layer.setColor("main", 0xFF0000);

        JsonObject entry = layersOf(ProjectJson.project(this.project)).get(0).getAsJsonObject();

        assertFalse(layersOf(untouched).get(0).getAsJsonObject().has("colors"),
                "the element's own colours have nothing to say");
        assertEquals("#ff0000", entry.getAsJsonObject("colors").get("main").getAsString());
    }

    @Test
    void aRecolouredKeyComesBackFromStorage() {
        this.project.add(this.coloredHats, this.coloredHats.items().get(0), "en_us")
                .setColor("main", 0x123456);
        String written = ProjectJson.project(this.project);

        SkinProject reopened = new SkinProject();
        ProjectJson.read(parse(written), reopened, catalog(this.coloredHats), "en_us");

        assertEquals(0x123456, reopened.layers().get(0).color("main"));
    }

    @Test
    void aStoredKeyTheElementNoLongerHasIsLetGo() {
        String stored = """
                {"v": 1, "slim": false, "layers": [{"kind": "preset", "cat": "hats",
                 "preset": "cap", "colors": {"main": "#0000ff", "feather": "#ff0000"}}]}
                """;

        int dropped = ProjectJson.read(parse(stored), this.project, catalog(this.coloredHats), "en_us");

        assertEquals(0, dropped, "the layer itself is still there");
        assertEquals(List.of("main"), this.project.layers().get(0).colorKeys());
        assertEquals(0x0000FF, this.project.layers().get(0).color("main"));
    }
}
