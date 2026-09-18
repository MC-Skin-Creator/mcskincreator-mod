/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui;

/**
 * The player standing in the scene.
 *
 * <p>An interface for the same reason {@link Canvas} is one: {@link PlayerFigure}
 * renders the game's own player, which needs a running game to exist at all, and the
 * preview tool in the tests stands something else in its place. Everything the scene
 * does around the figure — where the bar goes, how big the portrait may be, what the
 * dock holds — is then laid out and looked at without one.
 */
public interface Figure {
    /**
     * Says where the figure goes and how big it may be.
     *
     * <p>Called on every layout, which is to say on every pick. A figure that is
     * already the right size keeps whichever way it was turned; one that is not is
     * built again, which is also what puts it back facing forward.
     *
     * @param width zero when there is no room for a figure at all
     */
    void place(int x, int y, int width, int height);

    /** Puts the figure back the way it started, facing forward. */
    void reset();

    void draw(Canvas canvas, int mouseX, int mouseY, float delta);

    /**
     * The figure turns under the mouse, so it is handed the gesture itself.
     *
     * @return true when the press landed on the figure and started a turn
     */
    boolean press(double mouseX, double mouseY, int button);

    /** Turning is by how far the mouse moved, not by where it ended up. */
    void drag(double mouseX, double mouseY, double dragX, double dragY, int button);

    void release(double mouseX, double mouseY, int button);
}
