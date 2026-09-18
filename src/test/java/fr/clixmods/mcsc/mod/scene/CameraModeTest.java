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
 * <p>The middle one is the one worth pinning down. The in-game view must <strong>not</strong>
 * take the game's camera: the level renderer draws the local player only while they are
 * the camera entity, so moving the camera off them empties the world. That was the first
 * version of this feature, and it cost a round of testing to find — the rule is in
 * `LevelRenderer`, not in anything the mod can see from here.
 */
class CameraModeTest {
    @Test
    void onlyTheWorkshopWorksWithoutAWorld() {
        assertFalse(CameraMode.WORKSHOP.needsWorld());
        assertTrue(CameraMode.IN_GAME.needsWorld());
        assertTrue(CameraMode.FIRST_PERSON.needsWorld());
    }

    @Test
    void onlyFirstPersonBorrowsTheGamesCamera() {
        assertFalse(CameraMode.WORKSHOP.takesGameCamera());
        assertFalse(CameraMode.IN_GAME.takesGameCamera(),
                "moving the camera off the player stops the player being drawn at all");
        assertTrue(CameraMode.FIRST_PERSON.takesGameCamera());
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
