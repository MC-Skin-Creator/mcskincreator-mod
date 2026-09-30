/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui;

import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
//? if fabric {
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
//?} else {
/*import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
*///?}

/**
 * Adds the "Skin Creator" entry to the title screen and to the pause menu.
 *
 * <p>This and the entry point are the only places that know which loader runs the
 * mod: each loader has its own event for "a screen just laid out its widgets", and
 * its own way of adding one to it. Past that, {@link SkinPanel} does the same thing
 * on both.
 */
public final class MenuButtons {
    private MenuButtons() {
    }

    public static void register() {
        //? if fabric {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (isMenu(screen)) {
                SkinPanel.addTo(client, screen, ScreenCompat.widgets(screen), scaledWidth, scaledHeight);
            }
        });
        //?} else {
        /*NeoForge.EVENT_BUS.addListener((ScreenEvent.Init.Post event) -> {
            Screen screen = event.getScreen();
            if (isMenu(screen)) {
                SkinPanel.addTo(Minecraft.getInstance(), screen, widgets(event), screen.width, screen.height);
            }
        });
        *///?}
    }

    private static boolean isMenu(Screen screen) {
        return screen instanceof TitleScreen || screen instanceof PauseScreen;
    }

    //? if neoforge {
    /*/^*
     * The screen's widgets as the list {@link SkinPanel} reads and adds to.
     *
     * <p>Fabric hands over a live list whose {@code add} puts a widget on the screen.
     * NeoForge hands over a read-only view and a separate {@code addListener}, so this
     * copies the view and routes {@code add} to the event: the copy's constructor
     * does not go through {@code add}, which is what keeps the widgets already there
     * from being added twice.
     ^/
    private static List<AbstractWidget> widgets(ScreenEvent.Init.Post event) {
        List<AbstractWidget> existing = new ArrayList<>();
        for (GuiEventListener listener : event.getListenersList()) {
            if (listener instanceof AbstractWidget widget) {
                existing.add(widget);
            }
        }
        return new ArrayList<>(existing) {
            @Override
            public boolean add(AbstractWidget widget) {
                event.addListener(widget);
                return super.add(widget);
            }
        };
    }
    *///?}
}
