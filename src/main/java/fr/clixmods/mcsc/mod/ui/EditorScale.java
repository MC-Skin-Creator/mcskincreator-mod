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
 * <p>So the question asked here is the other way round: which is the largest scale this
 * window can carry that still leaves the editor {@value #DESIGN_ROWS} rows of its own
 * pixels? More rows than that is not more room, it is the same layout drawn smaller —
 * and past a point, drawn too small to read. Fewer is the layout folding its columns
 * into drawers, which it knows how to do.
 *
 * <p>And the scale is <strong>even</strong>, which is the other half of the answer to
 * "the text is too big". The editor draws its secondary text at half the font's size,
 * and half of an even scale is a whole screen pixel: the bitmap lands on the grid
 * exactly. An odd scale would smear every one of those letters, so an odd scale is not
 * taken, whatever the extra rows would have been worth.
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
 *   <tr><td>2560 x 1440</td><td>2</td><td>1280 x 720</td></tr>
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
        // The scale is a double before 1.21.9 and an int from it; it only ever holds a
        // whole number, so the cast reads the same value on every target.
        if ((int) window.getGuiScale() == wanted) {
            return false;
        }
        if (this.playersOwn == 0) {
            this.playersOwn = (int) window.getGuiScale();
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
        if ((int) window.getGuiScale() != this.playersOwn) {
            window.setGuiScale(this.playersOwn);
        }
        this.playersOwn = 0;
    }

    /**
     * The largest <strong>even</strong> scale that still leaves {@link #DESIGN_ROWS}
     * rows; failing that, two; failing that, whatever the window can carry.
     *
     * <p>Even is the whole point. The editor's small text is the game's font at a half,
     * and a half of an even scale is a whole screen pixel — the bitmap lands on the grid
     * and the letters are as sharp as the screen can draw them. At an odd scale the same
     * text falls between pixels and smears, so the scale that would have been picked for
     * a few more rows is not worth the text it ruins.
     *
     * <p>Failing that, two. A window too short to give the editor its rows at a scale of
     * two is a window the layout has to fold for anyway, and folding at a readable size
     * beats three columns nobody can read: 384 rows at two is a 1366x768 laptop, which
     * is a small screen and not a broken one.
     *
     * <p>Split out from the window so it can be checked against a table rather than a
     * running game. The bug this replaces was a one-line arithmetic mistake that no test
     * could have caught, because there was nothing to call.
     *
     * @param windowHeight the window's height in screen pixels
     * @param autoScale what the game itself would pick for this window
     */
    public static int scaleFor(int windowHeight, int autoScale) {
        int highest = Math.max(1, autoScale);
        int best = 0;
        for (int scale = 2; scale <= highest; scale += 2) {
            if (windowHeight / scale >= DESIGN_ROWS) {
                best = scale;
            }
        }
        if (best == 0) {
            best = 2;
        }
        // Never past what the game says this window holds, which on a very small one
        // leaves the odd scale of one — and there the text is soft, because there is
        // nothing else to be done with a window that size.
        return Math.max(1, Math.min(best, highest));
    }
}
