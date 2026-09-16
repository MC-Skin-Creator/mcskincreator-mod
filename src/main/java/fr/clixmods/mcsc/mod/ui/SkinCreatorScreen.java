/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui;

//? if >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?} else {
import net.minecraft.client.gui.GuiGraphics;
//?}
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The skin editor screen.
 *
 * <p>Empty on purpose for now: this first version only proves the mod loads and
 * that the game can open its screen. The element library, the layer panel and the
 * player preview land here next.
 */
public class SkinCreatorScreen extends Screen {
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int TITLE_Y = 40;
    private static final int TITLE_COLOR = 0xFFFFFF;
    private static final int PLACEHOLDER_COLOR = 0xA0A0A0;

    private final Screen parent;

    public SkinCreatorScreen(Screen parent) {
        super(Component.translatable("screen.mcskincreator.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.addRenderableWidget(Button
                .builder(Component.translatable("gui.mcskincreator.close"), ignored -> this.onClose())
                .bounds((this.width - BUTTON_WIDTH) / 2, this.height - 32, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    // 26.x replaced immediate-mode screen drawing with a render-state extraction
    // pass: the hook, its parameter type and the text call all changed. The two
    // variants below are the whole difference - what they draw is identical.
    //? if >=26.1 {
    /*@Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        graphics.centeredText(this.font, this.title,
                this.width / 2, TITLE_Y, TITLE_COLOR);
        graphics.centeredText(this.font, Component.translatable("screen.mcskincreator.placeholder"),
                this.width / 2, this.height / 2, PLACEHOLDER_COLOR);
    }
    *///?} else {
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);

        graphics.drawCenteredString(this.font, this.title,
                this.width / 2, TITLE_Y, TITLE_COLOR);
        graphics.drawCenteredString(this.font, Component.translatable("screen.mcskincreator.placeholder"),
                this.width / 2, this.height / 2, PLACEHOLDER_COLOR);
    }
    //?}

    @Override
    public void onClose() {
        // Hand control back to whichever menu opened us, so Escape behaves the way
        // the player expects from a vanilla sub-screen.
        if (this.minecraft != null) {
            ScreenCompat.setScreen(this.minecraft, this.parent);
        }
    }
}
