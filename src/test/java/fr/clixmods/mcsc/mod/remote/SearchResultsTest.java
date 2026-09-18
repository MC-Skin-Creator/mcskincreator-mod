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

/** Reading what a search matched, outfits and all. */
class SearchResultsTest {
    private static final String ANSWER = """
            {
              "q": "vert",
              "total": 3,
              "page": 0,
              "size": 100,
              "results": [
                {"type": "outfit", "cat": null, "id": "tenue-verte"},
                {"type": "preset", "cat": "eyes", "id": "eye-verts-a"},
                {"type": "preset", "cat": "top", "id": "top-vert"}
              ]
            }""";

    @Test
    void everyMatchIsRead() throws CatalogFormatException {
        SearchResults results = SearchResults.parse(ANSWER);

        assertEquals("vert", results.query());
        assertEquals(3, results.total());
        assertEquals(3, results.hits().size());
        assertEquals("eyes", results.hits().get(1).category());
        assertEquals("eye-verts-a", results.hits().get(1).id());
    }

    @Test
    void anOutfitIsToldApartFromAnElement() throws CatalogFormatException {
        SearchResults results = SearchResults.parse(ANSWER);

        assertFalse(results.hits().get(0).isPreset(), "an outfit is a list the mod does not have");
        assertTrue(results.hits().get(1).isPreset());
    }

    @Test
    void anAnswerWithNoMatchIsAnAnswer() throws CatalogFormatException {
        SearchResults results = SearchResults.parse("{\"q\": \"zzz\", \"total\": 0, \"results\": []}");

        assertEquals(0, results.total());
        assertTrue(results.hits().isEmpty());
    }

    @Test
    void somethingThatIsNotAnAnswerIsRefused() {
        assertThrows(CatalogFormatException.class, () -> SearchResults.parse("[]"));
    }
}
