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

import fr.clixmods.mcsc.mod.style.Palette;
import fr.clixmods.mcsc.mod.style.Sprites;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Every sprite the editor is made of, drawn at the sizes the editor draws it at.
 *
 * <p>A sprite is not a colour swatch: it is a picture with a border that the game
 * slices, and it only looks like itself between certain sizes. {@code widget/tab} is
 * the one that taught this — it is 130 by 24 with no bottom border at all, because it
 * is meant to sit on the edge of the panel it belongs to, and a row of them floating
 * in the middle of a column reads as a row of brackets.
 *
 * <p>So this writes the materials out to be looked at, at the sizes in use and at two
 * that bracket them. Choosing a sprite for a control is then a decision somebody can
 * check rather than one that has to be run to be seen.
 */
class MaterialsSheetTest {
    private static final Path OUTPUT = Paths.get("build", "ui-preview");
    private static final int SCALE = 3;
    private static final int[] HEIGHTS = {12, 14, 16, 20, 24};
    private static final int SAMPLE_WIDTH = 60;

    private record Material(String name, Identifier sprite) {
    }

    @Test
    void theMaterialsSheetIsWrittenOut() throws IOException {
        List<Material> materials = List.of(
                new Material("panel", Sprites.PANEL),
                new Material("slot", Sprites.SLOT),
                new Material("slot hover", Sprites.SLOT_HOVERED),
                new Material("button", Sprites.BUTTON),
                new Material("button hover", Sprites.BUTTON_HOVERED),
                new Material("button off", Sprites.BUTTON_DISABLED),
                new Material("tab", Sprites.TAB),
                new Material("tab hover", Sprites.TAB_HOVERED),
                new Material("tab on", Sprites.TAB_SELECTED),
                new Material("tab on hover", Sprites.TAB_SELECTED_HOVERED),
                new Material("field", Sprites.FIELD),
                new Material("field focus", Sprites.FIELD_FOCUSED),
                new Material("slider", Sprites.SLIDER),
                new Material("slider handle", Sprites.SLIDER_HANDLE),
                new Material("checkbox", Sprites.CHECKBOX),
                new Material("checkbox on", Sprites.CHECKBOX_TICKED));

        int labelWidth = 84;
        int gap = 6;
        int rowHeight = 30;
        int width = labelWidth + HEIGHTS.length * (SAMPLE_WIDTH + gap) + gap;
        int height = rowHeight * materials.size() + 20;

        ImageCanvas canvas = new ImageCanvas(width, height);
        canvas.fill(0, 0, width, height, 0xFF202225);

        int cursorX = labelWidth;
        for (int sampleHeight : HEIGHTS) {
            canvas.textFlat(Component.literal(sampleHeight + "px"), cursorX, 4, Palette.INK_MUTED);
            cursorX += SAMPLE_WIDTH + gap;
        }

        int cursorY = 18;
        for (Material material : materials) {
            canvas.textFlat(Component.literal(material.name()), 4,
                    cursorY + (rowHeight - 9) / 2, Palette.INK);
            int x = labelWidth;
            for (int sampleHeight : HEIGHTS) {
                canvas.sprite(material.sprite(), x, cursorY + (rowHeight - sampleHeight) / 2,
                        SAMPLE_WIDTH, sampleHeight);
                x += SAMPLE_WIDTH + gap;
            }
            cursorY += rowHeight;
        }

        Files.createDirectories(OUTPUT);
        ImageIO.write(new EditorPreview().magnify(canvas.image(), SCALE), "png",
                OUTPUT.resolve("materials.png").toFile());
    }
}
