/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.scene;

/**
 * Everything about how the figure should look, this frame.
 *
 * <p>One argument rather than six setters, and it exists so that the scene can keep the
 * decisions — which camera, which pose, how far round it has been turned — while
 * whatever draws the figure keeps only the drawing. That split is what lets the editor
 * be laid out and looked at without a running game: the preview tool answers the same
 * call with a hatched rectangle and never has to know what a pose is.
 *
 * @param camera   where the character is being looked at from
 * @param pose     what they are doing
 * @param playing  false leaves them standing rather than frozen mid-stride
 * @param seconds  how far into the animation, for whatever cycle the pose runs
 * @param eye      the turn, the tilt, the zoom and the pan
 */
public record SceneShot(CameraMode camera, ScenePose pose, boolean playing, float seconds,
                        SceneCamera eye) {
    /** Whether the game is drawing the character itself, so there is nothing to draw here. */
    public boolean drawnByTheGame() {
        return this.camera.overlaysGame();
    }
}
