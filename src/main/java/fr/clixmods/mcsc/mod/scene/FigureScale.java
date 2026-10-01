/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.scene;

import com.mojang.blaze3d.vertex.PoseStack;

// Scales a figure drawn on the screen: blocks to pixels, with the depth turned towards
// the viewer. Before 1.20.5 PoseStack#scale corrects the normals with a fast cube root
// that a negative scale sends wild, and the figure's faces flicker between light and
// dark; the game's own inventory portrait scales the matrix alone there, and so does
// this. From 1.20.5 #scale flips the normals cleanly, and the inventory light is turned
// to match, so the plain call is the right one.
public final class FigureScale {
    private FigureScale() {
    }

    public static void apply(PoseStack stack, float scale) {
        //? if <1.20.5 {
        /*stack.last().pose().scale(scale, scale, -scale);
        *///?} else {
        stack.scale(scale, scale, -scale);
        //?}
    }
}
