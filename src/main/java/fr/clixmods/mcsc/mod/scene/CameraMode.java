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
    WORKSHOP("workshop", false),

    /** Your real character, in the real world, with a camera going round them. */
    IN_GAME("in_game", true),

    /** Your own arm, drawn by the game exactly as it draws it in play. */
    FIRST_PERSON("first_person", true);

    private final String id;
    private final boolean needsWorld;

    CameraMode(String id, boolean needsWorld) {
        this.id = id;
        this.needsWorld = needsWorld;
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

    /** Whether this camera takes the game's own camera over, and so has to give it back. */
    public boolean takesGameCamera() {
        return this != WORKSHOP;
    }
}
