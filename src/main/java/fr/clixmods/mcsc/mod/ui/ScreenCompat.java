/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;

//? if fabric {
import net.fabricmc.fabric.api.client.screen.v1.Screens;
//?}
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
//? if >=26.3 {
/*import com.mojang.blaze3d.Blaze3D;
*///?} elif >=1.21.11 {
import net.minecraft.util.Util;
//?} else {
/*import net.minecraft.Util;
*///?}

/**
 * The calls outside drawing whose names differ between the supported Minecraft versions.
 *
 * <p>All are plain renames with identical behavior, so they are isolated here
 * rather than scattered as conditionals across the screens that use them. Anything
 * that behaves the same on every target belongs in normal shared code, not in here.
 */
public final class ScreenCompat {
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

    //? if fabric {
    /** {@code Screens#getButtons} became {@code getWidgets} in Fabric screen API v5. */
    static List<AbstractWidget> widgets(Screen screen) {
        //? if >=26.1 {
        /*return Screens.getWidgets(screen);
        *///?} else {
        return Screens.getButtons(screen);
        //?}
    }
    //?}

    /**
     * {@code Util.getPlatform().openUri} moved to {@code Blaze3D.openUri} in 26.3. The
     * {@code Util} class itself moved from {@code net.minecraft} to
     * {@code net.minecraft.util} in 1.21.11, which is why its import is versioned too.
     */
    public static void openUri(URI uri) {
        //? if >=26.3 {
        /*Blaze3D.openUri(uri);
        *///?} else {
        Util.getPlatform().openUri(uri);
        //?}
    }

    /**
     * {@code Util.getPlatform().openPath} moved to {@code Blaze3D.openPath} in 26.3, and
     * did not exist yet in 1.20.1, which only opens a {@code File}.
     */
    public static void openPath(Path path) {
        //? if >=26.3 {
        /*Blaze3D.openPath(path);
        *///?} elif >=1.21 {
        Util.getPlatform().openPath(path);
        //?} else {
        /*Util.getPlatform().openFile(path.toFile());
        *///?}
    }

    /**
     * Whether shift is held: asked of the client from 1.21.9, of {@code Screen} as a
     * static before it.
     */
    public static boolean hasShiftDown() {
        // Null outside a running game: the editor's preview, in the tests.
        Minecraft client = Minecraft.getInstance();
        //? if >=1.21.9 {
        return client != null && client.hasShiftDown();
        //?} else {
        /*return client != null && Screen.hasShiftDown();
        *///?}
    }
}
