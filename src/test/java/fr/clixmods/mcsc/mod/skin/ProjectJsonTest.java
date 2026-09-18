/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.skin;

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

import fr.clixmods.mcsc.mod.catalog.CatalogCategory;
import fr.clixmods.mcsc.mod.project.Layer;
import fr.clixmods.mcsc.mod.project.SkinProject;
import net.minecraft.world.entity.player.PlayerModelType;

/**
 * The body sent to {@code POST /api/v1/textures}: the stack bottom to top, hidden
 * layers left out, and nothing written that was never moved off its default.
 */
class ProjectJsonTest {
    private final SkinProject project = new SkinProject();
    private final CatalogCategory hats = category("hats", "head", "cap", "crown", "helm");

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

    private static List<String> itemsOf(String json) {
        List<String> items = new ArrayList<>();
        for (var entry : layersOf(json)) {
            items.add(entry.getAsJsonObject().get("item").getAsString());
        }
        return items;
    }

    @Test
    void theStackIsWrittenBottomToTop() {
        add("cap");
        add("crown");
        add("helm");

        assertEquals(List.of("cap", "crown", "helm"), itemsOf(ProjectJson.stack(this.project)));
    }

    @Test
    void aHiddenLayerIsLeftOut() {
        add("cap");
        add("crown").setVisible(false);
        add("helm");

        assertEquals(List.of("cap", "helm"), itemsOf(ProjectJson.stack(this.project)));
    }

    @Test
    void anEmptyProjectStillCarriesItsModel() {
        String json = ProjectJson.stack(this.project);

        assertEquals(0, layersOf(json).size());
        assertEquals("classic", parse(json).get("model").getAsString());
    }

    @Test
    void theModelFollowsTheProject() {
        this.project.setModel(PlayerModelType.SLIM);

        assertEquals("slim", parse(ProjectJson.stack(this.project)).get("model").getAsString());
    }

    @Test
    void anUntouchedLayerCarriesNothingButItsIdentity() {
        add("cap");

        JsonObject entry = layersOf(ProjectJson.stack(this.project)).get(0).getAsJsonObject();

        assertEquals("preset", entry.get("kind").getAsString());
        assertEquals("hats", entry.get("category").getAsString());
        assertEquals("cap", entry.get("item").getAsString());
        assertFalse(entry.has("opacity"), "an opacity left at 100 has nothing to say");
        assertFalse(entry.has("adj"), "adjustments left alone have nothing to say");
        assertEquals(3, entry.size());
    }

    @Test
    void anOpacityThatMovedIsWritten() {
        add("cap").setOpacity(40);

        JsonObject entry = layersOf(ProjectJson.stack(this.project)).get(0).getAsJsonObject();

        assertEquals(40, entry.get("opacity").getAsInt());
        assertFalse(entry.has("adj"));
    }

    @Test
    void oneAdjustmentOffItsDefaultWritesAllThree() {
        add("cap").setHue(30);

        JsonObject entry = layersOf(ProjectJson.stack(this.project)).get(0).getAsJsonObject();
        JsonObject adjustments = entry.getAsJsonObject("adj");

        assertEquals(30, adjustments.get("hue").getAsInt());
        assertEquals(100, adjustments.get("saturation").getAsInt());
        assertEquals(0, adjustments.get("brightness").getAsInt());
        assertFalse(entry.has("opacity"));
    }

    @Test
    void whatWasClampedIsWhatIsSent() {
        add("cap").setOpacity(500);

        JsonObject entry = layersOf(ProjectJson.stack(this.project)).get(0).getAsJsonObject();

        assertFalse(entry.has("opacity"), "a clamped 500 is 100, which is the default");
    }

    @Test
    void aSinglePresetIsOneLayerAndNothingElse() {
        String json = ProjectJson.singlePreset("hats", "cap", false);
        JsonArray layers = layersOf(json);

        assertEquals(1, layers.size());
        JsonObject entry = layers.get(0).getAsJsonObject();
        assertEquals("preset", entry.get("kind").getAsString());
        assertEquals("hats", entry.get("category").getAsString());
        assertEquals("cap", entry.get("item").getAsString());
        assertEquals(3, entry.size());
        assertEquals("classic", parse(json).get("model").getAsString());
    }

    @Test
    void aSinglePresetCanBeAskedForTheSlimModel() {
        assertEquals("slim", parse(ProjectJson.singlePreset("hats", "cap", true)).get("model").getAsString());
    }

    @Test
    void theBodyIsValidJson() {
        add("cap").setOpacity(40);
        add("crown").setBrightness(-10);

        assertTrue(parse(ProjectJson.stack(this.project)).has("layers"));
    }
}
