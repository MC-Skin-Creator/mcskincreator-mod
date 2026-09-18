/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui.window;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import fr.clixmods.mcsc.mod.ui.Canvas;
import fr.clixmods.mcsc.mod.ui.widget.PixelButton;
import net.minecraft.network.chat.Component;

/**
 * Prose, then Cancel and one green button that does the thing it just described.
 *
 * <p>The prose is the point rather than the frame around it. This window exists for the
 * one action in the mod that leaves the machine and cannot be undone from here, and
 * what it costs has to be readable <em>before</em> the button, not discovered from the
 * result afterwards.
 *
 * <p>The button's label and whether it works are asked for at layout rather than fixed
 * at construction, so a window left open while a cooldown runs down counts down with
 * it. A button that says "wait 20 seconds" and still says it forty seconds later is a
 * button people conclude is broken.
 */
public class ConfirmWindow extends TextWindow {
    private final Supplier<Component> confirmLabel;
    private final BooleanSupplier confirmEnabled;
    private final Runnable onConfirm;

    public ConfirmWindow(String titleKey, List<Line> lines, Supplier<Component> confirmLabel,
                         BooleanSupplier confirmEnabled, Runnable onConfirm, ModalWindow returnsTo) {
        super(titleKey, lines, returnsTo);
        this.confirmLabel = confirmLabel;
        this.confirmEnabled = confirmEnabled;
        this.onConfirm = onConfirm;
    }

    @Override
    protected List<PixelButton> footer(Canvas canvas, Runnable close) {
        PixelButton confirm = new PixelButton(this.confirmLabel.get(), PixelButton.Style.PRIMARY,
                () -> {
                    this.onConfirm.run();
                    close.run();
                });
        // Disabled rather than absent: what this button does is the reason the window is
        // open, and a window whose one action vanished explains nothing.
        confirm.setEnabled(this.confirmEnabled.getAsBoolean());
        return List.of(
                new PixelButton(Component.translatable("gui.mcskincreator.cancel"),
                        PixelButton.Style.NORMAL, close),
                confirm);
    }
}
