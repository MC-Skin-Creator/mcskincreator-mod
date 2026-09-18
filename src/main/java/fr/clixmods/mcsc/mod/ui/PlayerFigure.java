/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui;

import java.util.Arrays;

import fr.clixmods.mcsc.mod.skin.PreviewSkin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PlayerSkinWidget;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

/**
 * The game's own player, drawn in the scene.
 *
 * <p>Nothing here renders a figure: {@link PlayerSkinWidget} already draws a player,
 * already turns under the mouse, and already follows the classic or slim model of the
 * skin it is handed. The mod only supplies the skin, and this is the whole of what it
 * takes to hang that widget in the middle of an interface the mod paints itself.
 */
public final class PlayerFigure implements Figure {
    private final PreviewSkin preview;

    private PlayerSkinWidget widget;
    /** The bounds the current widget was built for, so it is only rebuilt when they move. */
    private int[] bounds = {0, 0, 0, 0};

    public PlayerFigure(PreviewSkin preview) {
        this.preview = preview;
    }

    @Override
    public void place(int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            this.widget = null;
            return;
        }

        // The widget carries the figure's rotation, and layout runs on every pick. Only
        // a change of size or position builds a new one, so stacking an element does not
        // quietly spin the player back to facing forward.
        int[] wanted = {x, y, width, height};
        if (this.widget == null || !Arrays.equals(this.bounds, wanted)) {
            Minecraft client = Minecraft.getInstance();
            this.widget = new PlayerSkinWidget(width, height,
                    client.getEntityModels(), this.preview::playerSkin);
            this.bounds = wanted;
        }
        this.widget.setX(x);
        this.widget.setY(y);
    }

    /** Throws the widget away, which is what puts the figure back facing forward. */
    @Override
    public void reset() {
        this.widget = null;
    }

    @Override
    public void draw(Canvas canvas, int mouseX, int mouseY, float delta) {
        if (this.widget != null) {
            canvas.widget(this.widget, mouseX, mouseY, delta);
        }
    }

    @Override
    public boolean press(double mouseX, double mouseY, int button) {
        return this.widget != null && button == 0
                && this.widget.mouseClicked(event(mouseX, mouseY, button), false);
    }

    @Override
    public void drag(double mouseX, double mouseY, double dragX, double dragY, int button) {
        if (this.widget != null) {
            this.widget.mouseDragged(event(mouseX, mouseY, button), dragX, dragY);
        }
    }

    @Override
    public void release(double mouseX, double mouseY, int button) {
        if (this.widget != null) {
            this.widget.mouseReleased(event(mouseX, mouseY, button));
        }
    }

    private static MouseButtonEvent event(double mouseX, double mouseY, int button) {
        return new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, 0));
    }
}
