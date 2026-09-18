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
     * The sizes this screen gives itself.
     *
     * <p>The editor takes its own GUI scale — the whole one nearest to 960 by 540 — so
     * these are what a window actually produces: 1080p and 4K land on 960x540, 1440p on
     * 853x480, 1600x900 on 800x450, and 720p is left at a scale of one. Each is written
     * out magnified by that scale, so the picture is the size it appears on the screen
     * it came from.
     */
    private static final int[][] SIZES = {
        {960, 540, 2}, {853, 480, 3}, {800, 450, 2}, {1280, 720, 1}, {640, 360, 3},
    };

    @Test
    void theEditorRendersAtEverySizeTheGameGivesIt() throws IOException {
        Files.createDirectories(OUTPUT);
        for (int[] size : SIZES) {
            BufferedImage image = EditorPreview.of(size[0], size[1], size[2]);
            assertNotNull(image);
            ImageIO.write(image, "png", OUTPUT.resolve(size[0] + "x" + size[1] + ".png").toFile());
            ImageIO.write(EditorPreview.of("en_us", size[0], size[1], size[2]), "png",
                    OUTPUT.resolve(size[0] + "x" + size[1] + "-en.png").toFile());
        }
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
