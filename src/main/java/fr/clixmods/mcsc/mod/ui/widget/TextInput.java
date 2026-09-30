/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui.widget;

import java.util.function.Consumer;

import com.mojang.blaze3d.platform.InputConstants;

import fr.clixmods.mcsc.mod.style.Metrics;
import fr.clixmods.mcsc.mod.style.Palette;
import fr.clixmods.mcsc.mod.style.Surface;
import fr.clixmods.mcsc.mod.ui.Canvas;
import fr.clixmods.mcsc.mod.ui.Element;
import fr.clixmods.mcsc.mod.ui.Paint;
import net.minecraft.network.chat.Component;

/**
 * A text field.
 *
 * <p>The game's own field sprite, which already tells a box you can type into from a
 * slot you cannot, and already has a lit variant for the one holding the cursor. The
 * site draws its own and turns the border gold; there is nothing to gain by copying
 * that when the player already knows this one.
 *
 * <p>While it has the focus it reports {@link #capturesTyping()}, and the screen
 * holds back every shortcut. Someone typing "band" into the search box is not asking
 * for the bucket tool.
 */
public class TextInput extends Element {
    private final Component placeholder;
    private final Consumer<String> onChange;
    private final int limit;

    private String value = "";
    private int cursor;
    private boolean focused;

    public TextInput(Component placeholder, int limit, Consumer<String> onChange) {
        this.placeholder = placeholder;
        this.limit = limit;
        this.onChange = onChange;
        this.height = Metrics.BUTTON_HEIGHT_COMPACT;
    }

    public String value() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value.length() > this.limit ? value.substring(0, this.limit) : value;
        this.cursor = this.value.length();
        this.onChange.accept(this.value);
    }

    /** Clears without telling anyone — for when the caller is about to act anyway. */
    public void clearQuietly() {
        this.value = "";
        this.cursor = 0;
    }

    @Override
    public void blur() {
        this.focused = false;
    }

    public boolean isEmpty() {
        return this.value.isEmpty();
    }

    /** Puts the cursor at the end and selects nothing — a name box opens ready to type. */
    public void takeFocus() {
        this.focused = true;
        this.cursor = this.value.length();
    }

    @Override
    public void draw(Paint paint) {
        if (!visible()) {
            return;
        }
        Canvas canvas = paint.canvas();
        boolean lit = this.focused || paint.focused() == this;
        Surface.field(canvas, this.x, this.y, this.width, this.height, lit);

        int textX = this.x + Metrics.FIELD_INSET + Metrics.PAD_TIGHT;
        int textY = this.y + (this.height - canvas.lineHeight()) / 2;
        int room = this.width - (textX - this.x) - Metrics.PAD_TIGHT - Metrics.FIELD_INSET;

        if (this.value.isEmpty() && !lit) {
            // Placeholder text carries no shadow: it is not content, and a shadow would
            // make it look like content that happens to be grey.
            canvas.textFlat(this.placeholder, textX, textY, Palette.INK_FAINT);
        } else {
            String shown = canvas.trimToWidth(this.value, room);
            canvas.text(Component.literal(shown), textX, textY, Palette.INK);
            if (this.focused && (paint.time() / 500L) % 2L == 0L) {
                int caret = textX + canvas.textWidth(shown.substring(0, Math.min(this.cursor, shown.length())));
                canvas.fill(caret, textY - 1, 1, canvas.lineHeight() + 1, Palette.INK);
            }
        }
    }

    @Override
    public boolean mouseDown(double mouseX, double mouseY, int button) {
        if (!contains(mouseX, mouseY)) {
            this.focused = false;
            return false;
        }
        this.focused = true;
        this.cursor = this.value.length();
        return true;
    }

    @Override
    public boolean capturesTyping() {
        return this.focused;
    }

    @Override
    public boolean charTyped(int codepoint) {
        if (!this.focused || this.value.length() >= this.limit || Character.isISOControl(codepoint)) {
            return false;
        }
        this.value = this.value.substring(0, this.cursor)
                + Character.toString(codepoint)
                + this.value.substring(this.cursor);
        this.cursor++;
        this.onChange.accept(this.value);
        return true;
    }

    @Override
    public boolean keyDown(int key, int modifiers) {
        if (!this.focused) {
            return false;
        }
        switch (key) {
            case InputConstants.KEY_BACKSPACE -> {
                if (this.cursor > 0) {
                    this.value = this.value.substring(0, this.cursor - 1) + this.value.substring(this.cursor);
                    this.cursor--;
                    this.onChange.accept(this.value);
                }
                return true;
            }
            case InputConstants.KEY_DELETE -> {
                if (this.cursor < this.value.length()) {
                    this.value = this.value.substring(0, this.cursor) + this.value.substring(this.cursor + 1);
                    this.onChange.accept(this.value);
                }
                return true;
            }
            case InputConstants.KEY_LEFT -> {
                this.cursor = Math.max(0, this.cursor - 1);
                return true;
            }
            case InputConstants.KEY_RIGHT -> {
                this.cursor = Math.min(this.value.length(), this.cursor + 1);
                return true;
            }
            case InputConstants.KEY_HOME -> {
                this.cursor = 0;
                return true;
            }
            case InputConstants.KEY_END -> {
                this.cursor = this.value.length();
                return true;
            }
            default -> {
                return false;
            }
        }
    }
}
