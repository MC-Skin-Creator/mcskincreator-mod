/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.scene;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Three questions the three cameras answer differently, and getting any of them wrong
 * is a camera that shows nothing.
 *
 * <p>The middle one is the one worth pinning down, and it has already been got wrong
 * twice in opposite directions. The level renderer draws the local player only while
 * they are the camera entity ({@code LevelRenderer.renderLevel}), so the in-game view
 * must take the game's camera — and must leave it pointed at the player. Flying a
 * separate camera around them empties the world; not taking the camera at all leaves
 * the game in whatever view it was in.
 */
class CameraModeTest {
    @Test
    void onlyTheWorkshopWorksWithoutAWorld() {
        assertFalse(CameraMode.WORKSHOP.needsWorld());
        assertTrue(CameraMode.IN_GAME.needsWorld());
        assertTrue(CameraMode.FIRST_PERSON.needsWorld());
    }

    @Test
    void bothWorldCamerasBorrowTheGamesCamera() {
        assertFalse(CameraMode.WORKSHOP.takesGameCamera());
        assertTrue(CameraMode.IN_GAME.takesGameCamera(),
                "only the game draws your own character, and only through its own camera");
        assertTrue(CameraMode.FIRST_PERSON.takesGameCamera());
    }

    @Test
    void onlyTheGameBackdropNeedsAWorld() {
        assertFalse(SceneBackdrop.PANEL.needsWorld());
        assertTrue(SceneBackdrop.WORLD.needsWorld());
        for (SceneBackdrop backdrop : SceneBackdrop.values()) {
            assertTrue(backdrop.labelKey().startsWith("backdrop.mcskincreator."),
                    backdrop.name());
            assertTrue(backdrop.tooltipKey().endsWith(".tooltip"), backdrop.name());
        }
    }

    @Test
    void bothWorldCamerasLayerOverTheGame() {
        assertFalse(CameraMode.WORKSHOP.overlaysGame());
        assertTrue(CameraMode.IN_GAME.overlaysGame());
        assertTrue(CameraMode.FIRST_PERSON.overlaysGame());
    }

    @Test
    void everyCameraIsNamedAndExplained() {
        for (CameraMode mode : CameraMode.values()) {
            assertTrue(mode.labelKey().startsWith("camera.mcskincreator."), mode.name());
            assertTrue(mode.tooltipKey().endsWith(".tooltip"), mode.name());
        }
    }
}
