/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.remote;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import fr.clixmods.mcsc.mod.catalog.CatalogFormatException;

/**
 * Reading a provenance sheet, whose four fields are published under French names
 * because they are copied out of the site's own files and are frozen that way in the
 * contract.
 */
class ItemCreditTest {
    private static final String SHEET = """
            {
              "work": {
                "titre": "Aquarelliste",
                "auteur": "someone",
                "licence": "cc-by-4",
                "url": "https://example.invalid/aquarelliste"
              },
              "projects": [
                {"id": "guide", "name": "Guide", "en": "Guide", "es": "Guía"},
                {"id": "herboriste", "name": "Herboriste"}
              ],
              "total": 11
            }""";

    @Test
    void theWorkIsReadUnderTheNamesTheContractPublishes() throws CatalogFormatException {
        ItemCredit credit = ItemCredit.parse(SHEET);

        assertTrue(credit.hasWork());
        assertEquals("Aquarelliste", credit.work().title());
        assertEquals("someone", credit.work().author());
        assertEquals("cc-by-4", credit.work().licence());
        assertEquals("https://example.invalid/aquarelliste", credit.work().url());
    }

    @Test
    void theModelsAreNamedInTheThreeLanguagesTheCatalogueCarries() throws CatalogFormatException {
        ItemCredit credit = ItemCredit.parse(SHEET);

        assertEquals(2, credit.models().size());
        assertEquals("guide", credit.models().get(0).id());
        assertEquals("Guía", credit.models().get(0).name().forLanguage("es_es"));
        assertEquals("Herboriste", credit.models().get(1).name().forLanguage("en_us"),
                "a model with no translation still has a name");
    }

    @Test
    void theOnesTheServerDidNotNameAreCounted() throws CatalogFormatException {
        ItemCredit credit = ItemCredit.parse(SHEET);

        assertEquals(11, credit.total());
        assertEquals(9, credit.unnamedModels(), "eight at most are named, and the rest counted");
    }

    @Test
    void anElementWithNoWorkIsNotAFailure() throws CatalogFormatException {
        ItemCredit credit = ItemCredit.parse("{\"work\": null, \"projects\": [], \"total\": 0}");

        assertFalse(credit.hasWork());
        assertEquals(0, credit.total());
        assertEquals(0, credit.unnamedModels());
    }

    @Test
    void aMissingTotalIsWhatWasNamed() throws CatalogFormatException {
        ItemCredit credit = ItemCredit.parse("{\"projects\": [{\"id\": \"guide\"}]}");

        assertEquals(1, credit.total());
    }

    @Test
    void somethingThatIsNotASheetIsRefused() {
        assertThrows(CatalogFormatException.class, () -> ItemCredit.parse("<html>nope</html>"));
    }
}
