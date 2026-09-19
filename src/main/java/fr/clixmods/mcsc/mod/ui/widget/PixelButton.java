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
import fr.clixmods.mcsc.mod.style.Surface;
import fr.clixmods.mcsc.mod.ui.Canvas;
import fr.clixmods.mcsc.mod.ui.Element;
import fr.clixmods.mcsc.mod.ui.Paint;
import net.minecraft.network.chat.Component;

/**
 * Every action in the editor is one of these, drawn with the game's button sprite.
 *
 * <p>Four of the five styles are the same material in a different tone: an ordinary
 * action, the one action a bar or a footer is really about, the one that removes
 * something, and the one of a group that is chosen. Green and red are the site's, and
 * they are the whole of how this interface says "this one" — a screen where every
 * control is the same grey says nothing at all.
 *
 * <p>A ghost button is the exception and it is not a material: it is a label that
 * lights up, for a control that displays rather than acts.
 */
public class PixelButton extends Element {
    /** What the button is for, which is also how the game draws it. */
    public enum Style {
        /** An ordinary action. */
        NORMAL,
        /** The one action a bar or a window's footer is really about. Green. */
        PRIMARY,
        /** Removes something. Red. */
        DANGER,
        /** One of a group: the chosen one is green. */
        TAB,
        /** Not an action but a display control. Ink only, no material. */
        GHOST,
        /** The close cross of a window. */
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
        this.height = style == Style.CROSS ? Metrics.CROSS : Metrics.BUTTON_HEIGHT;
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

    /** The room the frame and the label's air take from the width. */
    private int padding() {
        return switch (this.style) {
            case GHOST -> Metrics.PAD_TIGHT * 2;
            // A row of tabs has to fit a column, so they take the frame and little else.
            case TAB -> (Metrics.BUTTON_INSET + Metrics.PAD_TIGHT) * 2;
            default -> (Metrics.BUTTON_INSET + Metrics.BUTTON_PAD_X) * 2;
        };
    }

    /** Measures against the real font, once the screen has one. */
    public PixelButton fit(Canvas canvas) {
        if (this.style == Style.CROSS) {
            this.width = Metrics.CROSS;
            this.height = Metrics.CROSS;
            return this;
        }
        this.width = canvas.textWidth(this.label) + padding();
        return this;
    }

    /** Measures, then gives up whatever will not fit. */
    public PixelButton fitWithin(Canvas canvas, int available) {
        fit(canvas);
        this.width = Math.min(this.width, Math.max(padding(), available));
        return this;
    }

    @Override
    public void draw(Paint paint) {
        if (!visible()) {
            return;
        }
        Canvas canvas = paint.canvas();
        boolean hot = paint.hot(this);

        boolean down = this.held && enabled();
        switch (this.style) {
            case CROSS -> {
                Surface.button(canvas, this.x, this.y, Metrics.CROSS, Metrics.CROSS,
                        hot ? Surface.Tone.RED : Surface.Tone.NEUTRAL, hot, down);
                canvas.textCentered(Component.literal("x"), this.x + Metrics.CROSS / 2,
                        this.y + (Metrics.CROSS - canvas.lineHeight()) / 2 + 1, Palette.INK);
                return;
            }
            case TAB -> Surface.tab(canvas, this.x, this.y, this.width, this.height,
                    this.active, hot);
            case GHOST -> {
                // Nothing: a ghost control has no material, only ink.
            }
            default -> Surface.button(canvas, this.x, this.y, this.width, this.height,
                    tone(), hot && enabled(), down);
        }

        // Cut to the button rather than drawn past it: a label that overflows its own
        // frame is the one thing a caller cannot see coming from the layout.
        int room = this.width - padding();
        // Pressing drops the content one pixel, which with the inverted bevel is the
        // whole of the press feedback.
        canvas.textCentered(Component.literal(
                        fr.clixmods.mcsc.mod.ui.Marquee.cut(canvas, this.label.getString(), room)),
                this.x + this.width / 2,
                this.y + (this.height - canvas.lineHeight()) / 2 + (down ? 1 : 0), ink(hot));
    }

    private Surface.Tone tone() {
        return switch (this.style) {
            case PRIMARY -> Surface.Tone.GREEN;
            case DANGER -> Surface.Tone.RED;
            default -> Surface.Tone.NEUTRAL;
        };
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

    /** True while the button is held down, so the caller can match the bevel. */
    public boolean pressed() {
        return this.held;
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
