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
 * settings at once, and at the scale most people play at it gets 480 by 270 pixels to
 * do that in. The game's font is 8 of those pixels tall — a tenth of the height of a
 * layer row — so everything around it has to be big too, and the result reads as an
 * interface designed for a phone.
 *
 * <p>The font is the floor here and it cannot be lowered: it is a bitmap, and half a
 * pixel of it is not half a letter, it is a broken letter. Drawing the editor smaller
 * within the player's scale would mean scaling by two thirds and mangling every glyph
 * on screen. The only way to fit more interface into the same window is to give the
 * whole window a smaller scale, which is what this does.
 *
 * <h2>Why it asks for a number of rows and not a number of pixels</h2>
 *
 * <p>The first version of this asked for the largest whole scale that still left the
 * editor 1280 by 720, and that was the wrong question. A 1080p window can hold 1280 by
 * 720 at a scale of <em>one</em> — so that is what it was given, and one game pixel
 * became one screen pixel. The editor was laid out correctly and drawn a third of the
 * size it should have been: every label four screen pixels tall, on a 24 inch monitor.
 * Fitting is not the constraint. <strong>How big a pixel ends up</strong> is.
 *
 * <p>So the question asked here is the other way round: of the scales this window can
 * carry, which one leaves the editor nearest {@value #DESIGN_ROWS} rows of its own
 * pixels? More rows than that is not more room, it is the same layout drawn smaller —
 * and past a point, drawn too small to read. Fewer is the layout folding its columns
 * into drawers, which it knows how to do.
 *
 * <p>Height decides it alone. Width only ever varies the amount of scene between the
 * two columns, and the layout handles a narrow one; height is what the stack of layers
 * and the grid of thumbnails are actually competing for.
 *
 * <table border="1">
 *   <caption>What that comes to, on the windows people have</caption>
 *   <tr><th>window</th><th>scale</th><th>the editor's own pixels</th></tr>
 *   <tr><td>1280 x 720</td><td>2</td><td>640 x 360</td></tr>
 *   <tr><td>1366 x 768</td><td>2</td><td>683 x 384</td></tr>
 *   <tr><td>1600 x 900</td><td>2</td><td>800 x 450</td></tr>
 *   <tr><td>1920 x 1080</td><td>2</td><td>960 x 540</td></tr>
 *   <tr><td>1920 x 1200</td><td>2</td><td>960 x 600</td></tr>
 *   <tr><td>2560 x 1440</td><td>3</td><td>853 x 480</td></tr>
 *   <tr><td>3840 x 2160</td><td>4</td><td>960 x 540</td></tr>
 * </table>
 *
 * <p>It is the player's setting, so three rules come with taking it:
 *
 * <ul>
 *   <li>never above what the window can carry — {@link Window#calculateScale} says what
 *       that is, and it is the game's own idea of what this window can hold;</li>
 *   <li>always restored, in {@code removed()}, which the game calls whenever this
 *       screen goes away for any reason;</li>
 *   <li>re-applied on every layout, because a window resize makes the game recompute
 *       the scale from the options and quietly undo this.</li>
 * </ul>
 */
public final class EditorScale {
    /**
     * The number of its own rows the editor is laid out for.
     *
     * <p>540 is a 1080p window at a scale of two, and it is the size every picture in
     * {@code build/ui-preview/} is checked at. It is a target and not a minimum: the
     * scale nearest it wins, so a window that cannot divide down to 540 rows gets the
     * closest it can rather than nothing.
     */
    public static final int DESIGN_ROWS = 540;

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
        int wanted = scaleFor(window.getHeight(), window.calculateScale(0, false));
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
     * The whole scale that leaves the editor nearest {@link #DESIGN_ROWS} rows.
     *
     * <p>Every scale the window can carry is tried and the nearest wins, because the
     * answer is not monotonic in any one direction: 768 screen rows are better halved
     * to 384 than left whole at 768, and 1200 are better halved to 600 than cut to 400.
     * A tie goes to the larger scale — the same distance from the target, in bigger
     * pixels.
     *
     * <p>Split out from the window so it can be checked against a table rather than a
     * running game. The bug this replaces was a one-line arithmetic mistake that no
     * test could have caught, because there was nothing to call.
     *
     * @param windowHeight the window's height in screen pixels
     * @param autoScale what the game itself would pick for this window
     */
    public static int scaleFor(int windowHeight, int autoScale) {
        int highest = Math.max(1, autoScale);
        int best = 1;
        int bestDistance = Integer.MAX_VALUE;
        for (int scale = 1; scale <= highest; scale++) {
            int distance = Math.abs(windowHeight / scale - DESIGN_ROWS);
            // Not strictly less: a tie is resolved in favour of the later, larger scale.
            if (distance <= bestDistance) {
                bestDistance = distance;
                best = scale;
            }
        }
        return best;
    }
}
