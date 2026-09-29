/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.preview;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import fr.clixmods.mcsc.mod.ui.EditorScale;

/**
 * Renders the editor and writes the pictures out.
 *
 * <p>This is not a screenshot test: nothing here compares against a stored image, and
 * a change of layout breaks no assertion. It exists so that changing this interface
 * stops being blind work — {@code build/ui-preview/} holds what the screen looks like
 * at the sizes the game actually gives it, and looking is a great deal faster than
 * launching a client.
 *
 * <p>What it does assert is the one thing a picture cannot say for itself: that the
 * font and the sprites were really found. A preview drawn without them would be a
 * plausible-looking lie.
 */
class EditorPreviewTest {
    private static final Path OUTPUT = Paths.get("build", "ui-preview");

    /**
     * The windows this screen is looked at in, and what the game says each one holds.
     *
     * <p>These are screen sizes, not editor sizes. {@link EditorScale#scaleFor} turns
     * each into the scale the editor takes and the rows it gets, exactly as it does in
     * the game, so the pictures move when that rule moves and there is no second table
     * to keep in step with the first.
     *
     * <p>Each picture is written at the window's own size, magnified by the scale that
     * window uses. What comes out is therefore what a photograph of that screen would
     * show, at the size it would show it — which is the whole point, because the fault
     * these are for catching is an editor drawn too small, and an editor drawn too small
     * looks perfectly correct when you magnify the picture of it.
     */
    private static final int[][] WINDOWS = {
        {1280, 720, 3}, {1366, 768, 3}, {1600, 900, 3},
        {1920, 1080, 4}, {2560, 1440, 6}, {3840, 2160, 9},
    };

    @Test
    void theEditorRendersAtEverySizeTheGameGivesIt() throws IOException {
        Files.createDirectories(OUTPUT);
        for (int[] window : WINDOWS) {
            int scale = EditorScale.scaleFor(window[1], window[2]);
            int width = window[0] / scale;
            int height = window[1] / scale;
            String name = window[0] + "x" + window[1];
            BufferedImage image = EditorPreview.of(width, height, scale);
            assertNotNull(image);
            ImageIO.write(image, "png", OUTPUT.resolve(name + ".png").toFile());
            ImageIO.write(EditorPreview.of("en_us", width, height, scale), "png",
                    OUTPUT.resolve(name + "-en.png").toFile());
        }
    }

    /**
     * The layers column on its own, blown up, because that is where the small controls
     * are and a whole screen at screen size cannot show whether one of them works.
     *
     * <p>A frame, a bevel and a glyph are three or four pixels each. Looked at in a
     * 1920 by 1080 picture they are a smudge; looked at here they are what a player
     * leaning towards their monitor sees, which is the only way to tell a button with
     * a border from a hole with something at the bottom of it.
     */
    @Test
    void theSmallControlsAreWrittenOutBlownUp() throws IOException {
        Files.createDirectories(OUTPUT);
        EditorPreview preview = new EditorPreview()
                .withCatalog(EditorPreview.sampleCatalog())
                .withLayers(5);
        int scale = EditorScale.scaleFor(1080, 4);
        BufferedImage screen = preview.render(1920 / scale, 1080 / scale, scale);
        int layers = preview.chrome().layers().x() * scale;
        int library = preview.chrome().library().width() * scale;
        detail(preview, screen, "layers", layers, 0, 1920 - layers, 700);
        detail(preview, screen, "library", 0, 0, library, 700);
        detail(preview, screen, "settings", layers, 1080 - 320, 1920 - layers, 320);
        detail(preview, screen, "topbar-left", 0, 0, 640, 80);
        detail(preview, screen, "topbar-right", 620, 0, 640, 80);
        detail(preview, screen, "scene-foot", library, 1080 - 120, 900, 120);
        ImageIO.write(preview.magnify(preview.crop(screen, layers, 130, 1920 - layers, 60), 6),
                "png", OUTPUT.resolve("detail-layers-head.png").toFile());
    }

    private static void detail(EditorPreview preview, BufferedImage screen, String name,
                               int x, int y, int width, int height) throws IOException {
        ImageIO.write(preview.magnify(preview.crop(screen, x, y, width, height), 3), "png",
                OUTPUT.resolve("detail-" + name + ".png").toFile());
    }

    /** The design size on its own, drawn four times up, to be looked at closely. */
    @Test
    void theDesignSizeIsWrittenOutMagnifiedToBeLookedAt() throws IOException {
        Files.createDirectories(OUTPUT);
        ImageIO.write(EditorPreview.of(960, 540, 4), "png",
                OUTPUT.resolve("960x540@4.png").toFile());
    }

    @Test
    void anEmptyEditorRenders() throws IOException {
        Files.createDirectories(OUTPUT);
        BufferedImage image = new EditorPreview()
                .withCatalog(EditorPreview.sampleCatalog())
                .render(960, 540, 2);
        ImageIO.write(image, "png", OUTPUT.resolve("960x540-empty.png").toFile());
    }

    /** The game's font is on the classpath, and its advances are the game's own. */
    @Test
    void theFontIsTheGames() {
        PixelFont font = PixelFont.vanilla();
        assertTrue(font.advance('A') == 6, "an A is six pixels wide in vanilla");
        assertTrue(font.advance('i') == 2, "an i is two");
        assertTrue(font.advance(' ') == 4, "a space is four");
        assertTrue(font.width("Export") > 0);
    }

    /** The game's sprites are there too — a preview without them shows nothing at all. */
    @Test
    void theSpritesAreTheGames() {
        assertNotNull(GameAssets.image("assets/minecraft/textures/gui/sprites/widget/button.png"),
                "the button sprite should come out of the Minecraft jar on the test classpath");
        assertNotNull(GameAssets.json(
                "assets/minecraft/textures/gui/sprites/popup/background.png.mcmeta"),
                "and its nine-slice metadata with it");
    }
}
