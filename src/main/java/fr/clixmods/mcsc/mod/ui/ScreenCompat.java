/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui;

import java.util.List;

import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;

/**
 * The two calls whose names differ between the supported Minecraft versions.
 *
 * <p>Both are plain renames with identical behavior, so they are isolated here
 * rather than scattered as conditionals across the screens that use them. Anything
 * that behaves the same on every target belongs in normal shared code, not in here.
 */
final class ScreenCompat {
    private ScreenCompat() {
    }

    /** {@code Minecraft#setScreen} became {@code setScreenAndShow} in 26.x. */
    static void setScreen(Minecraft client, Screen screen) {
        //? if >=26.1 {
        /*client.setScreenAndShow(screen);
        *///?} else {
        client.setScreen(screen);
        //?}
    }

    /** {@code Screens#getButtons} became {@code getWidgets} in Fabric screen API v5. */
    static List<AbstractWidget> widgets(Screen screen) {
        //? if >=26.1 {
        /*return Screens.getWidgets(screen);
        *///?} else {
        return Screens.getButtons(screen);
        //?}
    }
}
