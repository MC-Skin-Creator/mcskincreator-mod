/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui;

import java.util.function.Supplier;

import fr.clixmods.mcsc.mod.scene.PosedPlayer;
import fr.clixmods.mcsc.mod.scene.SceneCamera;
import fr.clixmods.mcsc.mod.skin.SkinLook;
//? if >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?} else {
import net.minecraft.client.gui.GuiGraphics;
//?}
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
//? if >=1.21.9 {
import net.minecraft.client.input.MouseButtonEvent;
//?}
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.CommonComponents;

/**
 * The player on the title and pause screens: turned by dragging, like vanilla's
 * {@code PlayerSkinWidget} it replaces, and watching the pointer, which that widget
 * cannot — it hands the game a finished model and has no say over the head.
 *
 * <p>So the figure goes through {@link PosedPlayer} instead, the same entity route the
 * editor draws with, where the head is a field on the render state. It is still a
 * widget, because a mod can add widgets to a vanilla screen but not draw on it.
 */
final class MenuFigure extends AbstractWidget {
    private final Font font;
    private final Supplier<SkinLook> skin;
    private final SceneCamera camera = new SceneCamera();

    MenuFigure(int width, int height, Font font, Supplier<SkinLook> skin) {
        super(0, 0, width, height, CommonComponents.EMPTY);
        this.font = font;
        this.skin = skin;
    }

    // The hook and its graphics type are the only things 26.x renamed here; the body
    // is shared, and GameCanvas takes either type.
    //? if >=26.1 {
    /*@Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
    *///?} else {
    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
    //?}
        PosedPlayer.drawWatching(new GameCanvas(graphics, this.font), this.skin.get(), this.camera,
                mouseX, mouseY, this.getX(), this.getY(), this.getWidth(), this.getHeight());
    }

    //? if >=1.21.9 {
    @Override
    protected void onDrag(MouseButtonEvent event, double dragX, double dragY) {
        this.camera.turn(dragX, dragY);
    }
    //?} else {
    /*@Override
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        this.camera.turn(dragX, dragY);
    }
    *///?}

    @Override
    public void playDownSound(SoundManager soundManager) {
        // Grabbing the figure to turn it is not a button press.
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
        // Decorative, as vanilla's skin widget is; the tooltip says what it is.
    }

    @Override
    public ComponentPath nextFocusPath(FocusNavigationEvent event) {
        // Out of the tab order, as vanilla's is: it is only ever reached with the mouse.
        return null;
    }
}
