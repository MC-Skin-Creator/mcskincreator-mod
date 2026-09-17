/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui.widget;

import java.util.List;

import fr.clixmods.mcsc.mod.style.Metrics;
import fr.clixmods.mcsc.mod.style.Palette;
import fr.clixmods.mcsc.mod.style.Sprites;
import fr.clixmods.mcsc.mod.style.Surface;
import fr.clixmods.mcsc.mod.ui.Canvas;
import fr.clixmods.mcsc.mod.ui.Element;
import fr.clixmods.mcsc.mod.ui.Paint;
import net.minecraft.network.chat.Component;

/**
 * Every action in the editor is one of these, drawn with the game's button sprite.
 *
 * <p>The site distinguishes a primary action by painting it green and a destructive
 * one red. Neither colour means anything in a Minecraft menu, and inventing a green
 * button would be inventing a widget the game does not have — so the distinction is
 * carried where the game already carries it: the one choice of a group is a selected
 * tab, and everything else is a button.
 *
 * <p>A ghost button is the exception, and it is not a material: it is a label that
 * lights up, which is what the game does for the links on its own screens.
 */
public class PixelButton extends Element {
    /** What the button is for, which is also how the game draws it. */
    public enum Style {
        /** An ordinary action. The game's button. */
        NORMAL,
        /** One of a group, drawn as a tab so the chosen one reads as chosen. */
        TAB,
        /** Not an action but a display control. Ink only, no material. */
        GHOST,
        /** The close cross of a window, at the size the game fixes for it. */
        CROSS
    }

    private final Component label;
    private final Runnable action;
    private final Style style;

    private List<Component> tooltip = List.of();
    private boolean active;
    private boolean held;

    public PixelButton(Component label, Style style, Runnable action) {
        this.label = label;
        this.style = style;
        this.action = action;
        this.height = style == Style.CROSS ? Sprites.CROSS_SIZE : Metrics.BUTTON_HEIGHT;
        this.width = this.height;
    }

    /** What the button says it does — never a restatement of its own label. */
    @SuppressWarnings("unchecked")
    public <T extends PixelButton> T withTooltip(Component text) {
        this.tooltip = List.of(text);
        return (T) this;
    }

    /** Marks the button as the chosen one of a group. */
    public PixelButton setActive(boolean active) {
        this.active = active;
        return this;
    }

    public boolean active() {
        return this.active;
    }

    public Component label() {
        return this.label;
    }

    /** Measures against the real font, once the screen has one. */
    public PixelButton fit(Canvas canvas) {
        if (this.style == Style.CROSS) {
            this.width = Sprites.CROSS_SIZE;
            this.height = Sprites.CROSS_SIZE;
            return this;
        }
        int padding = this.style == Style.GHOST
                ? Metrics.PAD_TIGHT * 2
                : (Metrics.BUTTON_INSET + Metrics.BUTTON_PAD_X) * 2;
        this.width = canvas.textWidth(this.label) + padding;
        return this;
    }

    @Override
    public void draw(Paint paint) {
        if (!visible()) {
            return;
        }
        Canvas canvas = paint.canvas();
        boolean hot = paint.hot(this);

        switch (this.style) {
            case CROSS -> {
                canvas.sprite(hot ? Sprites.CROSS_HOVERED : Sprites.CROSS,
                        this.x, this.y, Sprites.CROSS_SIZE, Sprites.CROSS_SIZE);
                return;
            }
            case TAB -> Surface.tab(canvas, this.x, this.y, this.width, this.height, this.active, hot);
            case NORMAL -> Surface.button(canvas, this.x, this.y, this.width, this.height,
                    !enabled() ? Surface.State.DISABLED
                            : hot ? Surface.State.HOVERED : Surface.State.NORMAL);
            case GHOST -> {
                // Nothing: a ghost control has no material, only ink.
            }
        }

        canvas.textCentered(this.label, this.x + this.width / 2,
                this.y + (this.height - canvas.lineHeight()) / 2, ink(hot));
    }

    private int ink(boolean hot) {
        if (!enabled()) {
            return Palette.INK_DISABLED;
        }
        if (this.style == Style.GHOST) {
            return hot ? Palette.INK_HOVERED : Palette.INK_MUTED;
        }
        return hot ? Palette.INK_HOVERED : Palette.INK;
    }

    @Override
    public boolean mouseDown(double mouseX, double mouseY, int button) {
        if (button != 0 || !contains(mouseX, mouseY)) {
            return false;
        }
        this.held = true;
        return true;
    }

    @Override
    public void mouseUp(double mouseX, double mouseY, int button) {
        boolean wasHeld = this.held;
        this.held = false;
        if (wasHeld && contains(mouseX, mouseY)) {
            activate();
        }
    }

    @Override
    public boolean activate() {
        if (!enabled()) {
            return false;
        }
        this.action.run();
        return true;
    }

    @Override
    public List<Component> tooltip() {
        return this.tooltip;
    }
}
