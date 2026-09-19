/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.scene;

/**
 * What is behind the figure in the workshop view.
 *
 * <p>The site offers a colour or one of nine landscapes it ships as images. The mod has
 * something better to hand and no images at all: the world the player is standing in,
 * already being drawn behind the editor. So the choice is two-way — a flat panel, or the
 * game.
 *
 * <p>It is a separate question from the camera, and deliberately so. The in-game view
 * shows the <em>real</em> character, which is the right thing for judging a skin in
 * context and the wrong thing for judging an animation: it cannot be posed, because it
 * is a person standing in a world rather than a figure on a stand. Putting the world
 * behind the workshop figure is what keeps the animations, the zoom and the pan
 * available with the landscape behind them.
 *
 * <p>A preference, not part of the project: it is never serialised, and a skin saved
 * while the grass was showing does not remember the grass.
 */
public enum SceneBackdrop {
    /** The editor's own dark. Always available, and what the title screen gets. */
    PANEL("panel", false),

    /** The world, live, straight through the editor. */
    WORLD("world", true);

    private final String id;
    private final boolean needsWorld;

    SceneBackdrop(String id, boolean needsWorld) {
        this.id = id;
        this.needsWorld = needsWorld;
    }

    public String labelKey() {
        return "backdrop.mcskincreator." + this.id;
    }

    public String tooltipKey() {
        return labelKey() + ".tooltip";
    }

    /** Whether there has to be a world loaded for this to show anything. */
    public boolean needsWorld() {
        return this.needsWorld;
    }
}
