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
 * A settings slider: what it is and what it reads on one line, the rail under it.
 *
 * <p>Two lines rather than one, because one does not fit. A side column is some 120
 * pixels wide; taking a third of that for the label and a fixed column for the value
 * left a rail of forty pixels between two words that were themselves cut short —
 * "Satur…" beside a number half under the panel's frame. Stacked, the label is whole,
 * the value is whole, and the rail is as long as the panel is wide.
 *
 * <p>The number is still right aligned, so the values of four settings line up and
 * none of them jumps sideways as it changes: a value that moves while you drag is a
 * value you cannot read.
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
        this.height = Metrics.SLIDER_RAIL;
    }

    /** How tall a slider is: its line of text, and the rail under it. */
    public static int heightFor(Canvas canvas) {
        return canvas.smallLineHeight() + Metrics.PAD_TIGHT + Metrics.SLIDER_RAIL;
    }

    @Override
    public void draw(Paint paint) {
        if (!visible()) {
            return;
        }
        Canvas canvas = paint.canvas();
        boolean hot = paint.hot(this);
        int value = this.read.getAsInt();

        // Label and value at the half size: this is a row you read while your hand is
        // already on the handle, and at full size the two words above a slider were the
        // loudest thing in the column.
        Component shown = this.format.apply(value);
        int valueWidth = canvas.smallTextWidth(shown);
        int room = Math.max(0, this.width - valueWidth - Metrics.PAD_TIGHT);
        canvas.textSmall(Component.literal(fr.clixmods.mcsc.mod.ui.Marquee.cutSmall(
                        canvas, this.label.getString(), room)),
                this.x, this.y,
                hot ? Palette.INK_HOVERED : Palette.INK_MUTED);
        canvas.textSmall(shown, this.x + this.width - valueWidth, this.y, Palette.INK);

        int railY = railY(canvas);
        int railWidth = railWidth();
        Surface.sliderRail(canvas, this.x, railY, railWidth, Metrics.SLIDER_RAIL, hot);

        int travel = railWidth - Metrics.SLIDER_HANDLE;
        int handleX = this.x + Math.round(travel * fraction(value));
        Surface.sliderHandle(canvas, handleX, railY, Metrics.SLIDER_RAIL, hot);
    }

    // Drawing and hit testing have to agree on where the rail is, to the pixel, so
    // both ask these rather than each working it out for itself.
    private int railY(Canvas canvas) {
        return this.y + this.height - Metrics.SLIDER_RAIL;
    }

    private int railWidth() {
        return this.width;
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
        int travel = Math.max(1, railWidth() - Metrics.SLIDER_HANDLE);
        double along = (mouseX - this.x - Metrics.SLIDER_HANDLE / 2.0) / travel;
        this.write.accept(clamp(this.minimum + (int) Math.round(along * (this.maximum - this.minimum))));
    }

    private int clamp(int value) {
        return Math.max(this.minimum, Math.min(this.maximum, value));
    }
}
