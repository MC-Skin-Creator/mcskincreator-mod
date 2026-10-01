/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui;

import java.util.List;
import java.util.Optional;


import fr.clixmods.mcsc.mod.MCSkinCreatorClient;
import fr.clixmods.mcsc.mod.skin.AppliedSkin;
import fr.clixmods.mcsc.mod.skin.GameSkins;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
//? if >=1.20.2 {
import net.minecraft.client.gui.components.SpriteIconButton;
//?} else {
/*import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
*///?}
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * The skin entry as it sits on a vanilla menu: the player name, the player model
 * below it and the button that opens the editor, stacked against the right edge
 * and centred on the menu they stand next to.
 *
 * <p>Everything here is a widget, which is what makes it work from the loader's
 * "screen initialised" event: a mod can add widgets to someone else's screen
 * but never draw on it, and {@link MenuFigure} draws the model itself.
 */
final class SkinPanel {
    private static final int PANEL_WIDTH = 120;
    private static final int BUTTON_HEIGHT = 20;
    private static final int SKIN_WIDTH = 85;
    private static final int SKIN_HEIGHT = 110;
    private static final int NAME_HEIGHT = 9;
    private static final int GAP = 4;
    private static final int MARGIN = 8;

    /** The mark, a 32 pixel sprite in the game's GUI atlas, drawn at 16 like the vanilla icons. */
    private static final Identifier ICON = MCSkinCreatorClient.id("icon");
    private static final int ICON_SIZE = 16;

    private static final int PANEL_HEIGHT = NAME_HEIGHT + GAP + SKIN_HEIGHT + GAP + BUTTON_HEIGHT;

    /** Room left above the panel for the logo and the splash text. */
    private static final int TOP_CLEARANCE = 72;

    /**
     * Narrowest vanilla menu entry: 98 on the two-column rows, 200 or 204 on the
     * full-width ones. Narrower widgets are the language and accessibility icons.
     */
    private static final int MENU_ENTRY_MIN_WIDTH = 90;

    private SkinPanel() {
    }

    /**
     * Lays the entry out beside the menu of {@code screen}. {@code widgets} are the
     * screen's own, and adding to the list puts a widget on the screen: the loader's
     * hook in {@link MenuButtons} makes it so.
     */
    static void addTo(Minecraft client, Screen screen, List<AbstractWidget> widgets,
                      int scaledWidth, int scaledHeight) {
        Optional<Bounds> found = menuBounds(widgets);

        if (found.isEmpty()) {
            // No menu to sit next to: the pause screen opened with F3+Esc, which
            // deliberately shows nothing at all.
            return;
        }

        Bounds menu = found.get();
        int x = scaledWidth - MARGIN - PANEL_WIDTH;
        int lowestTop = scaledHeight - PANEL_HEIGHT - MARGIN;

        if (x < menu.right() + GAP || lowestTop < TOP_CLEARANCE) {
            // A small window at a large GUI scale leaves no room beside the menu for
            // the panel. Shrink the entry to the mark alone, in a square like the
            // language and accessibility buttons, and put it at the end of the menu's
            // last row - beside the accessibility button on the title screen.
            addCompact(client, screen, widgets, menu);
            return;
        }

        int y = Math.max(TOP_CLEARANCE, Math.min(lowestTop, menu.centerY() - PANEL_HEIGHT / 2));

        widgets.add(playerName(client, x, y));
        widgets.add(skinPreview(client, x + (PANEL_WIDTH - SKIN_WIDTH) / 2, y + NAME_HEIGHT + GAP));
        addOpenButton(client, screen, widgets, x, y + PANEL_HEIGHT - BUTTON_HEIGHT);
    }

    /**
     * The full-width button: the mark and the label, laid out by the game.
     *
     * <p>Both entries are {@code SpriteIconButton}s, the class behind the language and
     * accessibility buttons, rather than a button with a widget drawn over it: the mark
     * is then part of the button, and nothing that moves the buttons of a menu can leave
     * one behind.
     */
    private static void addOpenButton(Minecraft client, Screen screen, List<AbstractWidget> widgets,
                                      int x, int y) {
        widgets.add(openButton(client, screen, x, y, PANEL_WIDTH, false));
    }

    /** The square button: the mark alone, named by its tooltip. */
    private static void addCompact(Minecraft client, Screen screen, List<AbstractWidget> widgets, Bounds menu) {
        int right = 0;
        for (AbstractWidget widget : widgets) {
            // The last row's rightmost widget, small icon buttons included.
            if (widget.getHeight() >= BUTTON_HEIGHT && widget.getY() + widget.getHeight() == menu.bottom()) {
                right = Math.max(right, widget.getX() + widget.getWidth());
            }
        }
        Button button = openButton(client, screen, right + GAP, menu.bottom() - BUTTON_HEIGHT,
                BUTTON_HEIGHT, true);
        button.setTooltip(Tooltip.create(Component.translatable("menu.mcskincreator.open")));
        widgets.add(button);
    }

    private static Button openButton(Minecraft client, Screen screen, int x, int y, int width,
                                     boolean iconOnly) {
        Component label = Component.translatable("menu.mcskincreator.open");
        Button.OnPress open = ignored -> ScreenCompat.setScreen(client, new SkinCreatorScreen(screen));
        //? if >=1.20.2 {
        Button button = SpriteIconButton
                .builder(label, open, iconOnly)
                .width(width)
                .sprite(ICON, ICON_SIZE, ICON_SIZE)
                .build();
        button.setPosition(x, y);
        return button;
        //?} else {
        /*return new IconButton(x, y, width, label, open, iconOnly);
        *///?}
    }

    //? if <1.20.2 {
    /*// 1.20.1 has neither SpriteIconButton nor the sprite atlas it reads the icon from:
    // the icon is the same file, drawn as a plain texture over a vanilla button. The
    // wide button says its label, as SpriteIconButton's does beside its icon; the compact
    // one draws the icon alone and keeps the label for narration and the tooltip.
    private static final class IconButton extends Button {
        private static final Identifier ICON_TEXTURE = MCSkinCreatorClient.id("textures/gui/sprites/icon.png");

        private final boolean iconOnly;

        IconButton(int x, int y, int width, Component label, OnPress open, boolean iconOnly) {
            super(x, y, width, BUTTON_HEIGHT, label, open, DEFAULT_NARRATION);
            this.iconOnly = iconOnly;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            super.renderWidget(graphics, mouseX, mouseY, delta);
            if (this.iconOnly) {
                graphics.blit(ICON_TEXTURE, getX() + (getWidth() - ICON_SIZE) / 2,
                        getY() + (getHeight() - ICON_SIZE) / 2, 0, 0,
                        ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            }
        }

        @Override
        public void renderString(GuiGraphics graphics, Font font, int color) {
            if (!this.iconOnly) {
                super.renderString(graphics, font, color);
            }
        }
    }
    *///?}

    private static StringWidget playerName(Minecraft client, int x, int y) {
        // A StringWidget draws at its own left edge, so it is sized to the name it
        // measures and centred by hand over the panel. The clamp is what keeps a
        // sixteen-character name from running past the panel; 1.21.9 is where it
        // appeared, and before it the widest such name still fits, 96 pixels in 120.
        StringWidget name = new StringWidget(Component.literal(client.getUser().getName()), client.font);
        //? if >=1.21.9 {
        name.setMaxWidth(PANEL_WIDTH);
        //?}
        name.setPosition(x + (PANEL_WIDTH - name.getWidth()) / 2, y);
        return name;
    }

    private static MenuFigure skinPreview(Minecraft client, int x, int y) {
        // The lookup already falls back to the default skin and keeps polling until
        // the real one is downloaded, so the panel fills in on its own and offline
        // players get Steve or Alex instead of an empty box. It asks for an unsigned
        // texture, since a locally applied skin will never carry a signature.
        //
        // What it resolves, though, is the account's skin, and a running client's idea
        // of that is the profile it was handed on joining: after an upload this panel
        // would go on showing the old skin until the game restarts. Wrapping the lookup
        // puts the applied skin here too - the mixin cannot, since a menu has no player
        // entity to draw from.
        MenuFigure preview = new MenuFigure(SKIN_WIDTH, SKIN_HEIGHT, client.font,
                AppliedSkin.over(GameSkins.localId(client), GameSkins.localLookup(client)));
        preview.setPosition(x, y);
        preview.setTooltip(Tooltip.create(Component.translatable("gui.mcskincreator.skin_preview")));
        return preview;
    }

    /**
     * The box the vanilla menu entries occupy, empty when the screen has none. The
     * panel is aligned on that box rather than on hardcoded coordinates: the
     * vanilla grid moves with the window size, the GUI scale and the Realms
     * notification, and it is not laid out the same way on every version.
     */
    private static Optional<Bounds> menuBounds(List<AbstractWidget> widgets) {
        int top = Integer.MAX_VALUE;
        int bottom = 0;
        int right = 0;
        boolean found = false;

        for (AbstractWidget widget : widgets) {
            // Height rules out the copyright line at the bottom right, which is a
            // full widget on both versions and would otherwise push the panel off
            // the screen on its own.
            if (widget.getWidth() < MENU_ENTRY_MIN_WIDTH || widget.getHeight() < BUTTON_HEIGHT) {
                continue;
            }

            found = true;
            top = Math.min(top, widget.getY());
            bottom = Math.max(bottom, widget.getY() + widget.getHeight());
            right = Math.max(right, widget.getX() + widget.getWidth());
        }

        return found ? Optional.of(new Bounds(top, bottom, right)) : Optional.empty();
    }

    private record Bounds(int top, int bottom, int right) {
        int centerY() {
            return (this.top + this.bottom) / 2;
        }
    }
}
