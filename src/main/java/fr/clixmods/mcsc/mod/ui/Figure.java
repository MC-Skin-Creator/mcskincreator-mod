/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui;

import fr.clixmods.mcsc.mod.scene.SceneShot;

/**
 * The player standing in the scene.
 *
 * <p>An interface for the same reason {@link Canvas} is one: {@link PlayerFigure} renders
 * the game's own player, which needs a running game to exist at all, and the preview tool
 * in the tests stands something else in its place. Everything the scene does around the
 * figure — where the bar goes, how big the portrait may be, what the dock holds — is then
 * laid out and looked at without one.
 *
 * <p>Two methods, and it used to be six: the other four carried the turn, because the
 * figure was vanilla's player widget and the widget owned its own rotation. The scene has
 * a camera now, and a camera is not the figure's to keep — so the turn, the tilt, the
 * zoom and the pan live in {@code ScenePanel}, and arrive here as a {@link SceneShot}.
 * What is left is what this seam was always for: somewhere to draw, and something to draw
 * that does not need a GPU.
 */
public interface Figure {
    /**
     * Says where the figure goes and how big it may be.
     *
     * <p>Called on every layout, which is to say on every pick.
     *
     * @param width zero when there is no room for a figure at all
     */
    void place(int x, int y, int width, int height);

    /**
     * Draws the figure as the scene currently wants it.
     *
     * <p>{@code shot} may say the game is drawing the character itself, in which case the
     * honest answer is to draw nothing.
     */
    void draw(Canvas canvas, SceneShot shot, int mouseX, int mouseY, float delta);
}
