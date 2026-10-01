/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.project;

import static fr.clixmods.mcsc.mod.Fixtures.category;
import static fr.clixmods.mcsc.mod.Fixtures.single;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import fr.clixmods.mcsc.mod.catalog.CatalogCategory;
import fr.clixmods.mcsc.mod.skin.SkinModel;

/** The rules the layer stack follows, none of which need a game to check. */
class SkinProjectTest {
    private final SkinProject project = new SkinProject();

    /** The ids of the stack, bottom to top - the order the sheet is composed in. */
    private List<String> stack() {
        List<String> ids = new ArrayList<>();
        for (Layer layer : this.project.layers()) {
            ids.add(layer.itemId());
        }
        return ids;
    }

    private Layer add(CatalogCategory category, String itemId) {
        return this.project.add(category, category.items().stream()
                .filter(item -> item.id().equals(itemId))
                .findFirst()
                .orElseThrow(), "en_us");
    }

    @Nested
    class Stacking {
        @Test
        void newLayersGoOnTop() {
            CatalogCategory hats = category("hats", "head", "cap", "crown");

            add(hats, "cap");
            add(hats, "crown");

            assertEquals(List.of("cap", "crown"), stack());
        }

        @Test
        void theLayerAddedIsTheOneSelected() {
            CatalogCategory hats = category("hats", "head", "cap");

            Layer layer = add(hats, "cap");

            assertSame(layer, project.selected());
        }

        @Test
        void theStackIsNotHandedOutForEditing() {
            CatalogCategory hats = category("hats", "head", "cap");
            add(hats, "cap");

            assertThrows(UnsupportedOperationException.class, () -> project.layers().clear());
        }

        @Test
        void aFullStackRefusesTheNextLayer() {
            CatalogCategory hats = category("hats", "head", "cap");
            for (int i = 0; i < SkinProject.MAX_LAYERS; i++) {
                assertNotNull(add(hats, "cap"), "layer " + i + " should have been stacked");
            }

            assertNull(add(hats, "cap"));
            assertEquals(SkinProject.MAX_LAYERS, project.layers().size());
        }

        @Test
        void removingClearsTheSelectionItHeld() {
            CatalogCategory hats = category("hats", "head", "cap");
            Layer layer = add(hats, "cap");

            project.remove(layer);

            assertTrue(project.isEmpty());
            assertNull(project.selected());
        }

        @Test
        void aDuplicateSitsStraightAboveItsOriginal() {
            CatalogCategory hats = category("hats", "head", "cap", "crown");
            Layer cap = add(hats, "cap");
            add(hats, "crown");

            Layer copy = project.duplicate(cap);

            assertEquals(List.of("cap", "cap", "crown"), stack());
            assertSame(copy, project.layers().get(1));
            assertNotSame(cap, copy);
            assertSame(copy, project.selected());
        }

        @Test
        void aLayerOutsideTheStackIsNotDuplicated() {
            CatalogCategory hats = category("hats", "head", "cap");
            Layer detached = add(hats, "cap").copy();

            assertNull(project.duplicate(detached));
        }
    }

    @Nested
    class SingleCategories {
        @Test
        void chosingAgainReplacesRatherThanStacks() {
            CatalogCategory skins = single("skin", "body", "steve", "alex");

            add(skins, "steve");
            add(skins, "alex");

            assertEquals(List.of("alex"), stack());
        }

        @Test
        void theReplacementStaysAtTheBottomOfTheStack() {
            CatalogCategory skins = single("skin", "body", "steve", "alex");
            CatalogCategory hats = category("hats", "head", "cap");

            add(skins, "steve");
            add(hats, "cap");
            add(skins, "alex");

            assertEquals(List.of("alex", "cap"), stack());
        }

        @Test
        void onlyItsOwnCategoryIsReplaced() {
            CatalogCategory skins = single("skin", "body", "steve");
            CatalogCategory eyes = single("eyes", "head", "blue");

            add(skins, "steve");
            add(eyes, "blue");

            assertEquals(List.of("blue", "steve"), stack());
        }
    }

    @Nested
    class Moving {
        private Layer lower;
        private Layer between;
        private Layer upper;

        /** head, torso, head: a layer of another region sits between the two hats. */
        private void buildInterleavedStack() {
            CatalogCategory hats = category("hats", "head", "cap", "crown");
            CatalogCategory shirts = category("shirts", "torso", "tee");

            lower = add(hats, "cap");
            between = add(shirts, "tee");
            upper = add(hats, "crown");
        }

        @Test
        void aLayerSkipsPastAnotherRegionToReachItsNeighbour() {
            buildInterleavedStack();

            assertTrue(project.move(lower, 1));

            assertEquals(List.of("crown", "tee", "cap"), stack());
            assertSame(between, project.layers().get(1));
        }

        @Test
        void movingDownIsTheSameSwapBackwards() {
            buildInterleavedStack();

            assertTrue(project.move(upper, -1));

            assertEquals(List.of("crown", "tee", "cap"), stack());
        }

        @Test
        void theTopmostOfItsRegionHasNowhereToGo() {
            buildInterleavedStack();

            assertFalse(project.move(upper, 1));
            assertEquals(List.of("cap", "tee", "crown"), stack());
        }

        @Test
        void theOnlyLayerOfItsRegionNeverMoves() {
            buildInterleavedStack();

            assertFalse(project.move(between, 1));
            assertFalse(project.move(between, -1));
            assertEquals(List.of("cap", "tee", "crown"), stack());
        }

        @Test
        void aLayerOutsideTheStackDoesNotMove() {
            buildInterleavedStack();

            assertFalse(project.move(lower.copy(), 1));
        }
    }

    @Nested
    class Ordering {
        @Test
        void aRegionIsListedTopmostFirst() {
            CatalogCategory hats = category("hats", "head", "cap", "crown");
            add(hats, "cap");
            add(hats, "crown");

            List<Layer> shown = project.displayOrder("head");

            assertEquals("crown", shown.get(0).itemId());
            assertEquals("cap", shown.get(1).itemId());
        }

        @Test
        void aRegionListsOnlyItsOwnLayers() {
            add(category("hats", "head", "cap"), "cap");
            add(category("shirts", "torso", "tee"), "tee");

            assertEquals(1, project.displayOrder("head").size());
            assertTrue(project.displayOrder("legs").isEmpty());
        }

        @Test
        void regionsInUseFollowTheCatalogueOrder() {
            add(category("shirts", "torso", "tee"), "tee");
            add(category("hats", "head", "cap"), "cap");

            assertEquals(List.of("head", "torso"),
                    project.regionsInUse(List.of("head", "torso", "legs")));
        }

        @Test
        void aRegionTheCatalogueDroppedIsListedAfterTheKnownOnes() {
            add(category("hats", "head", "cap"), "cap");
            add(category("wings", "back", "angel"), "angel");

            assertEquals(List.of("head", "back"), project.regionsInUse(List.of("head", "torso")));
        }

        @Test
        void thePreviewFallsBackToTheTopmostVisibleLayer() {
            CatalogCategory hats = category("hats", "head", "cap", "crown");
            Layer cap = add(hats, "cap");
            Layer crown = add(hats, "crown");

            assertSame(crown, project.topVisible());

            crown.setVisible(false);
            assertSame(cap, project.topVisible());

            cap.setVisible(false);
            assertNull(project.topVisible());
        }
    }

    @Nested
    class Revision {
        @Test
        void everyMutationMovesIt() {
            CatalogCategory hats = category("hats", "head", "cap", "crown");

            assertBumps(() -> add(hats, "cap"), "add");
            Layer cap = project.layers().get(0);
            assertBumps(() -> project.duplicate(cap), "duplicate");
            assertBumps(() -> add(hats, "crown"), "add another region-mate");
            assertBumps(() -> project.move(cap, 1), "move");
            assertBumps(() -> project.remove(cap), "remove");
            assertBumps(() -> project.setModel(SkinModel.SLIM), "setModel");
            assertBumps(project::touch, "touch");
            assertBumps(() -> project.restore(project.snapshot()), "restore");
            assertBumps(project::clear, "clear");
        }

        @Test
        void aChangeThatChangedNothingLeavesItAlone() {
            CatalogCategory hats = category("hats", "head", "cap");
            Layer detached = add(hats, "cap").copy();
            int before = project.revision();

            project.remove(detached);
            project.move(detached, 1);
            project.select(project.layers().get(0));

            assertEquals(before, project.revision());
        }

        private void assertBumps(Runnable mutation, String name) {
            int before = project.revision();
            mutation.run();
            assertTrue(project.revision() > before, name + " should have bumped the revision");
        }
    }

    @Nested
    class Model {
        @Test
        void theModelIsWideUntilItIsChanged() {
            assertEquals(SkinModel.WIDE, project.model());
            assertFalse(project.isSlim());

            project.setModel(SkinModel.SLIM);

            assertEquals(SkinModel.SLIM, project.model());
            assertTrue(project.isSlim());
        }
    }

    @Nested
    class Snapshots {
        @Test
        void aSnapshotDetachesFromTheLayersItCopied() {
            CatalogCategory hats = category("hats", "head", "cap");
            Layer cap = add(hats, "cap");
            SkinProject.Snapshot snapshot = project.snapshot();

            cap.setOpacity(10);
            project.restore(snapshot);

            assertEquals(100, project.layers().get(0).opacity());
            assertNotSame(cap, project.layers().get(0));
        }

        @Test
        void restoringPutsTheSelectionBackOnTheSameRank() {
            CatalogCategory hats = category("hats", "head", "cap", "crown");
            add(hats, "cap");
            add(hats, "crown");
            SkinProject.Snapshot snapshot = project.snapshot();

            project.clear();
            project.restore(snapshot);

            assertEquals("crown", project.selected().itemId());
            assertSame(project.layers().get(1), project.selected());
        }

        @Test
        void restoringASelectionLessSnapshotSelectsNothing() {
            CatalogCategory hats = category("hats", "head", "cap");
            add(hats, "cap");
            project.select(null);

            project.restore(project.snapshot());

            assertNull(project.selected());
        }

        @Test
        void theModelTravelsWithTheSnapshot() {
            SkinProject.Snapshot wide = project.snapshot();
            project.setModel(SkinModel.SLIM);

            project.restore(wide);

            assertEquals(SkinModel.WIDE, project.model());
        }
    }

    @Nested
    class StartingFresh {
        @Test
        void aNewProjectWearsThePlainSkinOnTheWideModel() {
            CatalogCategory skins = single("skin", "base", "skin-rose", SkinProject.DEFAULT_SKIN);
            CatalogCategory hats = category("hats", "head", "cap");
            add(hats, "cap");
            project.setModel(SkinModel.SLIM);

            project.startFresh(fr.clixmods.mcsc.mod.Fixtures.catalog(hats, skins), "en_us");

            assertEquals(List.of(SkinProject.DEFAULT_SKIN), stack());
            assertEquals(SkinModel.WIDE, project.model());
            assertNull(project.selected());
        }

        @Test
        void aCatalogueWithoutThePlainSkinFallsBackToItsFirstSkin() {
            CatalogCategory skins = single("skin", "base", "skin-rose", "skin-ash");

            project.startFresh(fr.clixmods.mcsc.mod.Fixtures.catalog(skins), "en_us");

            assertEquals(List.of("skin-rose"), stack());
        }
    }
}
