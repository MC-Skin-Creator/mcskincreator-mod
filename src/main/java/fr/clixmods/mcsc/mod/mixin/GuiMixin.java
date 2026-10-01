/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.mixin;

import fr.clixmods.mcsc.mod.scene.HiddenHud;
//? if >=1.21 {
import net.minecraft.client.DeltaTracker;
//?}
import net.minecraft.client.gui.Gui;
//? if >=26.2 {
/*import org.spongepowered.asm.mixin.injection.ModifyVariable;
*///?} elif >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
*///?} else {
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

/**
 * Leaves the game's HUD undrawn while the editor's first-person view is open.
 *
 * <p>The game has a flag for this and it cannot be used here. Both targets guard the
 * {@code ItemInHandRenderer} call with the same boolean they guard the HUD with —
 * {@code Options.hideGui} on 1.21.11, {@code GuiRenderState.isHudHidden} on 26.2, read
 * out of the bytecode of {@code GameRenderer.renderItemInHand} on both jars — so setting
 * it hid the hotbar and the arm together, and the arm is the one thing that view exists
 * to show. There is no second flag and no event that cancels HUD drawing: the HUD is
 * drawn from a method that returns void and is called for its side effects.
 *
 * <p>So the flag stays the player's (it is F1, a setting they own) and the drawing is
 * skipped here instead, for the frames {@link HiddenHud} asks about and no others.
 *
 * <p>The targets reach the HUD by different names but through the same class, which
 * is why this is one mixin with a versioned injection rather than several mixins:
 *
 * <ul>
 *   <li>1.21.10 and 1.21.11 draw it in {@code Gui.render}, which is the HUD and nothing
 *       else, so the call is cancelled outright;</li>
 *   <li>26.1 renamed that method {@code Gui.extractRenderState(GuiGraphicsExtractor,
 *       DeltaTracker)} and it is still the HUD and nothing else — the screen is drawn
 *       from {@code GameRenderer} after it — so it is cancelled the same way;</li>
 *   <li>26.2 and later moved the drawing into {@code Hud} and calls it from
 *       {@code Gui.extractRenderState(DeltaTracker, boolean, boolean)}, whose first
 *       boolean is what gates it — and whose second gates the screen, so cancelling the
 *       call would take the editor with it. The boolean is forced to false instead.</li>
 * </ul>
 */
@Mixin(Gui.class)
public abstract class GuiMixin {
    //? if >=26.2 {
    /*@ModifyVariable(method = "extractRenderState(Lnet/minecraft/client/DeltaTracker;ZZ)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private boolean mcskincreator$leaveTheHudOut(boolean drawHud) {
        return drawHud && !HiddenHud.hidden();
    }
    *///?} elif >=26.1 {
    /*@Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;"
            + "Lnet/minecraft/client/DeltaTracker;)V",
            at = @At("HEAD"), cancellable = true)
    private void mcskincreator$leaveTheHudOut(GuiGraphicsExtractor graphics, DeltaTracker delta,
                                              CallbackInfo info) {
        if (HiddenHud.hidden()) {
            info.cancel();
        }
    }
    *///?} elif >=1.21 {
    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V",
            at = @At("HEAD"), cancellable = true)
    private void mcskincreator$leaveTheHudOut(GuiGraphics graphics, DeltaTracker delta,
                                              CallbackInfo info) {
        if (HiddenHud.hidden()) {
            info.cancel();
        }
    }
    //?} else {
    /*// 1.20.1 hands the HUD a bare partial tick, before DeltaTracker existed.
    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;F)V", at = @At("HEAD"), cancellable = true)
    private void mcskincreator$leaveTheHudOut(GuiGraphics graphics, float partialTick, CallbackInfo info) {
        if (HiddenHud.hidden()) {
            info.cancel();
        }
    }
    *///?}
}
