/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui.widget;

import java.util.List;
import java.util.function.Consumer;

import fr.clixmods.mcsc.mod.style.Metrics;
import fr.clixmods.mcsc.mod.style.Palette;
import fr.clixmods.mcsc.mod.style.Surface;
import fr.clixmods.mcsc.mod.ui.Canvas;
import fr.clixmods.mcsc.mod.ui.Element;
import fr.clixmods.mcsc.mod.ui.Paint;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * A closed dropdown is a button with a pixel arrow; an open one is a menu of our own
 * making.
 *
 * <p>Never the system's menu. Minecraft has none to borrow, but the reason holds
 * anyway: a menu drawn by anything other than this interface would arrive with its
 * own font, its own corners and its own idea of a highlight, in the middle of a
 * screen that has spent every pixel agreeing on those three things.
 *
 * <p>It is fully navigable from the keyboard — arrows, Home, End, Enter, Escape — and
 * Escape closes the menu and stops there. Closing the window underneath as well is
 * the kind of thing that loses someone's work.
 *
 * @param <T> what the options stand for
 */
public class Dropdown<T> extends Element {
    /** The arrow, eight pixels across, drawn rather than typed. */
    private static final int ARROW = 8;

    private final List<T> options;
    private final java.util.function.Function<T, Component> naming;
    private final java.util.function.Supplier<T> read;
    private final Consumer<T> write;
    private final java.util.function.Predicate<T> available;

    private boolean open;
    private int highlighted;
    private int screenHeight = Integer.MAX_VALUE;

    public Dropdown(List<T> options,
                    java.util.function.Function<T, Component> naming,
                    java.util.function.Supplier<T> read,
                    Consumer<T> write,
                    java.util.function.Predicate<T> available) {
        this.options = options;
        this.naming = naming;
        this.read = read;
        this.write = write;
        this.available = available;
        this.height = Metrics.BUTTON_HEIGHT_COMPACT;
    }

    private int rowHeight(Canvas canvas) {
        return canvas.lineHeight() + Metrics.BUTTON_PAD_Y * 2;
    }

    @Override
    public void draw(Paint paint) {
        if (!visible()) {
            return;
        }
        Canvas canvas = paint.canvas();
        boolean hot = paint.hot(this) || this.open;
        Surface.button(canvas, this.x, this.y, this.width, this.height,
                Surface.Tone.NEUTRAL, hot, false);

        int inset = Metrics.OUTLINE + Metrics.BUTTON_PAD_X;
        int room = this.width - inset - ARROW - Metrics.PAD_TIGHT - Metrics.OUTLINE;
        Component current = this.naming.apply(this.read.get());
        canvas.text(Component.literal(fr.clixmods.mcsc.mod.ui.Marquee.cut(
                        canvas, current.getString(), room)),
                this.x + inset, this.y + (this.height - canvas.lineHeight()) / 2,
                hot ? Palette.GOLD : Palette.INK);

        drawArrow(canvas, this.x + this.width - Metrics.OUTLINE - Metrics.PAD_TIGHT - ARROW,
                this.y + (this.height - ARROW / 2) / 2, hot ? Palette.GOLD : Palette.INK);
    }

    /** A triangle made of rows of pixels, so it stays square at any GUI scale. */
    private static void drawArrow(Canvas canvas, int x, int y, int color) {
        for (int row = 0; row < ARROW / 2; row++) {
            canvas.fill(x + row, y + row, ARROW - row * 2, 1, color);
        }
    }

    @Override
    public boolean overlayActive() {
        return this.open;
    }

    @Override
    public void drawOverlay(Paint paint) {
        if (!this.open) {
            return;
        }
        Canvas canvas = paint.canvas();
        int row = rowHeight(canvas);
        int top = menuTop(canvas);
        int height = this.options.size() * row + Metrics.OUTLINE * 2;

        // A hard four pixel shadow, offset down and right. No blur: there is nothing
        // soft anywhere in this interface.
        canvas.fill(this.x + Metrics.ui(6), top + Metrics.ui(6), this.width, height,
                Palette.withAlpha(Palette.OUTLINE, 140));
        Surface.panel(canvas, this.x, top, this.width, height);

        for (int index = 0; index < this.options.size(); index++) {
            T option = this.options.get(index);
            int rowY = top + Metrics.OUTLINE + index * row;
            boolean usable = this.available.test(option);
            boolean current = option.equals(this.read.get());
            boolean lit = usable && (index == this.highlighted
                    || paint.over(this.x, rowY, this.width, row));

            if (current) {
                canvas.fill(this.x + Metrics.OUTLINE, rowY,
                        this.width - Metrics.OUTLINE * 2, row, Palette.GREEN);
            } else if (lit) {
                canvas.fill(this.x + Metrics.OUTLINE, rowY,
                        this.width - Metrics.OUTLINE * 2, row, Palette.PANEL_SUB);
            }

            int ink = !usable ? Palette.INK_FAINT : lit && !current ? Palette.GOLD : Palette.INK;
            canvas.text(this.naming.apply(option),
                    this.x + Metrics.OUTLINE + Metrics.BUTTON_PAD_X,
                    rowY + Metrics.BUTTON_PAD_Y, ink);
        }
    }

    /** Told by the screen, so the menu knows when it would fall off the bottom. */
    public Dropdown<T> inScreen(int screenHeight) {
        this.screenHeight = screenHeight;
        return this;
    }

    /**
     * The menu hangs below the button, unless there is no room below — in which case
     * it hangs above it rather than off the bottom of the screen.
     */
    private int menuTop(Canvas canvas) {
        int height = this.options.size() * rowHeight(canvas) + Metrics.OUTLINE * 2;
        int below = this.y + this.height;
        return below + height > this.screenHeight ? this.y - height : below;
    }

    @Override
    public boolean overlayContains(double mouseX, double mouseY) {
        // While the menu is open every click belongs to it: one either chooses a row
        // or dismisses the menu, and neither reaches what is behind it.
        return this.open;
    }

    @Override
    public void closeOverlay() {
        this.open = false;
    }

    @Override
    public boolean mouseDown(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        if (this.open) {
            return true;
        }
        if (!contains(mouseX, mouseY)) {
            return false;
        }
        this.open = true;
        this.highlighted = Math.max(0, this.options.indexOf(this.read.get()));
        return true;
    }

    /** Called by the screen with the row height it drew the menu at. */
    public boolean overlayMouseDown(double mouseX, double mouseY, Canvas canvas) {
        if (!this.open) {
            return false;
        }
        int row = rowHeight(canvas);
        int top = menuTop(canvas) + Metrics.OUTLINE;
        int index = (int) ((mouseY - top) / row);
        if (mouseX >= this.x && mouseX < this.x + this.width
                && index >= 0 && index < this.options.size()) {
            T option = this.options.get(index);
            if (this.available.test(option)) {
                this.write.accept(option);
                this.open = false;
            }
            return true;
        }
        // A click anywhere else closes the menu and is swallowed, so the first click
        // outside dismisses instead of also pressing whatever it landed on.
        this.open = false;
        return true;
    }

    @Override
    public boolean keyDown(int key, int modifiers) {
        if (!this.open) {
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_SPACE) {
                return activate();
            }
            return false;
        }
        switch (key) {
            case GLFW.GLFW_KEY_ESCAPE -> {
                this.open = false;
                return true;
            }
            case GLFW.GLFW_KEY_UP -> {
                this.highlighted = step(-1);
                return true;
            }
            case GLFW.GLFW_KEY_DOWN -> {
                this.highlighted = step(1);
                return true;
            }
            case GLFW.GLFW_KEY_HOME -> {
                this.highlighted = 0;
                return true;
            }
            case GLFW.GLFW_KEY_END -> {
                this.highlighted = this.options.size() - 1;
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_SPACE -> {
                T option = this.options.get(this.highlighted);
                if (this.available.test(option)) {
                    this.write.accept(option);
                    this.open = false;
                }
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    /** Steps over options that cannot be chosen rather than stopping on them. */
    private int step(int direction) {
        int index = this.highlighted;
        for (int tries = 0; tries < this.options.size(); tries++) {
            index = Math.floorMod(index + direction, this.options.size());
            if (this.available.test(this.options.get(index))) {
                return index;
            }
        }
        return this.highlighted;
    }

    @Override
    public boolean activate() {
        this.open = !this.open;
        if (this.open) {
            this.highlighted = Math.max(0, this.options.indexOf(this.read.get()));
        }
        return true;
    }
}
