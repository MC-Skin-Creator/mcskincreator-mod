/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.remote;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;

import fr.clixmods.mcsc.mod.catalog.CatalogFormatException;

/** The library as the storage side of the contract keeps it. */
class SavedSkinTest {
    private static final String LIBRARY = """
            [
              {"id": "a1", "name": "Ranger", "at": 1750000000000,
               "data": {"v": 1, "slim": false, "layers": []}},
              {"id": "b2", "name": "Sans date", "data": {"v": 1, "layers": []}},
              {"id": "broken", "name": "No project"},
              "not an entry"
            ]""";

    @Test
    void everyUsableEntryIsRead() throws CatalogFormatException {
        List<SavedSkin> skins = SavedSkin.parseList(LIBRARY);

        assertEquals(2, skins.size(), "an entry with no project is not one the mod can open");
        assertEquals("a1", skins.get(0).id());
        assertEquals("Ranger", skins.get(0).name());
        assertEquals(1750000000000L, skins.get(0).at());
        assertEquals(0, skins.get(1).at(), "no date is a date of nothing, not of now");
    }

    @Test
    void theBodySentBackCarriesTheThreeFieldsTheServerWrites() {
        JsonObject data = JsonParser.parseString("{\"v\": 1, \"slim\": true, \"layers\": []}")
                .getAsJsonObject();
        SavedSkin skin = new SavedSkin("a1", "Ranger", 1750000000000L, data);

        JsonObject body = skin.body();

        assertEquals("Ranger", body.get("name").getAsString());
        assertEquals(1750000000000L, body.get("at").getAsLong());
        assertTrue(body.getAsJsonObject("data").get("slim").getAsBoolean());
        assertEquals(3, body.size(), "the identifier is in the address, not in the body");
    }

    @Test
    void aDrawnIdentifierIsOneTheServerAccepts() {
        assertTrue(SavedSkin.newId().matches("[A-Za-z0-9_-]{1,64}"));
    }

    @Test
    void somethingThatIsNotALibraryIsRefused() {
        assertThrows(CatalogFormatException.class, () -> SavedSkin.parseList("{\"skins\": []}"));
    }
}
