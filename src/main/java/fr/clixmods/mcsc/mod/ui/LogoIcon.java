/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui;

import net.minecraft.client.Minecraft;
//? if >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?} else {
import net.minecraft.client.gui.GuiGraphics;
//?}
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.Identifier;

/**
 * The mod's mark, drawn over a vanilla button on the menus.
 *
 * <p>It is a separate widget, added right after the button, because a vanilla
 * {@code Button} cannot be given a picture the same way on both versions. It is inert:
 * inactive widgets ignore the pointer, so every click and hover goes to the button
 * underneath, and it stays out of the tab order.
 */
final class LogoIcon extends AbstractWidget {
    /** The mark's size on screen: 16 pixels, as in the editor's top bar. */
    static final int SIZE = 16;

    private final Minecraft client;

    LogoIcon(Minecraft client, int x, int y) {
        super(x, y, SIZE, SIZE, CommonComponents.EMPTY);
        this.client = client;
        this.active = false;
    }

    //? if >=26.1 {
    /*@Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
    *///?} else {
    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
    //?}
        Identifier texture = Logo.texture(this.client);
        int source = Logo.size();
        if (texture == null || source <= 0) {
            return;
        }
        // The whole icon, scaled down: the file is 128 px and the mark is 16.
        new GameCanvas(graphics, this.client.font).blit(texture, this.getX(), this.getY(), SIZE, SIZE,
                0.0F, 0.0F, source, source, source, source);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
        // Decorative: the button underneath carries the name.
    }

    @Override
    public ComponentPath nextFocusPath(FocusNavigationEvent event) {
        return null;
    }
}
