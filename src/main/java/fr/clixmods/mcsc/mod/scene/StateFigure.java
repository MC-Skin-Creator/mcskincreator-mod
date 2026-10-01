/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
//? if >=1.21.2 && <1.21.6 {
/*package fr.clixmods.mcsc.mod.scene;

import java.util.Map;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import fr.clixmods.mcsc.mod.mixin.EntityRenderDispatcherAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// The figure on 1.21.2 to 1.21.5: a render state, drawn by the game's own player
// renderer, but by hand, since the picture-in-picture path that does it from 1.21.6
// does not exist yet. The numbers are the same as that path's: the rectangle is the
// scissor and its centre is where the figure stands, the scale is pixels per block.
//
// Drawn straight into the screen, the figure would share the depth buffer with the
// interface: what is under it would cut into it and what is drawn over it would not
// cover it. So the depth is cleared around it, which is what drawing it into a texture
// of its own gives the newer path for nothing.
public final class StateFigure {
    // What the game draws a figure in the inventory at: full brightness.
    private static final int FULL_BRIGHT = 15728880;

    private StateFigure() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void draw(GuiGraphics graphics, PlayerRenderState state, float scale, Vector3f translation,
                            Quaternionf rotation, int x, int y, int width, int height) {
        EntityRenderer renderer = renderer(state);
        if (renderer == null) {
            return;
        }
        graphics.flush();
        graphics.enableScissor(x, y, x + width, y + height);
        GuiDepth.clear();

        PoseStack stack = graphics.pose();
        stack.pushPose();
        stack.translate(x + width / 2.0F, y + height / 2.0F, 50.0F);
        stack.scale(scale, scale, -scale);
        stack.translate(translation.x, translation.y, translation.z);
        stack.mulPose(rotation);
        Vec3 offset = renderer.getRenderOffset(state);
        stack.translate(offset.x, offset.y, offset.z);

        Lighting.setupForEntityInInventory();
        graphics.drawSpecial(buffers -> renderer.render(state, stack, buffers, FULL_BRIGHT));
        Lighting.setupFor3DItems();
        stack.popPose();

        GuiDepth.clear();
        graphics.disableScissor();
    }

    // The renderer the dispatcher would pick for this skin: the slim one or the classic.
    private static EntityRenderer<?, ?> renderer(PlayerRenderState state) {
        Map<PlayerSkin.Model, EntityRenderer<? extends Player, ?>> renderers =
                ((EntityRenderDispatcherAccessor) (Object) Minecraft.getInstance().getEntityRenderDispatcher())
                        .mcskincreator$playerRenderers();
        EntityRenderer<?, ?> renderer = renderers.get(state.skin.model());
        return renderer != null ? renderer : renderers.get(PlayerSkin.Model.WIDE);
    }
}
*///?}
