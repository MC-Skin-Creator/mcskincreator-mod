/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.project;

import static fr.clixmods.mcsc.mod.Fixtures.category;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import fr.clixmods.mcsc.mod.catalog.CatalogCategory;
import fr.clixmods.mcsc.mod.skin.SkinModel;
import fr.clixmods.mcsc.mod.style.Metrics;

/**
 * The two rules that separate a working history from one that is a step behind: the
 * snapshot is taken before the change, and a gesture is one snapshot rather than one
 * per frame.
 */
class HistoryTest {
    private final SkinProject project = new SkinProject();
    private final History history = new History(this.project);

    private final CatalogCategory hats = category("hats", "head", "cap", "crown", "helm");

    private Layer add(String itemId) {
        return this.project.add(this.hats, this.hats.items().stream()
                .filter(item -> item.id().equals(itemId))
                .findFirst()
                .orElseThrow(), "en_us");
    }

    /** The ids of the stack, bottom to top. */
    private List<String> stack() {
        List<String> ids = new ArrayList<>();
        for (Layer layer : this.project.layers()) {
            ids.add(layer.itemId());
        }
        return ids;
    }

    /** How many undo steps the history is holding. */
    private int depth() {
        int steps = 0;
        while (this.history.undo()) {
            steps++;
        }
        return steps;
    }

    @Test
    void anUntouchedHistoryHasNothingToGiveBack() {
        assertFalse(this.history.canUndo());
        assertFalse(this.history.canRedo());
        assertFalse(this.history.undo());
        assertFalse(this.history.redo());
    }

    @Test
    void undoGivesBackTheStateFromBeforeTheChange() {
        this.history.record();
        add("cap");

        assertTrue(this.history.undo());

        assertTrue(this.project.isEmpty());
    }

    @Test
    void undoRestoresTheExactOrder() {
        add("cap");
        add("crown");
        add("helm");
        Layer bottom = this.project.layers().get(0);

        this.history.record();
        this.project.move(bottom, 1);
        assertEquals(List.of("crown", "cap", "helm"), stack());

        this.history.undo();

        assertEquals(List.of("cap", "crown", "helm"), stack());
    }

    @Test
    void undoGivesBackWhatWasEditedInPlace() {
        Layer cap = add("cap");

        this.history.record();
        cap.setOpacity(40);
        this.project.touch();

        this.history.undo();

        assertEquals(100, this.project.layers().get(0).opacity());
    }

    @Test
    void redoReplaysWhatUndoTookBack() {
        this.history.record();
        add("cap");
        this.history.undo();

        assertTrue(this.history.canRedo());
        assertTrue(this.history.redo());

        assertEquals(List.of("cap"), stack());
        assertFalse(this.history.canRedo());
    }

    @Test
    void aNewEditThrowsAwayWhatCouldHaveBeenRedone() {
        this.history.record();
        add("cap");
        this.history.undo();
        assertTrue(this.history.canRedo());

        this.history.record();

        assertFalse(this.history.canRedo());
    }

    @Test
    void theModelIsPartOfWhatUndoGivesBack() {
        this.history.record();
        this.project.setModel(SkinModel.SLIM);

        this.history.undo();

        assertEquals(SkinModel.WIDE, this.project.model());
    }

    @Test
    void oneGestureIsOneStepWhateverTheFrameCount() {
        Layer cap = add("cap");
        Object slider = new Object();

        for (int opacity = 99; opacity >= 60; opacity--) {
            this.history.beginGesture(slider);
            cap.setOpacity(opacity);
            this.project.touch();
        }
        this.history.endGesture();

        assertTrue(this.history.undo());
        assertEquals(100, this.project.layers().get(0).opacity());
        assertFalse(this.history.canUndo());
    }

    @Test
    void anotherControlStartsAnotherStep() {
        add("cap");

        this.history.beginGesture(new Object());
        this.history.beginGesture(new Object());

        assertEquals(2, depth());
    }

    @Test
    void endingAGestureLetsTheSameControlRecordAgain() {
        add("cap");
        Object slider = new Object();

        this.history.beginGesture(slider);
        this.history.endGesture();
        this.history.beginGesture(slider);

        assertEquals(2, depth());
    }

    @Test
    void theGestureRecordsBeforeTheDragMovedAnything() {
        Layer cap = add("cap");
        Object slider = new Object();

        this.history.beginGesture(slider);
        cap.setHue(90);
        this.history.beginGesture(slider);
        cap.setHue(180);
        this.history.endGesture();

        this.history.undo();

        assertEquals(0, this.project.layers().get(0).hue());
    }

    @Test
    void theHistoryStopsAtItsDepth() {
        for (int i = 0; i < Metrics.HISTORY_DEPTH + 10; i++) {
            this.history.record();
        }

        assertEquals(Metrics.HISTORY_DEPTH, depth());
    }

    @Test
    void theOldestStepIsTheOneDropped() {
        add("cap");
        // One record past the cap, so the very first snapshot has to fall off.
        for (int i = 0; i <= Metrics.HISTORY_DEPTH; i++) {
            this.history.record();
            add("crown");
        }

        // The first snapshot - the one holding a single layer - has fallen off the
        // end, so the oldest step still reachable is the second one.
        while (this.history.undo()) {
            // wind all the way back
        }

        assertEquals(2, this.project.layers().size());
    }
}
