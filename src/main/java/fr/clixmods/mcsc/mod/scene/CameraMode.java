/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.scene;

/**
 * Where the character is being looked at from.
 *
 * <p>Deliberately not the same question as what the middle column shows — that is
 * {@code ScenePanel.View}, which chooses between the figure, the 64x64 sheet and both.
 * A player comparing the sheet against the skin on their own arm wants both answers at
 * once, and folding the two choices into one row of five buttons would have made them
 * exclusive.
 */
public enum CameraMode {
    /** The figure on its own, on the panel, turned and zoomed by hand. Works anywhere. */
    WORKSHOP("workshop", false, false),

    /**
     * Your real character in the real world, seen over their shoulder.
     *
     * <p>The game's own third person, because nothing else draws your own character:
     * {@code LevelRenderer.renderLevel} skips the local player unless they are the
     * camera entity. That also fixes how the camera goes round them — it sits behind
     * wherever they look, so orbiting means turning their view and pinning their body.
     * Real game state, saved and put back.
     */
    IN_GAME("in_game", true, true),

    /** Your own arm, drawn by the game exactly as it draws it in play. */
    FIRST_PERSON("first_person", true, true);

    private final String id;
    private final boolean needsWorld;
    private final boolean takesGameCamera;

    CameraMode(String id, boolean needsWorld, boolean takesGameCamera) {
        this.id = id;
        this.needsWorld = needsWorld;
        this.takesGameCamera = takesGameCamera;
    }

    public String labelKey() {
        return "camera.mcskincreator." + this.id;
    }

    public String tooltipKey() {
        return labelKey() + ".tooltip";
    }

    /**
     * Whether this camera has anything to show without a world loaded.
     *
     * <p>Two of the three do not, and on the title screen they are not offered at all
     * rather than offered and empty — the rule the whole interface is built on.
     */
    public boolean needsWorld() {
        return this.needsWorld;
    }

    /**
     * Whether the editor is a layer over the running game rather than a screen in front
     * of it.
     *
     * <p>When it is, nothing may paint over the world: not the mod's own backdrop, and
     * not the blurred, opaque menu background vanilla draws behind every screen. Both
     * side panels fold away too — the game draws the first-person arm across the whole
     * window, and the layers panel sat exactly where the hand is.
     */
    public boolean overlaysGame() {
        return this.needsWorld;
    }

    /**
     * Whether the game's own camera has to be borrowed, and so given back.
     *
     * <p>Only the first-person view does. It is the one thing the mod cannot draw for
     * itself and does not want to: the site had to fit its arm numerically against a
     * screenshot, and here the arm is the game's.
     */
    public boolean takesGameCamera() {
        return this.takesGameCamera;
    }
}
