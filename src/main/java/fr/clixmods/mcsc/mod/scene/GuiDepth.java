/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
//? if >=1.21.5 && <1.21.6 {
/*package fr.clixmods.mcsc.mod.scene;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;

// Clears the depth the interface is drawn against, so a figure drawn straight into the
// screen neither cuts into what was drawn before it nor through what comes after. Only
// the targets before 1.21.6 need it, where the figure is not drawn into a texture of
// its own; the call is spelled three ways across them.
public final class GuiDepth {
    private GuiDepth() {
    }

    public static void clear() {
        // 1.21.5 talks to the GPU device rather than to GL.
        RenderSystem.getDevice().createCommandEncoder()
                .clearDepthTexture(Minecraft.getInstance().getMainRenderTarget().getDepthTexture(), 1.0);
    }
}
*///?} elif >=1.21.2 && <1.21.5 {
/*package fr.clixmods.mcsc.mod.scene;

import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;

// Clears the depth the interface is drawn against, so a figure drawn straight into the
// screen neither cuts into what was drawn before it nor through what comes after. Only
// the targets before 1.21.6 need it, where the figure is not drawn into a texture of
// its own; the call is spelled three ways across them.
public final class GuiDepth {
    private GuiDepth() {
    }

    public static void clear() {
        // Inside the scissor in force, which is the figure's rectangle.
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT);
    }
}
*///?} elif <1.21.2 {
/*package fr.clixmods.mcsc.mod.scene;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

// Clears the depth the interface is drawn against, so a figure drawn straight into the
// screen neither cuts into what was drawn before it nor through what comes after. Only
// the targets before 1.21.6 need it, where the figure is not drawn into a texture of
// its own; the call is spelled three ways across them.
public final class GuiDepth {
    private GuiDepth() {
    }

    public static void clear() {
        // Inside the scissor in force, which is the figure's rectangle.
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
    }
}
*///?}
