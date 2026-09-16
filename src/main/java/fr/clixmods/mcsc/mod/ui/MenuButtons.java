/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Adds the "Skin Creator" entry to the title screen and to the pause menu. */
public final class MenuButtons {
    private static final int BUTTON_WIDTH = 120;
    private static final int BUTTON_HEIGHT = 20;
    private static final int MARGIN = 4;

    private MenuButtons() {
    }

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof TitleScreen) && !(screen instanceof GameMenuScreen)) {
                return;
            }

            // Both vanilla screens lay their own buttons out in a centred grid whose
            // height changes with the window size, the Realms notification and the
            // GUI scale. Sitting in the bottom-left corner is the one spot that
            // cannot collide with them. To be revisited once the screen earns a
            // place of its own in the menu.
            ButtonWidget button = ButtonWidget
                    .builder(Text.translatable("menu.mcskincreator.open"),
                            ignored -> client.setScreen(new SkinCreatorScreen(screen)))
                    .dimensions(MARGIN, scaledHeight - BUTTON_HEIGHT - MARGIN, BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build();

            Screens.getButtons(screen).add(button);
        });
    }
}
