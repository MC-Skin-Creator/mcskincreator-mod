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
     * The sizes the game gives this screen.
     *
     * <p>A 1920x1080 window is 640x360 at a GUI scale of 3 and 480x270 at 4, which is
     * what most people play at; 854x480 is scale 2 on the same window and the widest
     * this screen realistically sees. The two small ones are where the columns give up
     * and become drawers. Each is written out magnified by its own scale, so the
     * picture is the size the player's screen shows it at.
     */
    private static final int[][] SIZES = {
        {854, 480, 2}, {640, 360, 3}, {480, 270, 4}, {427, 240, 3}, {320, 240, 3},
    };

    @Test
    void theEditorRendersAtEverySizeTheGameGivesIt() throws IOException {
        Files.createDirectories(OUTPUT);
        for (int[] size : SIZES) {
            BufferedImage image = EditorPreview.of(size[0], size[1], size[2]);
            assertNotNull(image);
            ImageIO.write(image, "png", OUTPUT.resolve(size[0] + "x" + size[1] + ".png").toFile());
        }
    }

    @Test
    void anEmptyEditorRenders() throws IOException {
        Files.createDirectories(OUTPUT);
        BufferedImage image = new EditorPreview()
                .withCatalog(EditorPreview.sampleCatalog())
                .render(640, 360, 3);
        ImageIO.write(image, "png", OUTPUT.resolve("640x360-empty.png").toFile());
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
