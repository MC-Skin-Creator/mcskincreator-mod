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
import fr.clixmods.mcsc.mod.ui.Icons;
import fr.clixmods.mcsc.mod.ui.Paint;
import net.minecraft.network.chat.Component;

/**
 * Every action in the interface is one of these.
 *
 * <p>The variants are the site's, and they mean different things rather than looking
 * different: green is the primary action and there is one per bar and one per window
 * footer; red destroys; a ghost button is not an action at all but a display control
 * — fold a panel, recentre the camera — so it has no material, only ink.
 *
 * <p>Pressing inverts the bevel and drops the content by a pixel. That is the whole
 * feedback: no animation, on the site or here.
 */
public class PixelButton extends Element {
    /** What the button is for, which is also what it is made of. */
    public enum Style {
        /** An ordinary action. */
        NORMAL,
        /** The one action a bar or a footer is really about. Green. */
        PRIMARY,
        /** Removes something. Red. */
        DANGER,
        /** Not an action: a display control. Ink only, no material. */
        GHOST,
        /** An icon on its own, tighter, with the name in the tooltip. */
        ICON
    }

    private final Component label;
    private final String icon;
    private final Runnable action;
    private final Style style;

    private List<Component> tooltip = List.of();
    private boolean active;
    private boolean held;

    public PixelButton(Component label, Style style, Runnable action) {
        this(label, null, style, action);
    }

    public static PixelButton icon(String icon, Runnable action) {
        return new PixelButton(null, icon, Style.ICON, action);
    }

    public PixelButton(Component label, String icon, Style style, Runnable action) {
        this.label = label;
        this.icon = icon;
        this.style = style;
        this.action = action;
        this.height = style == Style.ICON ? Metrics.BUTTON_HEIGHT_COMPACT : Metrics.BUTTON_HEIGHT;
        this.width = measure();
    }

    /** What the button says it does — never a restatement of its own label. */
    public PixelButton withTooltip(Component text) {
        this.tooltip = List.of(text);
        return this;
    }

    /**
     * Marks the button as the chosen one of a group. An active button is green and
     * bold, which is how a segmented group and a region tab show their choice.
     */
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

    /** The width the button would like: its content plus padding and outline. */
    public final int measure() {
        int padding = (this.style == Style.ICON ? Metrics.ui(8) : Metrics.BUTTON_PAD_X) * 2;
        if (this.icon != null && this.label == null) {
            return Icons.SIZE + padding;
        }
        // Measured without a font, so this is the fallback the screen refines in init().
        int text = this.label == null ? 0 : this.label.getString().length() * 6;
        return text + padding + Metrics.OUTLINE * 2;
    }

    /** Re-measures against the real font, once the screen has one. */
    public PixelButton fit(Canvas canvas) {
        int padding = (this.style == Style.ICON ? Metrics.ui(8) : Metrics.BUTTON_PAD_X) * 2;
        int content = this.label != null ? canvas.textWidth(this.label) : Icons.SIZE;
        this.width = content + padding + Metrics.OUTLINE * 2;
        return this;
    }

    @Override
    public void draw(Paint paint) {
        if (!visible()) {
            return;
        }
        Canvas canvas = paint.canvas();
        boolean hot = paint.hot(this);
        boolean pressed = this.held && hot;

        if (this.style != Style.GHOST) {
            Surface.button(canvas, this.x, this.y, this.width, this.height, tone(), hot, pressed);
        }

        int ink = ink(hot);
        int offset = pressed ? Metrics.PRESS_OFFSET : 0;
        int centerX = this.x + this.width / 2;
        int textY = this.y + (this.height - canvas.lineHeight()) / 2 + offset;

        if (this.icon != null && this.label == null) {
            Icons.draw(canvas, this.icon,
                    centerX - Icons.SIZE / 2, this.y + (this.height - Icons.SIZE) / 2 + offset, 1);
        } else if (this.label != null) {
            canvas.textCentered(this.label, centerX, textY, ink);
        }

        if (!enabled()) {
            // Forty percent opacity, the site's way of saying "not now". Laid over the
            // finished button in the panel's own colour rather than mixed into every
            // shade, so a disabled green button still reads as the green one.
            canvas.fill(this.x, this.y, this.width, this.height, Palette.withAlpha(Palette.PANEL, 153));
        }
    }

    private Surface.Tone tone() {
        if (this.active || this.style == Style.PRIMARY) {
            return Surface.Tone.GREEN;
        }
        return this.style == Style.DANGER ? Surface.Tone.RED : Surface.Tone.NEUTRAL;
    }

    private int ink(boolean hot) {
        if (this.style == Style.GHOST) {
            return hot ? Palette.GOLD : Palette.INK_DIM;
        }
        return tone().ink(hot);
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
        if (this.held && contains(mouseX, mouseY)) {
            this.held = false;
            activate();
            return;
        }
        this.held = false;
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
