/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui;

import fr.clixmods.mcsc.mod.scene.PosedPlayer;
import fr.clixmods.mcsc.mod.scene.SceneShot;
import fr.clixmods.mcsc.mod.skin.PreviewSkin;

/**
 * The game's own player, drawn in the scene.
 *
 * <p>Nothing here renders a figure: the game does, through the same picture-in-picture
 * path its inventory portrait goes through. The mod fills in a render state — a skin and
 * a situation — and hands it over; what comes back has the game's overlay layer, the
 * game's animation and the right proportions for a classic or a slim skin.
 *
 * <p>It used to be vanilla's {@code PlayerSkinWidget}, which turns under the mouse and
 * does nothing else. The scene wanted a zoom, a pan and a pose, and none of the three is
 * something that widget can be asked for.
 *
 * <p>Two of the three cameras draw nothing here at all: they let the game draw the real
 * character, in the real world, and this figure's whole job is then to keep out of the
 * way.
 */
public final class PlayerFigure implements Figure {
    private final PreviewSkin preview;

    private int x;
    private int y;
    private int width;
    private int height;

    public PlayerFigure(PreviewSkin preview) {
        this.preview = preview;
    }

    @Override
    public void place(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public void draw(Canvas canvas, SceneShot shot, int mouseX, int mouseY, float delta) {
        if (shot.drawnByTheGame() || this.width <= 0 || this.height <= 0) {
            return;
        }
        PosedPlayer.draw(canvas, this.preview.playerSkin(), shot.pose(), shot.playing(),
                shot.seconds(), shot.eye(), this.x, this.y, this.width, this.height);
    }
}
