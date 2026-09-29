/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui.window;

import java.util.List;
import java.util.function.Consumer;

import fr.clixmods.mcsc.mod.style.Metrics;
import fr.clixmods.mcsc.mod.style.Palette;
import fr.clixmods.mcsc.mod.ui.Canvas;
import fr.clixmods.mcsc.mod.ui.Paint;
import fr.clixmods.mcsc.mod.ui.Prose;
import fr.clixmods.mcsc.mod.ui.widget.PixelButton;
import fr.clixmods.mcsc.mod.ui.widget.TextInput;
import net.minecraft.network.chat.Component;

/**
 * Asking for a name.
 *
 * <p>The field takes the focus as the window opens, Enter accepts, and Cancel gives
 * back whichever window asked. The accepting action is the last button rather than a
 * coloured one, because that is where the game puts "Done" and a Minecraft menu has no
 * primary button to borrow.
 */
public class NameWindow extends ModalWindow {
    private final TextInput field;
    private final Consumer<String> onAccept;
    private final Runnable onCancel;
    private final Component hint;

    public NameWindow(String titleKey, String initial, Consumer<String> onAccept,
                      Runnable onCancel, ModalWindow returnsTo) {
        this(titleKey, initial, null, onAccept, onCancel, returnsTo);
    }

    /**
     * @param hint a sentence set under the field, or null for none: where the file will
     *             land, which the name alone does not say
     */
    public NameWindow(String titleKey, String initial, Component hint, Consumer<String> onAccept,
                      Runnable onCancel, ModalWindow returnsTo) {
        super(titleKey, returnsTo);
        this.hint = hint;
        this.onAccept = onAccept;
        this.onCancel = onCancel;
        this.field = new TextInput(Component.translatable("gui.mcskincreator.name_placeholder"),
                Metrics.MAX_NAME_CHARS, value -> {
                });
        this.field.setValue(initial);
        this.field.takeFocus();
    }

    /** Enter accepts from anywhere in the window, which is what a one-field form is for. */
    public boolean accept() {
        if (this.field.value().isBlank()) {
            return false;
        }
        this.onAccept.accept(this.field.value().trim());
        return true;
    }

    @Override
    public fr.clixmods.mcsc.mod.ui.Element initialFocus() {
        return this.field;
    }

    @Override
    protected int contentHeight(Canvas canvas) {
        return Metrics.BUTTON_HEIGHT_COMPACT + Metrics.PAD + hintHeight(canvas);
    }

    private int hintHeight(Canvas canvas) {
        if (this.hint == null) {
            return 0;
        }
        int rows = Prose.wrap(canvas, this.hint, width() - Metrics.PAD * 2).size();
        return rows * (canvas.lineHeight() + 1);
    }

    @Override
    protected void layoutBody(Canvas canvas, int left, int top, int width) {
        this.field.setBounds(left, top, width, Metrics.BUTTON_HEIGHT_COMPACT);
        addBodyChild(this.field);
    }

    @Override
    protected void drawBody(Paint paint, int left, int top, int width) {
        if (this.hint == null) {
            return;
        }
        Canvas canvas = paint.canvas();
        int cursorY = top + Metrics.BUTTON_HEIGHT_COMPACT + Metrics.PAD_TIGHT;
        for (String row : Prose.wrap(canvas, this.hint, width)) {
            canvas.text(Component.literal(row), left, cursorY, Palette.INK_MUTED);
            cursorY += canvas.lineHeight() + 1;
        }
    }

    @Override
    protected List<PixelButton> footer(Canvas canvas, Runnable close) {
        return List.of(
                new PixelButton(Component.translatable("gui.mcskincreator.cancel"),
                        PixelButton.Style.NORMAL, () -> {
                            this.onCancel.run();
                            close.run();
                        }),
                new PixelButton(Component.translatable("gui.mcskincreator.confirm"),
                        PixelButton.Style.NORMAL, () -> {
                            if (accept()) {
                                close.run();
                            }
                        }));
    }
}
