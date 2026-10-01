/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.project;

import static fr.clixmods.mcsc.mod.Fixtures.catalog;
import static fr.clixmods.mcsc.mod.Fixtures.category;
import static fr.clixmods.mcsc.mod.Fixtures.readyMade;
import static fr.clixmods.mcsc.mod.Fixtures.single;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import fr.clixmods.mcsc.mod.catalog.Catalog;
import fr.clixmods.mcsc.mod.catalog.CatalogModel;
import fr.clixmods.mcsc.mod.skin.SkinModel;

/**
 * What choosing one of the catalogue's ready-made stacks does, which is not the same
 * thing for the two kinds — the whole reason they are told apart.
 */
class ReadyMadeTest {
    private final Catalog catalog = catalog(
            single("skin", "base", "pale", "tan"),
            category("hair", "head", "long", "short"),
            category("top", "torso", "shirt", "coat"),
            category("shoe", "legs", "boots"));

    private final SkinProject project = new SkinProject();

    /** The ids of the stack, bottom to top. */
    private List<String> stack() {
        List<String> ids = new ArrayList<>();
        for (Layer layer : this.project.layers()) {
            ids.add(layer.itemId());
        }
        return ids;
    }

    private int apply(CatalogModel model) {
        return this.project.apply(model, this.catalog, "en_us");
    }

    @Nested
    class AModel {
        @Test
        void replaces_the_stack_rather_than_adding_to_it() {
            apply(readyMade("first", CatalogModel.Kind.MODEL, false, "skin/pale", "hair/long"));
            int stacked = apply(readyMade("second", CatalogModel.Kind.MODEL, false, "skin/tan", "top/coat"));

            assertEquals(2, stacked);
            assertEquals(List.of("tan", "coat"), stack());
        }

        @Test
        void brings_the_player_model_it_was_drawn_for() {
            apply(readyMade("fine", CatalogModel.Kind.MODEL, true, "skin/pale"));
            assertEquals(SkinModel.SLIM, project.model());

            apply(readyMade("wide", CatalogModel.Kind.MODEL, false, "skin/tan"));
            assertEquals(SkinModel.WIDE, project.model());
        }

        @Test
        void keeps_its_pieces_in_the_order_the_catalogue_lists_them() {
            apply(readyMade("dressed", CatalogModel.Kind.MODEL, false,
                    "skin/pale", "top/shirt", "hair/short", "shoe/boots"));

            assertEquals(List.of("pale", "shirt", "short", "boots"), stack());
        }
    }

    @Nested
    class AnOutfit {
        @Test
        void stacks_on_the_body_already_there() {
            apply(readyMade("start", CatalogModel.Kind.MODEL, false, "skin/pale", "hair/long"));
            int stacked = apply(readyMade("clothes", CatalogModel.Kind.OUTFIT, false, "top/coat", "shoe/boots"));

            assertEquals(2, stacked);
            assertEquals(List.of("pale", "long", "coat", "boots"), stack());
        }

        @Test
        void leaves_the_player_model_alone() {
            apply(readyMade("fine", CatalogModel.Kind.MODEL, true, "skin/pale"));
            apply(readyMade("clothes", CatalogModel.Kind.OUTFIT, false, "top/coat"));

            assertEquals(SkinModel.SLIM, project.model(),
                    "an outfit is worn by whichever body is already there");
        }
    }

    @Nested
    class APieceTheCatalogueNoLongerHas {
        @Test
        void is_skipped_rather_than_costing_the_whole_model() {
            int stacked = apply(readyMade("stale", CatalogModel.Kind.MODEL, false,
                    "skin/pale", "hair/gone", "gone/whatever", "top/coat"));

            assertEquals(2, stacked);
            assertEquals(List.of("pale", "coat"), stack());
        }

        @Test
        void can_leave_a_model_with_nothing_in_it() {
            int stacked = apply(readyMade("all-gone", CatalogModel.Kind.MODEL, false, "gone/whatever"));

            assertEquals(0, stacked);
            assertTrue(project.isEmpty());
        }
    }

    @Test
    void a_single_category_still_replaces_rather_than_stacking() {
        apply(readyMade("two-skins", CatalogModel.Kind.MODEL, false, "skin/pale", "skin/tan", "hair/long"));

        assertEquals(List.of("tan", "long"), stack(),
                "the second skin replaces the first and stays at the bottom");
    }

    @Test
    void the_revision_moves_once_so_one_undo_takes_the_whole_model_back() {
        int before = this.project.revision();
        apply(readyMade("dressed", CatalogModel.Kind.MODEL, false, "skin/pale", "top/shirt", "hair/short"));

        assertTrue(this.project.revision() > before, "the preview has to notice");
    }
}
