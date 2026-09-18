/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.preview;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import fr.clixmods.mcsc.mod.style.Metrics;
import fr.clixmods.mcsc.mod.style.Palette;
import fr.clixmods.mcsc.mod.style.Surface;
import net.minecraft.network.chat.Component;

/**
 * Every material the editor is made of, at the heights it is used at.
 *
 * <p>Four materials and three tones is the whole system, and it is small enough to fit
 * on one sheet — which is the point. A material that has to be run to be seen is a
 * material nobody checks, and the last time this interface went wrong it went wrong
 * because a sprite chosen from its name behaved nothing like its name at the size it
 * was given.
 */
class MaterialsSheetTest {
    private static final Path OUTPUT = Paths.get("build", "ui-preview");
    private static final int SCALE = 3;
    private static final int[] HEIGHTS = {12, 14, 16, 20, 26};
    private static final int SAMPLE_WIDTH = 60;

    private interface Swatch {
        void draw(ImageCanvas canvas, int x, int y, int width, int height);
    }

    private record Material(String name, Swatch swatch) {
    }

    @Test
    void theMaterialsSheetIsWrittenOut() throws IOException {
        List<Material> materials = List.of(
                new Material("panel", Surface::panel),
                new Material("slot", (c, x, y, w, h) -> Surface.slot(c, x, y, w, h)),
                new Material("slot hover", Surface::slotHighlight),
                new Material("button", (c, x, y, w, h) ->
                        Surface.button(c, x, y, w, h, Surface.Tone.NEUTRAL, false, false)),
                new Material("button hover", (c, x, y, w, h) ->
                        Surface.button(c, x, y, w, h, Surface.Tone.NEUTRAL, true, false)),
                new Material("button down", (c, x, y, w, h) ->
                        Surface.button(c, x, y, w, h, Surface.Tone.NEUTRAL, false, true)),
                new Material("green", (c, x, y, w, h) ->
                        Surface.button(c, x, y, w, h, Surface.Tone.GREEN, false, false)),
                new Material("green hover", (c, x, y, w, h) ->
                        Surface.button(c, x, y, w, h, Surface.Tone.GREEN, true, false)),
                new Material("red", (c, x, y, w, h) ->
                        Surface.button(c, x, y, w, h, Surface.Tone.RED, false, false)),
                new Material("tab off", (c, x, y, w, h) -> Surface.tab(c, x, y, w, h, false, false)),
                new Material("tab on", (c, x, y, w, h) -> Surface.tab(c, x, y, w, h, true, false)),
                new Material("field", (c, x, y, w, h) -> Surface.field(c, x, y, w, h, false)),
                new Material("field focus", (c, x, y, w, h) -> Surface.field(c, x, y, w, h, true)),
                new Material("slider rail", (c, x, y, w, h) -> Surface.sliderRail(c, x, y, w, h, false)),
                new Material("selected row", (c, x, y, w, h) -> {
                    Surface.slot(c, x, y, w, h, Palette.GREEN_FILL);
                    c.fill(x + Metrics.OUTLINE, y + Metrics.OUTLINE, Metrics.BAND,
                            h - Metrics.OUTLINE * 2, Palette.GREEN);
                }),
                new Material("tick off", (c, x, y, w, h) -> Surface.checkbox(c, x, y, false, false)),
                new Material("tick on", (c, x, y, w, h) -> Surface.checkbox(c, x, y, true, false)));

        int labelWidth = 84;
        int gap = 6;
        int rowHeight = 32;
        int width = labelWidth + HEIGHTS.length * (SAMPLE_WIDTH + gap) + gap;
        int height = rowHeight * materials.size() + 20;

        ImageCanvas canvas = new ImageCanvas(width, height);
        canvas.supply(fr.clixmods.mcsc.mod.style.Tiles.texture(), grain());
        canvas.fill(0, 0, width, height, Palette.VOID);

        int cursorX = labelWidth;
        for (int sampleHeight : HEIGHTS) {
            canvas.textFlat(Component.literal(sampleHeight + "px"), cursorX, 4, Palette.INK_FAINT);
            cursorX += SAMPLE_WIDTH + gap;
        }

        int cursorY = 18;
        for (Material material : materials) {
            canvas.textFlat(Component.literal(material.name()), 4,
                    cursorY + (rowHeight - 9) / 2, Palette.INK_MUTED);
            int x = labelWidth;
            for (int sampleHeight : HEIGHTS) {
                material.swatch().draw(canvas, x, cursorY + (rowHeight - sampleHeight) / 2,
                        SAMPLE_WIDTH, sampleHeight);
                x += SAMPLE_WIDTH + gap;
            }
            cursorY += rowHeight;
        }

        Files.createDirectories(OUTPUT);
        ImageIO.write(new EditorPreview().magnify(canvas.image(), SCALE), "png",
                OUTPUT.resolve("materials.png").toFile());
    }

    private static java.awt.image.BufferedImage grain() {
        int size = fr.clixmods.mcsc.mod.style.Tiles.SIZE;
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(size, size,
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                image.setRGB(x, y, fr.clixmods.mcsc.mod.style.Tiles.grey(
                        fr.clixmods.mcsc.mod.style.Tiles.shade(x, y)));
            }
        }
        return image;
    }
}
