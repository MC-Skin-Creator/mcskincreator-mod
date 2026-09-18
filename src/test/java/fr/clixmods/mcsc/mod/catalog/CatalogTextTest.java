/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The fallback chain: an entry always shows a name rather than a blank slot, whatever
 * the player's language and whatever the catalogue left out.
 */
class CatalogTextTest {
    private static final CatalogText FULL = new CatalogText("Casquette", "Cap", "Gorra");

    @Nested
    class French {
        @ParameterizedTest
        @ValueSource(strings = {"fr_fr", "fr_ca", "fr", "FR_FR"})
        void takesTheCataloguesOwnWording(String language) {
            assertEquals("Casquette", FULL.forLanguage(language));
        }

        @Test
        void fallsBackToEnglishThenSpanish() {
            assertEquals("Cap", new CatalogText("", "Cap", "Gorra").forLanguage("fr_fr"));
            assertEquals("Gorra", new CatalogText("", "", "Gorra").forLanguage("fr_fr"));
        }
    }

    @Nested
    class Spanish {
        @Test
        void takesTheSpanishTranslation() {
            assertEquals("Gorra", FULL.forLanguage("es_es"));
        }

        @Test
        void fallsBackToEnglishThenTheCataloguesOwnWording() {
            assertEquals("Cap", new CatalogText("Casquette", "Cap", "").forLanguage("es_mx"));
            assertEquals("Casquette", new CatalogText("Casquette", "", "").forLanguage("es_mx"));
        }
    }

    @Nested
    class EveryOtherLanguage {
        @ParameterizedTest
        @ValueSource(strings = {"en_us", "en_gb", "de_de", "ja_jp", "zz"})
        void readsEnglish(String language) {
            assertEquals("Cap", FULL.forLanguage(language));
        }

        @Test
        void fallsBackToTheCataloguesOwnWordingThenSpanish() {
            assertEquals("Casquette", new CatalogText("Casquette", "", "Gorra").forLanguage("de_de"));
            assertEquals("Gorra", new CatalogText("", "", "Gorra").forLanguage("de_de"));
        }
    }

    @Test
    void noLanguageAtAllIsTreatedAsAnUnknownOne() {
        assertEquals("Cap", FULL.forLanguage(null));
        assertEquals("Cap", FULL.forLanguage(""));
    }

    @Test
    void blankIsNotAName() {
        assertEquals("Cap", new CatalogText("   ", "Cap", "Gorra").forLanguage("fr_fr"));
    }

    @Test
    void anEntryWithNothingToSayStaysEmptyRatherThanFailing() {
        assertEquals("", CatalogText.EMPTY.forLanguage("fr_fr"));
        assertEquals("", CatalogText.EMPTY.forLanguage("es_es"));
        assertEquals("", CatalogText.EMPTY.forLanguage("en_us"));
    }
}
