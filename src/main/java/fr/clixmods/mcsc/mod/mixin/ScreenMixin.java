/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.mixin;

import java.util.List;

import fr.clixmods.mcsc.mod.ui.MenuButtons;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tells {@link MenuButtons} that a screen has just laid out its widgets - on Quilt, and
 * only there.
 *
 * <p>Fabric and NeoForge both have an event for this, and the mod uses it. Quilt has
 * none: its own one lived in QSL, which stopped following game versions at 1.21.1, and
 * the alternative is asking a Quilt player to install the Fabric API for the sake of one
 * button. So this class is listed in {@code mcskincreator.quilt.mixins.json}, which only
 * {@code quilt.mod.json} names: it is compiled on every target and applied on Quilt
 * alone, where it would otherwise put a second entry beside the one the event adds.
 *
 * <p>It hooks where the Fabric API's own screen events do, checked in its sources for
 * every supported version: the end of {@code Screen.init} and of {@code Screen.resize},
 * the two calls that rebuild a screen's widgets, and the screen's three lists, which are
 * what {@code addRenderableWidget} itself adds to. {@code init} took the
 * {@code Minecraft} as well up to 1.21.10, which is the one versioned line here; the
 * handler takes no argument of the target's, so it fits both.
 */
@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Shadow
    @Final
    private List<GuiEventListener> children;

    @Shadow
    @Final
    private List<NarratableEntry> narratables;

    @Shadow
    @Final
    private List<Renderable> renderables;

    //? if >=1.21.11 {
    @Inject(method = {"init(II)V", "resize"}, at = @At("TAIL"))
    //?} else {
    /*@Inject(method = {"init(Lnet/minecraft/client/Minecraft;II)V", "resize"}, at = @At("TAIL"))
    *///?}
    private void mcskincreator$addTheEntry(CallbackInfo info) {
        MenuButtons.afterInit((Screen) (Object) this, this::mcskincreator$add);
    }

    /** What {@code Screen#addRenderableWidget} does, which is protected. */
    @Unique
    private void mcskincreator$add(AbstractWidget widget) {
        this.children.add(widget);
        this.narratables.add(widget);
        this.renderables.add(widget);
    }
}
