/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.skin;

import com.mojang.blaze3d.platform.NativeImage;

/**
 * Reads and writes one pixel of a {@code NativeImage}, as packed ARGB on every target.
 *
 * <p>From 1.21.2 the game's own {@code getPixel} and {@code setPixel} speak ARGB. Before
 * it the only accessors were {@code getPixelRGBA} and {@code setPixelRGBA}, which despite
 * the name pack the bytes as ABGR - red and blue swapped. The mod reads and writes ARGB
 * everywhere, so the swap is made here and nowhere else.
 */
public final class ImagePixels {
    private ImagePixels() {
    }

    public static int get(NativeImage image, int x, int y) {
        //? if >=1.21.2 {
        return image.getPixel(x, y);
        //?} else {
        /*return swapRedAndBlue(image.getPixelRGBA(x, y));
        *///?}
    }

    public static void set(NativeImage image, int x, int y, int argb) {
        //? if >=1.21.2 {
        image.setPixel(x, y, argb);
        //?} else {
        /*image.setPixelRGBA(x, y, swapRedAndBlue(argb));
        *///?}
    }

    //? if <1.21.2 {
    /*// ARGB to ABGR and back: the same swap both ways.
    private static int swapRedAndBlue(int color) {
        return color & 0xFF00FF00 | color >>> 16 & 0xFF | (color & 0xFF) << 16;
    }
    *///?}
}
