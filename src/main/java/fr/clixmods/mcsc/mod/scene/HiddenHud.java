/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.scene;

/**
 * Whether the editor is asking for the game's HUD to be left undrawn this frame.
 *
 * <p>The game's own flag would do this in one line, and it is not usable: both targets
 * guard the {@code ItemInHandRenderer} call with the same boolean they guard the HUD
 * with, so hiding the HUD that way takes the held hand with it — and the hand is the
 * whole point of the first-person view. This is the half of that flag the editor
 * actually wants, and {@code GuiMixin} is what makes the two separable.
 *
 * <p>A flag rather than a callback, for the same reason {@link WorldPose} is: it is read
 * once a frame from the render thread and written from the editor, so a volatile read
 * and a branch is the whole cost, and there is nothing to unregister if the editor goes
 * away badly.
 *
 * <p>It hides <em>drawing</em> and nothing else. The game's flag is a setting the player
 * owns — F1 — and this never touches it, so the HUD comes back by itself the moment the
 * editor stops asking.
 */
public final class HiddenHud {
    private static volatile boolean hidden;

    private HiddenHud() {
    }

    /** Asks for the HUD to be left undrawn, or stops asking. */
    public static void hide(boolean hide) {
        hidden = hide;
    }

    /** Read by the mixin, once per frame. */
    public static boolean hidden() {
        return hidden;
    }
}
