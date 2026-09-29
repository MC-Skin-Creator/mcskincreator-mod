/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.skin;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import fr.clixmods.mcsc.engine.Composition;
import fr.clixmods.mcsc.mod.catalog.ThumbCrop;

/**
 * The front view the library draws its thumbnails from, and the frames it cuts out of
 * it.
 *
 * <p>Both come from {@code mcsc-engine}, so what is checked here is the seam: that the
 * bytes of an atlas reach the sheet as the ARGB a texture is uploaded from, and that no
 * crop has quietly grown its own rectangle again.
 */
class FrontSpriteTest {
    /** Paints one face of the skin, as an atlas buffer holds it. */
    private static byte[] skinWith(int u, int v, int width, int height, int argb) {
        byte[] skin = new byte[Composition.BYTES];
        for (int row = 0; row < height; row++) {
            for (int column = 0; column < width; column++) {
                int offset = ((v + row) * FrontSprite.SKIN_SIZE + u + column) * 4;
                skin[offset] = (byte) (argb >> 16);
                skin[offset + 1] = (byte) (argb >> 8);
                skin[offset + 2] = (byte) argb;
                skin[offset + 3] = (byte) (argb >>> 24);
            }
        }
        return skin;
    }

    @Nested
    class TheSprite {
        private final int[] sheet = new int[FrontSprite.WIDTH * FrontSprite.HEIGHT];

        private int at(int x, int y) {
            return this.sheet[y * FrontSprite.WIDTH + x];
        }

        @Test
        void the_face_lands_where_the_head_is_drawn() {
            int red = 0xFFFF0000;
            FrontSprite.draw(skinWith(8, 8, 8, 8, red), false, this.sheet, FrontSprite.WIDTH, 0, 0);

            // The head occupies the four middle columns of the top eight rows.
            assertEquals(red, at(4, 0));
            assertEquals(red, at(11, 7));
            assertEquals(0, at(3, 0), "nothing is drawn left of the head");
            assertEquals(0, at(4, 8), "the body is empty, so the row under the head is");
        }

        @Test
        void an_empty_skin_leaves_the_sheet_alone() {
            FrontSprite.draw(new byte[Composition.BYTES], false, this.sheet, FrontSprite.WIDTH, 0, 0);

            assertArrayEquals(new int[FrontSprite.WIDTH * FrontSprite.HEIGHT], this.sheet);
        }

        @Test
        void the_slim_arm_is_pushed_against_the_body() {
            int blue = 0xFF0000FF;
            // The right arm of the slim model is three texels wide, at the same corner.
            FrontSprite.draw(skinWith(44, 20, 3, 12, blue), true, this.sheet, FrontSprite.WIDTH, 0, 0);

            assertEquals(0, at(0, 8), "the missing column is the outer one");
            assertEquals(blue, at(1, 8));
            assertEquals(blue, at(3, 8));
        }

        @Test
        void a_sprite_is_written_at_its_place_in_a_sheet() {
            int[] sheet = new int[FrontSprite.WIDTH * 2 * FrontSprite.HEIGHT];
            int green = 0xFF00FF00;
            FrontSprite.draw(skinWith(8, 8, 8, 8, green), false, sheet, FrontSprite.WIDTH * 2,
                    FrontSprite.WIDTH, 0);

            assertEquals(green, sheet[FrontSprite.WIDTH + 4], "the second column of sprites");
            assertEquals(0, sheet[4], "and the first one is untouched");
        }
    }

    @Nested
    class TheCrops {
        @Test
        void every_crop_is_the_one_the_site_frames_with() {
            for (ThumbCrop crop : ThumbCrop.values()) {
                int[] expected = Composition.SPRITE_CROP.get(crop.name().toLowerCase(java.util.Locale.ROOT)
                        .replace("_", ""));
                assertArrayEquals(expected,
                        new int[] {crop.x(), crop.y(), crop.width(), crop.height()},
                        "crop " + crop);
            }
        }

        @Test
        void a_crop_the_catalogue_does_not_name_falls_back_to_the_whole_figure() {
            assertEquals(ThumbCrop.ALL, ThumbCrop.of("torso-and-a-half"));
            assertEquals(ThumbCrop.ALL, ThumbCrop.of(null));
            assertEquals(ThumbCrop.HEAD, ThumbCrop.of("HEAD"));
        }
    }
}
