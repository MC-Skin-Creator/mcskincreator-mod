/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.skin;

import static fr.clixmods.mcsc.mod.Fixtures.category;
import static fr.clixmods.mcsc.mod.Fixtures.readyMade;
import static fr.clixmods.mcsc.mod.Fixtures.single;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import fr.clixmods.mcsc.mod.catalog.Catalog;
import fr.clixmods.mcsc.mod.catalog.CatalogModel;

/**
 * The pictures of ready-made stacks, and above all that one is blended once: the
 * models window asks for all of them again every time an atlas lands.
 */
class ReadyMadeSkinsTest {
    private final CatalogModel dressed = readyMade("dressed", CatalogModel.Kind.MODEL, false,
            "skin/pale", "hair/long");
    private final CatalogModel bare = readyMade("bare", CatalogModel.Kind.MODEL, false, "skin/pale");
    private final Catalog catalog = new Catalog(
            List.of(single("skin", "base", "pale"), category("hair", "head", "long")),
            Map.of(), List.of(this.dressed, this.bare), List.of());

    private final Map<String, CategorySprites> atlases = new HashMap<>();
    private final ReadyMadeSkins blends = new ReadyMadeSkins();

    /** One opaque pixel of one colour at {@code offset}, the rest transparent. */
    private static byte[] skin(int offset, int red) {
        byte[] buffer = new byte[Composite.BYTES];
        buffer[offset] = (byte) red;
        buffer[offset + 3] = (byte) 0xFF;
        return buffer;
    }

    private void arrives(String categoryId, byte[] buffer) {
        this.atlases.put(categoryId, CategorySprites.unuploaded(categoryId, List.of(buffer)));
    }

    private List<byte[]> pictures() {
        return this.blends.of(this.catalog.models(), this.catalog, this.atlases::get, false, null);
    }

    @Test
    void stacks_the_pieces_bottom_first() {
        arrives("skin", skin(0, 10));
        arrives("hair", skin(4, 20));

        byte[] picture = pictures().get(0);
        assertEquals(10, picture[0]);
        assertEquals(20, picture[4]);
    }

    @Test
    void draws_nothing_for_a_stack_whose_atlas_is_still_on_its_way() {
        arrives("skin", skin(0, 10));

        List<byte[]> pictures = pictures();
        assertArrayEquals(new byte[Composite.BYTES], pictures.get(0));
        assertEquals(10, pictures.get(1)[0]);
    }

    @Test
    void does_not_blend_a_picture_again_when_its_atlases_have_not_moved() {
        arrives("skin", skin(0, 10));
        byte[] first = pictures().get(1);

        // Another category landing is exactly what used to redraw every picture.
        arrives("hair", skin(4, 20));
        assertSame(first, pictures().get(1));
    }

    @Test
    void blends_it_again_when_one_of_its_atlases_was_replaced() {
        arrives("skin", skin(0, 10));
        byte[] first = pictures().get(1);

        arrives("skin", skin(0, 30));
        byte[] second = pictures().get(1);
        assertNotSame(first, second);
        assertEquals(30, second[0]);
    }
}
