/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.scene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The camera's bounds, which are the whole reason it is a class of its own.
 *
 * <p>Every one of these is a way to lose the figure off the side of the panel with no
 * way back but the button, and none of them can be seen in a screenshot — a camera that
 * has drifted a thousand pixels looks exactly like an empty scene.
 */
class SceneCameraTest {
    @Test
    void tiltingStopsBeforeTheFigureTurnsOver() {
        SceneCamera camera = new SceneCamera();
        for (int drag = 0; drag < 200; drag++) {
            camera.turn(0, 100);
        }
        assertEquals(SceneCamera.PITCH_LIMIT, camera.pitch(), 1.0E-6F);

        for (int drag = 0; drag < 400; drag++) {
            camera.turn(0, -100);
        }
        assertEquals(-SceneCamera.PITCH_LIMIT, camera.pitch(), 1.0E-6F);
    }

    @Test
    void turningNeverLeavesOneRevolution() {
        SceneCamera camera = new SceneCamera();
        for (int drag = 0; drag < 1000; drag++) {
            camera.turn(500, 0);
        }
        assertTrue(camera.yaw() >= 0 && camera.yaw() < Math.PI * 2,
                "the turn is wrapped, or a float loses whole degrees of it: " + camera.yaw());
    }

    @Test
    void zoomingStaysBetweenItsBounds() {
        SceneCamera camera = new SceneCamera();
        for (int notch = 0; notch < 100; notch++) {
            camera.zoom(1);
        }
        assertEquals(SceneCamera.ZOOM_MAX, camera.zoom(), 1.0E-6F);

        for (int notch = 0; notch < 200; notch++) {
            camera.zoom(-1);
        }
        assertEquals(SceneCamera.ZOOM_MIN, camera.zoom(), 1.0E-6F);
    }

    @Test
    void aStillWheelDoesNothing() {
        SceneCamera camera = new SceneCamera();
        camera.zoom(0);
        assertTrue(camera.isCentred());
    }

    @Test
    void panningFollowsThePointerAndThenStops() {
        SceneCamera camera = new SceneCamera();
        camera.pan(30, -12);
        assertEquals(30, camera.panX(), 1.0E-6F);
        assertEquals(-12, camera.panY(), 1.0E-6F);

        for (int drag = 0; drag < 100; drag++) {
            camera.pan(100, 100);
        }
        assertEquals(SceneCamera.PAN_LIMIT, camera.panX(), 1.0E-6F);
        assertEquals(SceneCamera.PAN_LIMIT, camera.panY(), 1.0E-6F);
    }

    @Test
    void recentringPutsEverythingBack() {
        SceneCamera camera = new SceneCamera();
        camera.turn(80, 40);
        camera.pan(25, 25);
        camera.zoom(1);
        assertFalse(camera.isCentred());

        camera.recentre();
        assertTrue(camera.isCentred());
        assertEquals(SceneCamera.ZOOM_DEFAULT, camera.zoom(), 1.0E-6F);
        assertEquals(0, camera.panX(), 1.0E-6F);
        assertEquals(0, camera.panY(), 1.0E-6F);
    }
}
