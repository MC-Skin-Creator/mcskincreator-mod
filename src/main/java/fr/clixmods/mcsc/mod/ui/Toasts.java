/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import fr.clixmods.mcsc.mod.style.Metrics;
import fr.clixmods.mcsc.mod.style.Palette;
import fr.clixmods.mcsc.mod.style.Surface;
import net.minecraft.network.chat.Component;

/**
 * Notifications: a stone panel low on the screen, for two and a half seconds.
 *
 * <p>The panel is the game's. Whether it went well is read off the band down the left
 * flank — green or red — which is the one thing the site does that the game has no
 * sprite for, and the only colour of the mod's own left in the interface.
 *
 * <p>A failure that keeps happening is said once. {@link #failed} will not raise the
 * same complaint twice until {@link #succeeded} has been called for it, because a red
 * banner every two seconds stops being a warning and becomes something to click past.
 * The window that caused it keeps its own message for as long as the problem lasts —
 * that is where someone looks when they want to know what is still wrong.
 */
public final class Toasts {
    private final Deque<Toast> live = new ArrayDeque<>();
    private final Set<String> silenced = new HashSet<>();

    private record Toast(Component text, boolean ok, long shownAt) {
    }

    public void ok(Component text) {
        this.live.addLast(new Toast(text, true, System.currentTimeMillis()));
    }

    /**
     * Reports a failure, once.
     *
     * @param kind what went wrong, so two different failures are not confused for a
     *             repeat of the same one
     */
    public void failed(String kind, Component text) {
        if (!this.silenced.add(kind)) {
            return;
        }
        this.live.addLast(new Toast(text, false, System.currentTimeMillis()));
    }

    /** Arms the failure notice again: the thing that was failing has now worked. */
    public void succeeded(String kind) {
        this.silenced.remove(kind);
    }

    public void draw(Canvas canvas, int screenWidth, int screenHeight, long now) {
        this.live.removeIf(toast -> now - toast.shownAt() > Metrics.TOAST_MILLIS);

        int index = 0;
        for (Toast toast : this.live) {
            int height = canvas.lineHeight() + Metrics.PANEL_INSET * 2;
            int width = canvas.textWidth(toast.text()) + Metrics.BAND
                    + Metrics.PANEL_INSET * 2 + Metrics.PAD_TIGHT * 2;
            int x = (screenWidth - width) / 2;
            int y = screenHeight - Metrics.PAD * 3 - height - index * (height + Metrics.PAD_TIGHT);

            Surface.panel(canvas, x, y, width, height);
            canvas.fill(x + Metrics.PANEL_INSET, y + Metrics.PANEL_INSET,
                    Metrics.BAND, height - Metrics.PANEL_INSET * 2,
                    toast.ok() ? Palette.BAND_SUCCESS : Palette.BAND_FAILURE);
            canvas.text(toast.text(), x + Metrics.PANEL_INSET + Metrics.BAND + Metrics.PAD_TIGHT,
                    y + (height - canvas.lineHeight()) / 2,
                    toast.ok() ? Palette.INK : Palette.INK_FAILURE);
            index++;
        }
    }
}
