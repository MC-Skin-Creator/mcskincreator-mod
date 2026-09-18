/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui.widget;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import fr.clixmods.mcsc.mod.style.Metrics;
import fr.clixmods.mcsc.mod.style.Palette;
import fr.clixmods.mcsc.mod.style.Surface;
import fr.clixmods.mcsc.mod.ui.Canvas;
import fr.clixmods.mcsc.mod.ui.Element;
import fr.clixmods.mcsc.mod.ui.Paint;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * A settings slider: label on the left, carved rail, and the number on the right.
 *
 * <p>The number is right aligned in a fixed column so that the labels above and below
 * it line up and the value does not jump sideways as it changes — a value that moves
 * while you drag is a value you cannot read.
 *
 * <p>The effect is applied continuously as the handle moves, and the whole drag is
 * one entry in the history: the gesture is the edit, not each pixel of it.
 */
public class Slider extends Element {
    private final Component label;
    private final int minimum;
    private final int maximum;
    private final IntSupplier read;
    private final IntConsumer write;
    private final Runnable beginGesture;
    private final Runnable endGesture;
    private final java.util.function.IntFunction<Component> format;

    private boolean dragging;

    public Slider(Component label, int minimum, int maximum,
                  IntSupplier read, IntConsumer write,
                  java.util.function.IntFunction<Component> format,
                  Runnable beginGesture, Runnable endGesture) {
        this.label = label;
        this.minimum = minimum;
        this.maximum = maximum;
        this.read = read;
        this.write = write;
        this.format = format;
        this.beginGesture = beginGesture;
        this.endGesture = endGesture;
        this.height = Metrics.SLIDER_KNOB_HEIGHT;
    }

    @Override
    public void draw(Paint paint) {
        if (!visible()) {
            return;
        }
        Canvas canvas = paint.canvas();
        boolean hot = paint.hot(this);
        int value = this.read.getAsInt();

        int railX = railX();
        int railWidth = railWidth();
        int railY = this.y + (this.height - Metrics.SLIDER_HEIGHT) / 2;

        canvas.text(Component.literal(fr.clixmods.mcsc.mod.ui.Marquee.cut(
                        canvas, this.label.getString(), this.width / 3 - Metrics.PAD_TIGHT)),
                this.x, this.y + (this.height - canvas.lineHeight()) / 2,
                hot ? Palette.GOLD : Palette.INK_MUTED);

        Surface.slot(canvas, railX, railY, railWidth, Metrics.SLIDER_HEIGHT, Palette.FIELD);

        int travel = railWidth - Metrics.SLIDER_KNOB_WIDTH - Metrics.OUTLINE * 2;
        int knobX = railX + Metrics.OUTLINE + Math.round(travel * fraction(value));
        Surface.button(canvas, knobX, this.y, Metrics.SLIDER_KNOB_WIDTH, this.height,
                Surface.Tone.NEUTRAL, hot, false);

        Component shown = this.format.apply(value);
        canvas.text(shown,
                this.x + this.width - canvas.textWidth(shown),
                this.y + (this.height - canvas.lineHeight()) / 2,
                Palette.INK);
    }

    // Drawing and hit testing have to agree on where the rail is, to the pixel, so
    // both ask these two rather than each working it out for itself.
    private int railX() {
        return this.x + this.width / 3;
    }

    private int railWidth() {
        return this.width - this.width / 3 - Metrics.SLIDER_VALUE_WIDTH - Metrics.PAD_TIGHT;
    }

    private float fraction(int value) {
        return (value - this.minimum) / (float) (this.maximum - this.minimum);
    }

    @Override
    public boolean mouseDown(double mouseX, double mouseY, int button) {
        if (button != 0 || !contains(mouseX, mouseY)) {
            return false;
        }
        this.dragging = true;
        this.beginGesture.run();
        apply(mouseX);
        return true;
    }

    @Override
    public void mouseDrag(double mouseX, double mouseY, double dragX, double dragY, int button) {
        if (this.dragging) {
            apply(mouseX);
        }
    }

    @Override
    public void mouseUp(double mouseX, double mouseY, int button) {
        if (this.dragging) {
            this.dragging = false;
            this.endGesture.run();
        }
    }

    @Override
    public boolean keyDown(int key, int modifiers) {
        int step = switch (key) {
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_DOWN -> -1;
            case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_UP -> 1;
            default -> 0;
        };
        if (step == 0) {
            return false;
        }
        // One keystroke is one edit, so it opens and closes its own gesture.
        this.beginGesture.run();
        this.write.accept(clamp(this.read.getAsInt() + step));
        this.endGesture.run();
        return true;
    }

    private void apply(double mouseX) {
        int travel = Math.max(1, railWidth() - Metrics.SLIDER_KNOB_WIDTH - Metrics.OUTLINE * 2);
        double along = (mouseX - railX() - Metrics.OUTLINE - Metrics.SLIDER_KNOB_WIDTH / 2.0) / travel;
        this.write.accept(clamp(this.minimum + (int) Math.round(along * (this.maximum - this.minimum))));
    }

    private int clamp(int value) {
        return Math.max(this.minimum, Math.min(this.maximum, value));
    }
}
