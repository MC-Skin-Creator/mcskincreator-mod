/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
//? if fabric {
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
//?} elif neoforge {
/*import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
*///?}

/**
 * Adds the "Skin Creator" entry to the title screen and to the pause menu.
 *
 * <p>This and the entry point are the only places that know which loader runs the
 * mod: each loader has its own event for "a screen just laid out its widgets", and
 * its own way of adding one to it. Quilt has none - QSL, which had one, stopped
 * following game versions at 1.21.1 - so on Quilt the event is
 * {@link fr.clixmods.mcsc.mod.mixin.ScreenMixin}, which calls {@link #afterInit}.
 * Past that, {@link SkinPanel} does the same thing on all three.
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
        //?} elif quilt {
        /*// Nothing to register: the mixin calls afterInit on every screen.
        *///?} else {
        /*NeoForge.EVENT_BUS.addListener((ScreenEvent.Init.Post event) -> {
            Screen screen = event.getScreen();
            if (isMenu(screen)) {
                SkinPanel.addTo(Minecraft.getInstance(), screen, widgets(event.getListenersList(), event::addListener),
                        screen.width, screen.height);
            }
        });
        *///?}
    }

    /**
     * Called by {@link fr.clixmods.mcsc.mod.mixin.ScreenMixin} once {@code screen} has
     * laid out its widgets, where Fabric and NeoForge fire their event. {@code add}
     * puts a widget on the screen. Compiled everywhere because the mixin is, and only
     * ever called on Quilt, the one loader that applies it.
     */
    public static void afterInit(Screen screen, Consumer<AbstractWidget> add) {
        if (isMenu(screen)) {
            SkinPanel.addTo(Minecraft.getInstance(), screen, widgets(screen.children(), add),
                    screen.width, screen.height);
        }
    }

    private static boolean isMenu(Screen screen) {
        return screen instanceof TitleScreen || screen instanceof PauseScreen;
    }

    /**
     * The screen's widgets as the list {@link SkinPanel} reads and adds to.
     *
     * <p>Fabric hands over a live list whose {@code add} puts a widget on the screen.
     * NeoForge hands over a read-only view and a separate {@code addListener}, and on
     * Quilt the mixin has the screen's own lists, so this copies the widgets already
     * there and routes {@code add} to {@code put}: the copy's constructor does not go
     * through {@code add}, which is what keeps those widgets from being added twice.
     */
    private static List<AbstractWidget> widgets(Iterable<? extends GuiEventListener> listeners,
                                                Consumer<AbstractWidget> put) {
        List<AbstractWidget> existing = new ArrayList<>();
        for (GuiEventListener listener : listeners) {
            if (listener instanceof AbstractWidget widget) {
                existing.add(widget);
            }
        }
        return new ArrayList<>(existing) {
            @Override
            public boolean add(AbstractWidget widget) {
                put.accept(widget);
                return super.add(widget);
            }
        };
    }
}
