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

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlayerSkinWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The skin entry as it sits on a vanilla menu: the player name, the player model
 * below it and the button that opens the editor, stacked against the right edge
 * and centred on the menu they stand next to.
 *
 * <p>Everything here is a widget, which is what makes it work from
 * {@code ScreenEvents.AFTER_INIT}: a mod can add widgets to someone else's screen
 * but never draw on it, and {@link PlayerSkinWidget} draws the model itself. That
 * also keeps the whole panel clear of the 26.x render-state rewrite.
 */
final class SkinPanel {
    private static final int PANEL_WIDTH = 120;
    private static final int BUTTON_HEIGHT = 20;
    private static final int SKIN_WIDTH = 85;
    private static final int SKIN_HEIGHT = 110;
    private static final int NAME_HEIGHT = 9;
    private static final int GAP = 4;
    private static final int MARGIN = 8;

    /**
     * Room kept under the fallback button for the copyright line, which vanilla
     * puts in the bottom right corner, ten pixels tall, on both versions.
     */
    private static final int BOTTOM_CLEARANCE = 14;

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

    static void addTo(Minecraft client, Screen screen, int scaledWidth, int scaledHeight) {
        List<AbstractWidget> widgets = ScreenCompat.widgets(screen);
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
            // A small window at a large GUI scale leaves no room beside the menu.
            // Keep the button on the right edge and drop it under the menu rather
            // than letting the panel overlap vanilla.
            int y = Math.min(menu.bottom() + GAP, scaledHeight - BUTTON_HEIGHT - BOTTOM_CLEARANCE);
            widgets.add(openButton(client, screen, x, y));
            return;
        }

        int y = Math.max(TOP_CLEARANCE, Math.min(lowestTop, menu.centerY() - PANEL_HEIGHT / 2));

        widgets.add(playerName(client, x, y));
        widgets.add(skinPreview(client, x + (PANEL_WIDTH - SKIN_WIDTH) / 2, y + NAME_HEIGHT + GAP));
        widgets.add(openButton(client, screen, x, y + PANEL_HEIGHT - BUTTON_HEIGHT));
    }

    private static Button openButton(Minecraft client, Screen screen, int x, int y) {
        return Button
                .builder(Component.translatable("menu.mcskincreator.open"),
                        ignored -> ScreenCompat.setScreen(client, new SkinCreatorScreen(screen)))
                .bounds(x, y, PANEL_WIDTH, BUTTON_HEIGHT)
                .build();
    }

    private static StringWidget playerName(Minecraft client, int x, int y) {
        // A StringWidget draws at its own left edge, so it is sized to the name it
        // measures and centred by hand over the panel. The clamp is what keeps a
        // sixteen-character name from running past the panel.
        StringWidget name = new StringWidget(Component.literal(client.getUser().getName()), client.font);
        name.setMaxWidth(PANEL_WIDTH);
        name.setPosition(x + (PANEL_WIDTH - name.getWidth()) / 2, y);
        return name;
    }

    private static PlayerSkinWidget skinPreview(Minecraft client, int x, int y) {
        // createLookup already falls back to the default skin and keeps polling
        // until the real one is downloaded, so the panel fills in on its own and
        // offline players get Steve or Alex instead of an empty box. The flag would
        // demand a signed texture, which a locally applied skin will never carry.
        PlayerSkinWidget preview = new PlayerSkinWidget(SKIN_WIDTH, SKIN_HEIGHT, client.getEntityModels(),
                client.getSkinManager().createLookup(client.getGameProfile(), false));
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
