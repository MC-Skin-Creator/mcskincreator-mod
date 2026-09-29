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
 * <p>Minecraft has no dropdown to borrow, so this is assembled out of the parts it
 * does have: the button sprite when closed, the page arrow for the marker, and the
 * panel the game puts behind its own pop-ups for the open menu. Nothing is drawn that
 * the game does not already draw somewhere.
 *
 * <p>It is fully navigable from the keyboard — arrows, Home, End, Enter, Escape — and
 * Escape closes the menu and stops there. Closing the window underneath as well is
 * the kind of thing that loses someone's work.
 *
 * <p>An option may be null, which is how a list offers "any of them": the naming
 * function is asked what to call it like any other.
 *
 * @param <T> what the options stand for
 */
public class Dropdown<T> extends Element {
    /** {@code select { padding-right: 22px }} — the room the caret is given. */
    private static final int ARROW = Metrics.CARET_ROOM;

    private final List<T> options;
    private final java.util.function.Function<T, Component> naming;
    private final java.util.function.Supplier<T> read;
    private final Consumer<T> write;
    private final java.util.function.Predicate<T> available;

    private Component label;
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

    /**
     * Sizes the control to its label and its longest option, so the value never slides
     * under the caret as it changes.
     */
    public Dropdown<T> fit(Canvas canvas) {
        int widest = 0;
        for (T option : this.options) {
            widest = Math.max(widest, canvas.textWidth(this.naming.apply(option)));
        }
        int labelWidth = this.label == null ? 0
                : canvas.textWidth(this.label) + Metrics.PAD_TIGHT;
        this.width = (Metrics.BUTTON_INSET + Metrics.PAD_HAIR) * 2
                + labelWidth + widest + ARROW;
        return this;
    }

    private int rowHeight(Canvas canvas) {
        return canvas.lineHeight() + Metrics.PAD_TIGHT * 2;
    }

    @Override
    public void draw(Paint paint) {
        if (!visible()) {
            return;
        }
        Canvas canvas = paint.canvas();
        boolean hot = paint.hot(this) || this.open;
        Surface.button(canvas, this.x, this.y, this.width, this.height,
                hot ? Surface.State.HOVERED : Surface.State.NORMAL);

        int inset = Metrics.BUTTON_INSET + Metrics.PAD_HAIR;
        int cursorX = this.x + inset;
        int room = this.width - inset - ARROW;
        int textY = this.y + (this.height - canvas.lineHeight()) / 2;
        if (this.label != null) {
            // Quieter than the value: what the control decides is context, what it holds
            // is the answer, and the answer is what the eye should land on.
            int labelWidth = canvas.textWidth(this.label);
            if (labelWidth + Metrics.PAD_TIGHT < room) {
                canvas.text(this.label, cursorX, textY, Palette.INK_FAINT);
                cursorX += labelWidth + Metrics.PAD_TIGHT;
                room -= labelWidth + Metrics.PAD_TIGHT;
            }
        }
        Component current = this.naming.apply(this.read.get());
        canvas.text(Component.literal(fr.clixmods.mcsc.mod.ui.Marquee.cut(
                        canvas, current.getString(), room)),
                cursorX, textY, hot ? Palette.INK_HOVERED : Palette.INK);

        Surface.caret(canvas,
                this.x + this.width - Metrics.BUTTON_INSET - Metrics.PAD_HAIR
                        - (ARROW + Metrics.CARET_WIDTH) / 2,
                this.y + (this.height - Metrics.CARET_HEIGHT) / 2,
                hot ? Palette.INK_HOVERED : Palette.INK);
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
        int inset = Metrics.PANEL_INSET;
        int height = this.options.size() * row + inset * 2;

        // A hard shadow, offset down and right. No blur: the game has none anywhere.
        canvas.fill(this.x + Metrics.PAD_TIGHT, top + Metrics.PAD_TIGHT,
                this.width, height, Palette.SHADOW);
        Surface.panel(canvas, this.x, top, this.width, height);

        for (int index = 0; index < this.options.size(); index++) {
            T option = this.options.get(index);
            int rowY = top + inset + index * row;
            boolean usable = this.available.test(option);
            boolean current = java.util.Objects.equals(option, this.read.get());
            boolean lit = usable && (index == this.highlighted
                    || paint.over(this.x, rowY, this.width, row));

            // The chosen row is the game's selected tab; a row under the pointer takes
            // the plain one. Neither needs a colour the game does not use.
            if (current || lit) {
                Surface.tab(canvas, this.x + inset, rowY,
                        this.width - inset * 2, row, current, lit);
            }

            int ink = !usable ? Palette.INK_FAINT : lit ? Palette.INK_HOVERED : Palette.INK;
            canvas.text(this.naming.apply(option), this.x + inset + Metrics.PAD_TIGHT,
                    rowY + (row - canvas.lineHeight()) / 2, ink);
        }
    }

    /**
     * Names what the dropdown is for, shown in front of the value it holds.
     *
     * <p>For a control that stands on its own rather than under a panel's title. A
     * dropdown reading "3D" is perfectly clear about what it says and silent about what
     * it decides; "View: 3D" answers both, and answers the second one before it is
     * opened. The punctuation after the name belongs to the translation, because where
     * a colon goes and whether it takes a space before it is a question about a
     * language and not about a control.
     */
    public Dropdown<T> withLabel(Component label) {
        this.label = label;
        return this;
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
        int height = this.options.size() * rowHeight(canvas) + Metrics.PANEL_INSET * 2;
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
    public boolean clickSound() {
        return true;
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
        int top = menuTop(canvas) + Metrics.PANEL_INSET;
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
