/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;

/**
 * The scale the editor draws itself at, which is its own and not the player's.
 *
 * <p>This screen is a workbench, not a menu. A menu needs six buttons legible from the
 * sofa; this needs three columns, a grid of thumbnails, a stack of layers and four
 * settings at once, and at the GUI scale most people play at it gets 640 by 360 pixels
 * to do that in. The game's font is 8 of those pixels tall — a ninth of the height of
 * a layer row — so everything around it has to be big too, and the result reads as an
 * interface designed for a phone.
 *
 * <p>The font is the floor here and it cannot be lowered: it is a bitmap, and half a
 * pixel of it is not half a letter, it is a broken letter. Drawing the editor smaller
 * within the player's scale would mean scaling by two thirds and mangling every glyph
 * on screen. The only way to fit more interface into the same window is to give the
 * whole window a smaller scale, which is what this does.
 *
 * <p>So the editor asks for the whole scale that comes nearest to laying it out in
 * {@value #TARGET_WIDTH} by {@value #TARGET_HEIGHT} — the size it is designed for, and
 * what a 1080p window gives at a scale of 2 — and puts the player's own scale back the
 * moment the screen closes.
 *
 * <p>It is the player's setting, so three rules come with taking it:
 *
 * <ul>
 *   <li>never by more than one step at a time in practice, and never above what the
 *       window can carry — {@link Window#calculateScale} says what that is;</li>
 *   <li>always restored, in {@code removed()}, which the game calls whenever this
 *       screen goes away for any reason;</li>
 *   <li>re-applied on every layout, because a window resize makes the game recompute
 *       the scale from the options and quietly undo this.</li>
 * </ul>
 */
public final class EditorScale {
    /** The interface the editor is laid out for: a 1080p window at a scale of two. */
    public static final int TARGET_WIDTH = 960;
    public static final int TARGET_HEIGHT = 540;

    /** The player's own scale, kept from the first time this took it. */
    private int playersOwn;

    /**
     * Takes the scale, if it is not already the one the editor wants.
     *
     * @return true when the scale moved, which means whatever size the caller was told
     *     about is now stale
     */
    public boolean apply(Minecraft client) {
        if (client == null) {
            return false;
        }
        Window window = client.getWindow();
        int wanted = wanted(window);
        if (window.getGuiScale() == wanted) {
            return false;
        }
        if (this.playersOwn == 0) {
            this.playersOwn = window.getGuiScale();
        }
        window.setGuiScale(wanted);
        return true;
    }

    /** Gives it back. Called from {@code removed()}, so it happens however the screen ends. */
    public void restore(Minecraft client) {
        if (client == null || this.playersOwn == 0) {
            return;
        }
        Window window = client.getWindow();
        if (window.getGuiScale() != this.playersOwn) {
            window.setGuiScale(this.playersOwn);
        }
        this.playersOwn = 0;
    }

    /**
     * The whole scale that lands nearest the size the editor is designed for.
     *
     * <p>Whichever of width and height runs out first decides, so a wide short window
     * is not handed a scale its height cannot carry. Rounding rather than flooring
     * keeps a 1440p window at 3 — 853 by 480, which is the design size — where flooring
     * would give it 2 and 1280 by 720, an interface of small type in a large room.
     */
    private static int wanted(Window window) {
        double byWidth = window.getWidth() / (double) TARGET_WIDTH;
        double byHeight = window.getHeight() / (double) TARGET_HEIGHT;
        int nearest = (int) Math.round(Math.min(byWidth, byHeight));
        // calculateScale(0, …) is the game's own "auto": the largest scale this window
        // can carry and still hold a menu. Going past it would be asking for a screen
        // the game itself considers too small to draw.
        return Math.max(1, Math.min(nearest, window.calculateScale(0, false)));
    }
}
