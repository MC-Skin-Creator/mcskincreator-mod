/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.project;

import java.util.ArrayDeque;
import java.util.Deque;

import fr.clixmods.mcsc.mod.style.Metrics;

/**
 * Undo and redo over whole project snapshots.
 *
 * <p>Two rules make the difference between a history that works and one that is
 * subtly a step behind. A snapshot is taken <em>before</em> the change, never after
 * — taking it afterwards records the result and loses the thing being undone. And a
 * continuous gesture takes one snapshot for the whole gesture, not one per frame:
 * dragging a slider is one edit, so {@link #beginGesture(Object)} keys the snapshot
 * on the control being dragged and ignores every repeat until the gesture ends.
 */
public final class History {
    private final Deque<SkinProject.Snapshot> past = new ArrayDeque<>();
    private final Deque<SkinProject.Snapshot> future = new ArrayDeque<>();
    private final SkinProject project;

    private Object gesture;

    public History(SkinProject project) {
        this.project = project;
    }

    /** Records the state about to be replaced. Call before mutating, every time. */
    public void record() {
        this.past.addLast(this.project.snapshot());
        while (this.past.size() > Metrics.HISTORY_DEPTH) {
            this.past.removeFirst();
        }
        this.future.clear();
    }

    /**
     * Records once for a gesture identified by {@code owner}, then stays quiet until
     * {@link #endGesture()}.
     */
    public void beginGesture(Object owner) {
        if (this.gesture != owner) {
            this.gesture = owner;
            record();
        }
    }

    public void endGesture() {
        this.gesture = null;
    }

    public boolean canUndo() {
        return !this.past.isEmpty();
    }

    public boolean canRedo() {
        return !this.future.isEmpty();
    }

    public boolean undo() {
        if (this.past.isEmpty()) {
            return false;
        }
        this.future.addLast(this.project.snapshot());
        this.project.restore(this.past.removeLast());
        return true;
    }

    public boolean redo() {
        if (this.future.isEmpty()) {
            return false;
        }
        this.past.addLast(this.project.snapshot());
        this.project.restore(this.future.removeLast());
        return true;
    }
}
