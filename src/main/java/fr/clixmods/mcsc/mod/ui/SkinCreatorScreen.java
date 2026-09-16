/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

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
        super(Text.translatable("screen.mcskincreator.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.addDrawableChild(ButtonWidget
                .builder(Text.translatable("gui.mcskincreator.close"), ignored -> this.close())
                .dimensions((this.width - BUTTON_WIDTH) / 2, this.height - 32, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title,
                this.width / 2, TITLE_Y, TITLE_COLOR);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable("screen.mcskincreator.placeholder"),
                this.width / 2, this.height / 2, PLACEHOLDER_COLOR);
    }

    @Override
    public void close() {
        // Hand control back to whichever menu opened us, so Escape behaves the way
        // the player expects from a vanilla sub-screen.
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }
}
