/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Reading a catalogue payload, including the two details the parser's own javadoc
 * warns are silent when got wrong: a category names itself with {@code label} while an
 * element uses {@code name}, and an element's rank in the atlas is not its rank in
 * {@code items}.
 */
class CatalogParserTest {
    /** Close to what the site actually serves, extra fields and all. */
    private static final String PAYLOAD = """
            {
              "credits": ["someone"],
              "projects": [],
              "categories": [
                {
                  "id": "skin",
                  "region": "body",
                  "label": "Peau",
                  "en": "Skin",
                  "es": "Piel",
                  "single": true,
                  "thumbCrop": "all",
                  "atlas": {"url": "/atlas/skin-abc123.png", "dir": "atlas"},
                  "items": [
                    {"id": "steve", "name": "Steve", "en": "Steve", "es": "Steve"},
                    {"id": "alex", "name": "Alex", "slim": 1}
                  ]
                },
                {
                  "id": "hats",
                  "region": "head",
                  "label": "Chapeaux",
                  "name": "never read on a category",
                  "thumbCrop": "head",
                  "atlas": {"url": "/atlas/hats-def456.png"},
                  "items": [
                    {"id": "cap", "name": "Casquette", "en": "Cap"},
                    {"id": "crown", "name": "Couronne", "thumbCrop": "headtorso", "ajout": "2026-01-01"}
                  ]
                }
              ]
            }
            """;

    private static Catalog parse(String json) {
        try {
            return CatalogParser.parse(json);
        } catch (CatalogFormatException refused) {
            throw new AssertionError("the payload should have parsed", refused);
        }
    }

    private static CatalogCategory category(String json, String id) {
        return parse(json).category(id).orElseThrow();
    }

    @Nested
    class TheShapeOfIt {
        @Test
        void everyCategoryOfThePayloadIsRead() {
            Catalog catalog = parse(PAYLOAD);

            assertEquals(2, catalog.categories().size());
            assertFalse(catalog.isEmpty());
            assertEquals(List.of("body", "head"), catalog.regions());
        }

        @Test
        void fieldsTheModHasNoUseForAreIgnored() {
            assertEquals(2, category(PAYLOAD, "hats").items().size());
        }

        @Test
        void aCategoryCarriesItsRegionSingleFlagCropAndAtlas() {
            CatalogCategory skin = category(PAYLOAD, "skin");

            assertEquals("body", skin.region());
            assertTrue(skin.single());
            assertEquals(ThumbCrop.ALL, skin.thumbCrop());
            assertEquals("/atlas/skin-abc123.png", skin.atlasPath());
        }

        @Test
        void aCategoryWithoutASingleFlagStacks() {
            assertFalse(category(PAYLOAD, "hats").single());
        }

        @Test
        void anElementOverridesTheCropItsCategoryAsksFor() {
            CatalogCategory hats = category(PAYLOAD, "hats");

            assertEquals(ThumbCrop.HEAD, hats.thumbCrop(hats.items().get(0)));
            assertEquals(ThumbCrop.HEAD_TORSO, hats.thumbCrop(hats.items().get(1)));
        }

        @Test
        void categoriesAreListedInTheOrderTheCatalogueGivesThem() {
            assertEquals(List.of("skin", "hats"),
                    parse(PAYLOAD).categories().stream().map(CatalogCategory::id).toList());
            assertEquals(List.of("hats"),
                    parse(PAYLOAD).categoriesIn("head").stream().map(CatalogCategory::id).toList());
        }
    }

    @Nested
    class LabelAgainstName {
        @Test
        void aCategoryNamesItselfWithLabel() {
            CatalogText name = category(PAYLOAD, "hats").name();

            assertEquals("Chapeaux", name.forLanguage("fr_fr"));
            assertEquals("Chapeaux", name.base());
        }

        @Test
        void aCategorysTranslationsSitBesideItsLabel() {
            CatalogText name = category(PAYLOAD, "skin").name();

            assertEquals("Peau", name.forLanguage("fr_fr"));
            assertEquals("Skin", name.forLanguage("en_us"));
            assertEquals("Piel", name.forLanguage("es_es"));
        }

        @Test
        void anElementNamesItselfWithName() {
            CatalogItem cap = category(PAYLOAD, "hats").items().get(0);

            assertEquals("Casquette", cap.name().forLanguage("fr_fr"));
            assertEquals("Cap", cap.name().forLanguage("en_us"));
        }

        @Test
        void aCategoryWithNoLabelAtAllShowsNothingRatherThanBreaking() {
            String json = """
                    {"categories": [{"id": "hats", "items": [{"id": "cap", "name": "Casquette"}]}]}
                    """;

            assertSame(CatalogText.EMPTY, category(json, "hats").name());
        }
    }

    @Nested
    class AtlasRanks {
        /** cap, hat (slim), scarf, crown (slim), veil - ranks run ahead of the list. */
        private static final String RANKS = """
                {"categories": [{"id": "hats", "items": [
                  {"id": "cap"},
                  {"id": "hat", "slim": 1},
                  {"id": "scarf"},
                  {"id": "crown", "slim": true},
                  {"id": "veil"}
                ]}]}
                """;

        @Test
        void anElementWithASlimVariantTakesTwoConsecutiveBuffers() {
            List<CatalogItem> items = category(RANKS, "hats").items();

            assertEquals(0, items.get(0).atlasIndex());
            assertEquals(CatalogItem.NONE, items.get(0).slimAtlasIndex());

            assertEquals(1, items.get(1).atlasIndex());
            assertEquals(2, items.get(1).slimAtlasIndex());
        }

        @Test
        void everyElementAfterASlimOneRunsAheadOfItsRankInItems() {
            List<CatalogItem> items = category(RANKS, "hats").items();

            assertEquals(3, items.get(2).atlasIndex());
            assertEquals(4, items.get(3).atlasIndex());
            assertEquals(5, items.get(3).slimAtlasIndex());
            assertEquals(6, items.get(4).atlasIndex());
        }

        @Test
        void aSlimBufferIsReadOnlyForTheSlimModel() {
            List<CatalogItem> items = category(RANKS, "hats").items();

            assertFalse(items.get(0).hasSlim());
            assertEquals(0, items.get(0).atlasIndex(true));

            assertTrue(items.get(1).hasSlim());
            assertEquals(1, items.get(1).atlasIndex(false));
            assertEquals(2, items.get(1).atlasIndex(true));
        }

        @Test
        void theSlimFlagReadsAsANumberOrABoolean() {
            String json = """
                    {"categories": [{"id": "hats", "items": [
                      {"id": "zero", "slim": 0},
                      {"id": "off", "slim": false},
                      {"id": "on", "slim": 1}
                    ]}]}
                    """;
            List<CatalogItem> items = category(json, "hats").items();

            assertFalse(items.get(0).hasSlim());
            assertFalse(items.get(1).hasSlim());
            assertTrue(items.get(2).hasSlim());
        }

        @Test
        void anElementTheModDropsStillCostsItsBuffer() {
            // The buffers are in the atlas whether or not the mod can name the element,
            // so a rank skipped here would draw every later element with another's
            // pixels.
            String json = """
                    {"categories": [{"id": "hats", "items": [
                      {"id": "cap"},
                      {"name": "no id at all"},
                      {"id": "crown"}
                    ]}]}
                    """;
            List<CatalogItem> items = category(json, "hats").items();

            assertEquals(2, items.size());
            assertEquals(0, items.get(0).atlasIndex());
            assertEquals(2, items.get(1).atlasIndex());
        }
    }

    @Nested
    class WhatIsDropped {
        @Test
        void anItemLessCategoryIsNotWorthATab() {
            String json = """
                    {"categories": [
                      {"id": "empty", "region": "head", "label": "Vide", "items": []},
                      {"id": "hats", "region": "head", "label": "Chapeaux",
                       "items": [{"id": "cap", "name": "Casquette"}]}
                    ]}
                    """;
            Catalog catalog = parse(json);

            assertEquals(1, catalog.categories().size());
            assertTrue(catalog.category("empty").isEmpty());
        }

        @Test
        void aRegionWhoseCategoriesAllWentDisappearsWithThem() {
            String json = """
                    {"categories": [
                      {"id": "wings", "region": "back", "label": "Ailes", "items": []},
                      {"id": "hats", "region": "head", "label": "Chapeaux",
                       "items": [{"id": "cap", "name": "Casquette"}]}
                    ]}
                    """;

            assertEquals(List.of("head"), parse(json).regions());
        }

        @Test
        void aCategoryWithoutAnIdIsDropped() {
            String json = """
                    {"categories": [{"label": "Sans id", "items": [{"id": "cap"}]}]}
                    """;

            assertTrue(parse(json).isEmpty());
        }

        @Test
        void oneBadEntryDoesNotCostThePlayerTheWholeLibrary() {
            String json = """
                    {"categories": [
                      "not a category at all",
                      42,
                      {"id": "hats", "region": "head", "label": "Chapeaux",
                       "items": [{"id": "cap", "name": "Casquette"}, "junk"]}
                    ]}
                    """;
            Catalog catalog = parse(json);

            assertEquals(1, catalog.categories().size());
            assertEquals(1, catalog.category("hats").orElseThrow().items().size());
        }

        @Test
        void aCategoryWithoutAnItemsArrayIsDropped() {
            assertTrue(parse("{\"categories\": [{\"id\": \"hats\"}]}").isEmpty());
            assertTrue(parse("{\"categories\": [{\"id\": \"hats\", \"items\": {}}]}").isEmpty());
        }
    }

    @Nested
    class Defaults {
        @Test
        void aCategoryWithNoRegionBrowsesAsTheUnnamedOne() {
            String json = """
                    {"categories": [{"id": "hats", "label": "Chapeaux",
                     "items": [{"id": "cap", "name": "Casquette"}]}]}
                    """;

            assertEquals(CatalogParser.DEFAULT_REGION, category(json, "hats").region());
            assertEquals(List.of(CatalogParser.DEFAULT_REGION), parse(json).regions());
        }

        @Test
        void aCropTheModDoesNotKnowShowsTheWholeBody() {
            String json = """
                    {"categories": [{"id": "hats", "label": "Chapeaux", "thumbCrop": "elbow",
                     "items": [{"id": "cap", "name": "Casquette"}]}]}
                    """;

            assertEquals(ThumbCrop.ALL, category(json, "hats").thumbCrop());
        }

        @Test
        void anElementWithoutItsOwnCropDefersToItsCategory() {
            CatalogCategory hats = category(PAYLOAD, "hats");

            assertNull(hats.items().get(0).thumbCrop());
            assertEquals(hats.thumbCrop(), hats.thumbCrop(hats.items().get(0)));
        }

        @Test
        void aCategoryWithoutAnAtlasCarriesNoAddress() {
            String json = """
                    {"categories": [{"id": "hats", "label": "Chapeaux",
                     "items": [{"id": "cap", "name": "Casquette"}]}]}
                    """;

            assertEquals("", category(json, "hats").atlasPath());
        }

        @Test
        void aFieldOfTheWrongTypeIsTreatedAsAbsent() {
            String json = """
                    {"categories": [{"id": "hats", "label": 7, "region": 3, "single": "yes",
                     "atlas": "/not/an/object.png",
                     "items": [{"id": "cap", "name": "Casquette"}]}]}
                    """;
            CatalogCategory hats = category(json, "hats");

            assertSame(CatalogText.EMPTY, hats.name());
            assertEquals(CatalogParser.DEFAULT_REGION, hats.region());
            assertFalse(hats.single());
            assertEquals("", hats.atlasPath());
        }
    }

    @Nested
    class WhereTheElementsComeFrom {
        /** The table and the keys pointing at it, as the real catalogue publishes them. */
        private static final String CREDITED = """
                {
                  "credits": {
                    "aquarelliste": {
                      "titre": "Aquarelliste",
                      "auteur": "someone",
                      "licence": "cc-by-4",
                      "url": "https://example.invalid/aquarelliste"
                    },
                    "maison": {"titre": "MC Skin Creator", "auteur": "clixmods", "licence": "proprietaire"}
                  },
                  "categories": [
                    {
                      "id": "eyes",
                      "region": "head",
                      "label": "Yeux",
                      "items": [
                        {"id": "eye-classiques", "name": "Classiques", "credit": "maison"},
                        {"id": "eye-verts", "name": "Verts", "credit": "aquarelliste"},
                        {"id": "eye-orphelin", "name": "Orphelin"},
                        {"id": "eye-perdu", "name": "Perdu", "credit": "disparu"}
                      ]
                    }
                  ]
                }
                """;

        private CatalogItem item(String id) {
            return category(CREDITED, "eyes").items().stream()
                    .filter(candidate -> candidate.id().equals(id))
                    .findFirst()
                    .orElseThrow();
        }

        @Test
        void anElementPointsAtAWorkOfTheTable() {
            CatalogWork work = parse(CREDITED).workOf(item("eye-verts"));

            assertEquals("Aquarelliste", work.title());
            assertEquals("someone", work.author());
            assertEquals("cc-by-4", work.licence());
            assertTrue(work.hasUrl());
        }

        @Test
        void whatTheRepositoryDrewItselfIsAWorkLikeAnyOther() {
            assertEquals(CatalogWork.IN_HOUSE, item("eye-classiques").credit());
            assertEquals("MC Skin Creator", parse(CREDITED).workOf(item("eye-classiques")).title());
        }

        @Test
        void anElementNamingNoWorkHasNone() {
            assertTrue(parse(CREDITED).workOf(item("eye-orphelin")).isEmpty());
        }

        @Test
        void anElementNamingAWorkTheTableDoesNotHoldHasNoneEither() {
            assertEquals("disparu", item("eye-perdu").credit());
            assertTrue(parse(CREDITED).workOf(item("eye-perdu")).isEmpty(),
                    "a broken link reads as an undocumented provenance, not as a crash");
        }

        @Test
        void aCreditsFieldThatIsNotATableCostsTheCreditsAndNotTheCatalogue() {
            assertTrue(parse(PAYLOAD).works().isEmpty());
            assertEquals(2, parse(PAYLOAD).categories().size());
        }
    }

    @Nested
    class NotACatalogueAtAll {
        @Test
        void somethingThatIsNotAJsonObjectIsRefused() {
            assertThrows(CatalogFormatException.class, () -> CatalogParser.parse("[]"));
            assertThrows(CatalogFormatException.class, () -> CatalogParser.parse("\"hello\""));
            assertThrows(CatalogFormatException.class, () -> CatalogParser.parse("<html>nope</html>"));
        }

        @Test
        void anObjectWithoutACategoriesArrayIsRefused() {
            assertThrows(CatalogFormatException.class, () -> CatalogParser.parse("{}"));
            assertThrows(CatalogFormatException.class,
                    () -> CatalogParser.parse("{\"categories\": {}}"));
        }

        @Test
        void theRefusalSaysWhatWasWrongWithIt() {
            CatalogFormatException refused = assertThrows(CatalogFormatException.class,
                    () -> CatalogParser.parse("{}"));

            assertTrue(refused.getMessage().contains("categories"), refused.getMessage());
        }

        @Test
        void aCatalogueWithNoCategoriesIsEmptyRatherThanRefused() {
            assertTrue(parse("{\"categories\": []}").isEmpty());
            assertEquals(List.of(), parse("{\"categories\": []}").regions());
        }
    }

    /**
     * The ready-made stacks: the site's {@code projects}, which it calls models, and
     * its {@code outfits}. Their pieces name elements the way a serialised project
     * does — {@code cat} and {@code preset} — and not the way a catalogue entry does.
     */
    @Nested
    class ReadyMadeStacks {
        private static final String READY_MADE = """
                {
                  "categories": [
                    {
                      "id": "skin", "region": "body", "label": "Peau",
                      "atlas": {"url": "/atlas/skin.png"},
                      "items": [{"id": "steve", "name": "Steve"}]
                    }
                  ],
                  "projects": [
                    {
                      "id": "sorceress", "name": "Sorcière", "en": "Sorceress", "es": "Hechicera",
                      "credit": "some-work", "ajout": "2026-01-01", "base": "assets/",
                      "layers": [
                        {"cat": "skin", "preset": "skin-sorceress"},
                        {"cat": "top", "preset": "top-sorceress", "colors": {"cloth": "#4d1212"}}
                      ]
                    },
                    {"id": "fine", "name": "Fine", "slim": 1,
                     "layers": [{"cat": "skin", "preset": "skin-fine"}]},
                    {"id": "no-layers", "name": "Empty", "layers": []},
                    {"name": "No id", "layers": [{"cat": "skin", "preset": "skin-x"}]}
                  ],
                  "outfits": [
                    {
                      "id": "armour", "name": "Dame en armure", "en": "Lady in armour",
                      "layers": [
                        {"cat": "shoe", "preset": "shoe-armour"},
                        {"cat": "top", "preset": "top-armour"}
                      ]
                    }
                  ]
                }
                """;

        private static CatalogModel model(String id) {
            Catalog catalog = parse(READY_MADE);
            return Stream.concat(catalog.models().stream(), catalog.outfits().stream())
                    .filter(entry -> entry.id().equals(id))
                    .findFirst()
                    .orElseThrow();
        }

        @Test
        void projectsAreReadAsModelsAndOutfitsAsOutfits() {
            Catalog catalog = parse(READY_MADE);

            assertEquals(List.of("sorceress", "fine"),
                    catalog.models().stream().map(CatalogModel::id).toList());
            assertEquals(List.of("armour"),
                    catalog.outfits().stream().map(CatalogModel::id).toList());
            assertEquals(CatalogModel.Kind.MODEL, model("sorceress").kind());
            assertEquals(CatalogModel.Kind.OUTFIT, model("armour").kind());
        }

        @Test
        void aPieceNamesItsElementWithCatAndPreset() {
            assertEquals(List.of(
                            new CatalogModel.Piece("skin", "skin-sorceress"),
                            new CatalogModel.Piece("top", "top-sorceress",
                                    java.util.Map.of("cloth", 0x4D1212))),
                    model("sorceress").pieces());
        }

        @Test
        void aModelCarriesThePlayerModelItWasDrawnFor() {
            assertTrue(model("fine").slim());
            assertFalse(model("sorceress").slim());
        }

        @Test
        void anOutfitIsNeverSlim() {
            assertFalse(model("armour").slim(),
                    "an outfit is worn by whichever body is already there");
        }

        @Test
        void theThreeLabelsAreKept() {
            CatalogText name = model("sorceress").name();

            assertEquals("Sorcière", name.forLanguage("fr_fr"));
            assertEquals("Sorceress", name.forLanguage("en_us"));
            assertEquals("Hechicera", name.forLanguage("es_es"));
        }

        @Test
        void anEntryWithNoIdOrNoPieceIsDropped() {
            assertEquals(List.of("sorceress", "fine"),
                    parse(READY_MADE).models().stream().map(CatalogModel::id).toList());
        }

        @Test
        void theCategoriesAPieceNeedsAreOnlyTheOnesTheCatalogueStillHas() {
            Catalog catalog = parse(READY_MADE);

            assertEquals(List.of("skin"),
                    catalog.categoriesOf(model("sorceress")).stream()
                            .map(CatalogCategory::id).toList());
        }

        @Test
        void aCatalogueCarryingNeitherListIsStillACatalogue() {
            Catalog catalog = parse(PAYLOAD);

            assertEquals(List.of(), catalog.models());
            assertEquals(List.of(), catalog.outfits());
            assertFalse(catalog.isEmpty());
        }

        @Test
        void aListThatIsNotAListIsIgnoredRatherThanRefused() {
            Catalog catalog = parse("{\"categories\": [], \"projects\": 7, \"outfits\": \"no\"}");

            assertEquals(List.of(), catalog.models());
            assertEquals(List.of(), catalog.outfits());
        }
    }

    @Nested
    class Colours {
        private static final String COLOURED = """
                {
                  "categories": [
                    {"id": "hat", "region": "head", "items": [
                      {"id": "hat-green", "colors": {"main": "#359e61", "band": "#FFFFFF",
                                                     "bad": "green", "worse": 12}},
                      {"id": "hat-plain"}
                    ]}
                  ],
                  "projects": [
                    {"id": "ranger", "layers": [
                      {"cat": "hat", "preset": "hat-green", "colors": {"main": "#ff0000"}}
                    ]}
                  ]
                }
                """;

        @Test
        void anElementCarriesItsKeysInOrderAndOnlyTheReadableOnes() {
            CatalogItem green = category(COLOURED, "hat").items().get(0);

            assertEquals(List.of("main", "band"), List.copyOf(green.colors().keySet()));
            assertEquals(0x359E61, green.colors().get("main"));
            assertEquals(0xFFFFFF, green.colors().get("band"));
        }

        @Test
        void anElementWithoutKeysHasNone() {
            assertTrue(category(COLOURED, "hat").items().get(1).colors().isEmpty());
        }

        @Test
        void aPieceCarriesItsOverride() {
            CatalogModel ranger = parse(COLOURED).models().get(0);

            assertEquals(java.util.Map.of("main", 0xFF0000), ranger.pieces().get(0).colors());
        }
    }
}
