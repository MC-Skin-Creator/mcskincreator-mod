/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui.panel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Where the outfits' tab sits among the regions.
 *
 * <p>The catalogue does not carry the region: outfits are a list beside the
 * categories, so the tab is added to the ones it does carry, and where it lands is
 * arithmetic worth pinning. Nothing here draws.
 */
class LibraryRegionsTest {
    private static final List<String> CATALOGUE = List.of("base", "head", "torso", "arms", "legs");

    @Test
    void theOutfitsSitStraightAfterTheBody() {
        assertEquals(List.of("base", "outfit", "head", "torso", "arms", "legs"),
                LibraryPanel.regionsWith(CATALOGUE, true));
    }

    @Test
    void aCatalogueWithNoOutfitGetsNoTab() {
        assertEquals(CATALOGUE, LibraryPanel.regionsWith(CATALOGUE, false));
        assertFalse(LibraryPanel.regionsWith(CATALOGUE, false).contains(LibraryPanel.OUTFIT_REGION));
    }

    @Test
    void withNoBodyRegionToFollowTheyComeFirst() {
        assertEquals(List.of("outfit", "head", "legs"),
                LibraryPanel.regionsWith(List.of("head", "legs"), true));
    }

    @Test
    void aCatalogueWithNoRegionAtAllStillOffersTheOutfits() {
        assertEquals(List.of("outfit"), LibraryPanel.regionsWith(List.of(), true));
        assertEquals(List.of(), LibraryPanel.regionsWith(List.of(), false));
    }

    @Test
    void theSheetIsNotKeyedOnTheRegionName() {
        // A catalogue is free to name a real category "outfit"; two sheets under one
        // key would draw each other's pixels.
        assertFalse(LibraryPanel.OUTFIT_SHEET.equals(LibraryPanel.OUTFIT_REGION));
    }
}
