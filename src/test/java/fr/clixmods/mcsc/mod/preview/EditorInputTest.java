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

import org.junit.jupiter.api.Test;

import fr.clixmods.mcsc.mod.ui.EditorChrome;
import fr.clixmods.mcsc.mod.ui.panel.Panel;

/**
 * Where the pointer's input actually goes, which a picture cannot show.
 *
 * <p>The preview draws the editor; this asks the laid-out editor the questions the
 * screen asks it when something is clicked or scrolled. Both columns are on screen at
 * once and both answer the wheel, so which one answers is not a detail — it is the
 * difference between scrolling the list under the pointer and scrolling the other one.
 */
class EditorInputTest {
    private static final int WIDTH = 960;
    private static final int HEIGHT = 540;

    private static EditorChrome laidOut() {
        return laidOut(HEIGHT);
    }

    private static EditorChrome laidOut(int height) {
        EditorPreview preview = new EditorPreview()
                .withCatalog(EditorPreview.sampleCatalog())
                .withLayers(5);
        preview.render(WIDTH, height);
        return preview.chrome();
    }

    /**
     * The wheel goes to the column the pointer is over.
     *
     * <p>Both columns used to test the pointer's height and nothing else, and the screen
     * offers the wheel to the library first — so with the pointer over the layers, the
     * library took it and scrolled itself. The layer list simply would not scroll to the
     * wheel, though dragging its handle worked, which is what made it look like a focus
     * problem rather than a hit test with a dimension missing.
     */
    @Test
    void theLibraryDoesNotTakeAWheelOverTheLayers() {
        EditorChrome chrome = laidOut();
        Panel library = chrome.library();
        Panel layers = chrome.layers();
        int middle = HEIGHT / 2;

        assertTrue(layers.x() > library.x() + library.width(),
                "this size should put the two columns side by side");
        assertFalse(library.scroll(layers.x() + 2, middle, -1.0),
                "the library answered a wheel over the layers column");
        assertFalse(library.scroll(layers.x() + layers.width() - 2, middle, -1.0),
                "the library answered a wheel over the far edge of the layers column");
    }

    /** And the other way about, so the fix is a hit test and not an ordering accident. */
    @Test
    void theLayersDoNotTakeAWheelOverTheLibrary() {
        EditorChrome chrome = laidOut();
        Panel library = chrome.library();
        int middle = HEIGHT / 2;

        assertFalse(chrome.layers().scroll(library.x() + 2, middle, -1.0),
                "the layers answered a wheel over the library column");
    }

    /**
     * The library still scrolls when the pointer really is on it.
     *
     * <p>Short on purpose: at the full height the sample catalogue fits in the column
     * and there is nothing to scroll to, which is a true answer to a different question.
     */
    @Test
    void theLibraryTakesAWheelOverItself() {
        EditorChrome chrome = laidOut(360);
        Panel library = chrome.library();
        assertTrue(library.scroll(library.x() + library.width() / 2, 180, -1.0),
                "the library should scroll to a wheel inside its own column");
    }

    /** The scene is between them, and neither column reaches into it. */
    @Test
    void neitherColumnTakesAWheelOverTheScene() {
        EditorChrome chrome = laidOut();
        int middleOfScene = chrome.scene().x() + chrome.scene().width() / 2;
        assertFalse(chrome.library().scroll(middleOfScene, HEIGHT / 2, -1.0));
        assertFalse(chrome.layers().scroll(middleOfScene, HEIGHT / 2, -1.0));
    }
}
