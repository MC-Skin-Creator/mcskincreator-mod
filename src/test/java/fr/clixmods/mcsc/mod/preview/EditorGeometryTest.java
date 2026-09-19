/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.preview;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import fr.clixmods.mcsc.mod.ui.EditorChrome;
import fr.clixmods.mcsc.mod.ui.Element;
import fr.clixmods.mcsc.mod.ui.panel.Panel;

/**
 * Where everything landed, checked rather than looked at.
 *
 * <p>The preview says what the screen looks like at five sizes. This says that it
 * holds together at every size, which is a different question and not one anybody is
 * going to answer by eye: the arrangement changes at three thresholds and every panel
 * measures itself against the width it was given.
 *
 * <p>The rules are the ones that were actually broken. A control drawn outside the
 * screen is a control nobody can press. A control drawn outside its own panel is drawn
 * over the panel beside it. A control of no width is one that was laid out against a
 * number that came out negative, which is how a column too narrow for its contents
 * fails — quietly, with everything piled at one pixel.
 */
class EditorGeometryTest {
    @Test
    void nothingLandsOutsideTheScreen() {
        forEverySize((chrome, width, height) -> {
            for (Element element : chrome.targets()) {
                if (!element.visible()) {
                    continue;
                }
                int[] box = element.hitBox();
                if (box[2] == 0 || box[3] == 0) {
                    // Clipped away entirely, which is how a band too short for its
                    // contents gives up: they are out of reach, not off screen.
                    continue;
                }
                assertTrue(box[0] >= 0 && box[1] >= 0
                                && box[0] + box[2] <= width && box[1] + box[3] <= height,
                        () -> describe(element) + " is outside a " + width + "x" + height + " screen");
            }
        });
    }

    @Test
    void nothingLandsOutsideItsOwnPanel() {
        forEverySize((chrome, width, height) -> {
            for (Panel panel : List.of(chrome.library(), chrome.layers())) {
                if (!panel.visible()) {
                    continue;
                }
                for (Element child : panel.children()) {
                    if (!child.visible() || child == panel) {
                        continue;
                    }
                    int[] box = child.hitBox();
                    assertTrue(box[2] == 0 || box[3] == 0 || (box[0] >= panel.x()
                                    && box[0] + box[2] <= panel.x() + panel.width()),
                            () -> describe(child) + " runs past its panel at "
                                    + width + "x" + height);
                }
            }
        });
    }

    @Test
    void nothingIsLaidOutWithNoRoomAtAll() {
        forEverySize((chrome, width, height) -> {
            for (Element element : chrome.targets()) {
                if (!element.visible()) {
                    continue;
                }
                assertFalse(element.width() <= 0 || element.height() <= 0,
                        () -> describe(element) + " has no size at " + width + "x" + height);
            }
        });
    }

    /** The scene never disappears, whatever the width does. */
    @Test
    void theSceneIsAlwaysOnScreen() {
        forEverySize((chrome, width, height) -> assertTrue(
                chrome.scene().width() > 0 && chrome.scene().height() > 0,
                () -> "the scene vanished at " + width + "x" + height));
    }

    private interface Check {
        void run(EditorChrome chrome, int width, int height);
    }

    /**
     * Every size worth trying: the game's own, and a sweep across the thresholds.
     *
     * <p>The sweep is what finds the trouble. The three-column arrangement gives way to
     * drawers at a width that depends on the column width, which itself depends on the
     * width — so the interesting sizes are not round numbers and nobody would have
     * picked them by hand.
     */
    private void forEverySize(Check check) {
        for (int width = 200; width <= 1500; width += 17) {
            for (int height : new int[] {240, 360, 540, 720, 900}) {
                EditorPreview preview = new EditorPreview()
                        .withCatalog(EditorPreview.sampleCatalog())
                        .withLayers(5);
                preview.render(width, height);
                check.run(preview.chrome(), width, height);
            }
        }
    }

    private static String describe(Element element) {
        int[] box = element.hitBox();
        return element.getClass().getSimpleName()
                + "[" + box[0] + "," + box[1] + " " + box[2] + "x" + box[3] + "]";
    }
}
