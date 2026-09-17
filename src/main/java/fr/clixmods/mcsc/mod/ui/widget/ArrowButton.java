/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui.widget;

import fr.clixmods.mcsc.mod.style.Sprites;
import fr.clixmods.mcsc.mod.ui.Canvas;
import fr.clixmods.mcsc.mod.ui.Paint;
import net.minecraft.network.chat.Component;

/**
 * The button that folds a column away, and unfolds it again.
 *
 * <p>It is the game's page arrow — the one its own paged screens turn with — pointing
 * the way the panel would go. The site draws a chevron; typing one as a character
 * would leave its shape to whatever font is loaded, and drawing one would be inventing
 * an arrow next to the one the game already has.
 */
public class ArrowButton extends PixelButton {
    private final boolean pointsLeft;

    public ArrowButton(boolean pointsLeft, Runnable action) {
        super(Component.empty(), Style.GHOST, action);
        this.pointsLeft = pointsLeft;
        this.width = Sprites.ARROW_WIDTH;
        this.height = Sprites.ARROW_HEIGHT;
    }

    @Override
    public ArrowButton fit(Canvas canvas) {
        this.width = Sprites.ARROW_WIDTH;
        this.height = Sprites.ARROW_HEIGHT;
        return this;
    }

    @Override
    public void draw(Paint paint) {
        if (!visible()) {
            return;
        }
        boolean hot = paint.hot(this);
        paint.canvas().sprite(this.pointsLeft
                ? (hot ? Sprites.ARROW_LEFT_HOVERED : Sprites.ARROW_LEFT)
                : (hot ? Sprites.ARROW_RIGHT_HOVERED : Sprites.ARROW_RIGHT),
                this.x, this.y, this.width, this.height);
    }
}
