/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.skin;

import static fr.clixmods.mcsc.mod.Fixtures.category;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import fr.clixmods.mcsc.mod.catalog.CatalogCategory;
import fr.clixmods.mcsc.mod.catalog.CatalogItem;
import fr.clixmods.mcsc.mod.catalog.CatalogText;
import fr.clixmods.mcsc.mod.project.Layer;
import fr.clixmods.mcsc.mod.project.SkinProject;
import fr.clixmods.mcsc.mod.skin.SkinModel;

/**
 * What a stack composes to, which is the one thing the player looks at and the one
 * thing that has to match the site byte for byte.
 *
 * <p>The blending arithmetic is the library's, so these do not re-test it: they check
 * that the mod hands the library the right layers, in the right order, in the units
 * it works in.
 */
class CompositeTest {
    private static final String CATEGORY = "hats";

    /** A whole sheet of one colour, which makes a composition easy to read back. */
    private static byte[] filled(int red, int green, int blue, int alpha) {
        byte[] buffer = new byte[Composite.BYTES];
        for (int offset = 0; offset < buffer.length; offset += 4) {
            buffer[offset] = (byte) red;
            buffer[offset + 1] = (byte) green;
            buffer[offset + 2] = (byte) blue;
            buffer[offset + 3] = (byte) alpha;
        }
        return buffer;
    }

    /** The first pixel, as four values of 0 to 255 — every sheet here is uniform. */
    private static int[] first(byte[] sheet) {
        return new int[] {
            sheet[0] & 0xFF, sheet[1] & 0xFF, sheet[2] & 0xFF, sheet[3] & 0xFF,
        };
    }

    @Nested
    class PlainBuffers {
        @Test
        void nothing_stacked_is_transparent() {
            byte[] sheet = Composite.of(Arrays.asList());

            assertEquals(Composite.BYTES, sheet.length);
            assertArrayEquals(new byte[Composite.BYTES], sheet);
        }

        @Test
        void the_last_buffer_covers_the_ones_before() {
            byte[] sheet = Composite.of(Arrays.asList(
                    filled(255, 0, 0, 255), filled(0, 0, 255, 255)));

            assertArrayEquals(new int[] {0, 0, 255, 255}, first(sheet));
        }

        @Test
        void a_transparent_layer_leaves_what_is_under_it() {
            byte[] sheet = Composite.of(Arrays.asList(
                    filled(255, 0, 0, 255), filled(0, 0, 255, 0)));

            assertArrayEquals(new int[] {255, 0, 0, 255}, first(sheet));
        }

        @Test
        void a_buffer_that_is_not_here_yet_is_skipped() {
            byte[] sheet = Composite.of(Arrays.asList(
                    filled(255, 0, 0, 255), null, new byte[8]));

            assertArrayEquals(new int[] {255, 0, 0, 255}, first(sheet));
        }
    }

    @Nested
    class TheEditedProject {
        private final SkinProject project = new SkinProject();
        private final Map<String, byte[]> atlas = new HashMap<>();

        /** One atlas buffer per rank, keyed the way the screen looks them up. */
        private final Composite.Pixels pixels =
                (categoryId, atlasIndex) -> this.atlas.get(categoryId + "#" + atlasIndex);

        private Layer stack(String itemId, int atlasIndex, int slimAtlasIndex) {
            CatalogCategory category = category(CATEGORY, "head", itemId);
            CatalogItem item = new CatalogItem(itemId, new CatalogText(itemId, itemId, itemId),
                    atlasIndex, slimAtlasIndex, null, "");
            return this.project.add(category, item, "en_us");
        }

        private Layer stack(String itemId, int atlasIndex) {
            return stack(itemId, atlasIndex, CatalogItem.NONE);
        }

        private int[] composed() {
            return first(Composite.of(this.project, this.pixels));
        }

        @Test
        void an_empty_project_composes_to_nothing() {
            assertArrayEquals(new byte[Composite.BYTES], Composite.of(this.project, this.pixels));
        }

        @Test
        void the_upper_layer_covers_the_one_below() {
            this.atlas.put(CATEGORY + "#0", filled(255, 0, 0, 255));
            this.atlas.put(CATEGORY + "#1", filled(0, 0, 255, 255));
            stack("red", 0);
            stack("blue", 1);

            assertArrayEquals(new int[] {0, 0, 255, 255}, composed());
        }

        @Test
        void a_hidden_layer_contributes_nothing() {
            this.atlas.put(CATEGORY + "#0", filled(255, 0, 0, 255));
            this.atlas.put(CATEGORY + "#1", filled(0, 0, 255, 255));
            stack("red", 0);
            stack("blue", 1).setVisible(false);

            assertArrayEquals(new int[] {255, 0, 0, 255}, composed());
        }

        @Test
        void opacity_is_a_percentage_here_and_a_fraction_there() {
            this.atlas.put(CATEGORY + "#0", filled(0, 0, 0, 255));
            this.atlas.put(CATEGORY + "#1", filled(255, 255, 255, 255));
            stack("black", 0);
            stack("white", 1).setOpacity(50);

            // Half of white over black: the mod says 50, the library wants 0.5.
            assertArrayEquals(new int[] {128, 128, 128, 255}, composed());
        }

        @Test
        void an_adjustment_is_a_percentage_here_and_a_factor_there() {
            this.atlas.put(CATEGORY + "#0", filled(255, 0, 0, 255));
            stack("red", 0).setSaturation(0);

            // Saturation zeroed leaves the lightness alone, so pure red turns mid grey.
            assertArrayEquals(new int[] {128, 128, 128, 255}, composed());
        }

        @Test
        void an_untouched_layer_is_composed_as_it_came() {
            this.atlas.put(CATEGORY + "#0", filled(12, 34, 56, 255));
            stack("plain", 0);

            assertArrayEquals(new int[] {12, 34, 56, 255}, composed());
        }

        @Test
        void a_layer_whose_atlas_has_not_arrived_is_left_out() {
            this.atlas.put(CATEGORY + "#0", filled(255, 0, 0, 255));
            stack("red", 0);
            stack("still-loading", 7);

            assertArrayEquals(new int[] {255, 0, 0, 255}, composed());
        }

        @Test
        void the_slim_model_reads_the_slim_buffer() {
            this.atlas.put(CATEGORY + "#0", filled(255, 0, 0, 255));
            this.atlas.put(CATEGORY + "#1", filled(0, 255, 0, 255));
            stack("two-cut", 0, 1);

            assertArrayEquals(new int[] {255, 0, 0, 255}, composed());

            this.project.setModel(SkinModel.SLIM);
            assertArrayEquals(new int[] {0, 255, 0, 255}, composed());
        }
    }

    @Nested
    class Colours {
        /** A hat whose one key is pure red, with no zone map: every opaque pixel is that key. */
        private final CatalogItem redHat = new CatalogItem("cap", new CatalogText("Cap", "Cap", "Cap"),
                0, CatalogItem.NONE, null, "", Map.of("main", 0xFF0000));

        @Test
        void a_recoloured_key_reaches_the_pixels() {
            byte[] sheet = Composite.recolor(CATEGORY, this.redHat, Map.of("main", 0x0000FF), false,
                    filled(255, 0, 0, 255));

            assertArrayEquals(new int[] {0, 0, 255, 255}, first(sheet));
        }

        @Test
        void no_recolour_is_the_same_buffer() {
            byte[] buffer = filled(255, 0, 0, 255);

            assertEquals(buffer, Composite.recolor(CATEGORY, this.redHat, Map.of(), false, buffer));
        }

        @Test
        void a_layers_colours_are_composed_into_the_project() {
            CatalogCategory hats = new CatalogCategory(CATEGORY, "head", new CatalogText("Hats", "Hats", "Hats"),
                    false, null, "", java.util.List.of(this.redHat));
            SkinProject project = new SkinProject();
            project.add(hats, this.redHat, "en_us").setColor("main", 0x00FF00);

            byte[] sheet = Composite.of(project, (category, index) -> filled(255, 0, 0, 255));

            assertArrayEquals(new int[] {0, 255, 0, 255}, first(sheet));
        }
    }
}
