/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui;

/**
 * Everything an element needs to know to draw one frame of itself.
 *
 * <p>The focused element is in here for one reason: hovering exists with a mouse and
 * not with a controller or a keyboard, so anything the site reveals on hover has to
 * be reachable by focus too. {@link #hot} answers "should this look lit up" once, and
 * every widget asks that instead of asking whether the mouse is over it.
 *
 * <p>{@code blocked} is the other half of that. While a menu is open over the screen
 * the pointer belongs to the menu, whatever it happens to be sitting on top of — and
 * a frame drawn without knowing that lights up the layer row behind the menu, shows
 * its tooltip, and turns the model to follow a cursor that is not aiming at it. The
 * screen sets it for the frame and hands the menu itself an {@link #unblocked} copy,
 * so the pointer stops at the top layer instead of falling through every one of them.
 *
 * @param blocked true while something is open over the screen and the pointer is its own
 */
public record Paint(Canvas canvas, int mouseX, int mouseY, long time, Element focused,
                    boolean blocked) {

    public Paint(Canvas canvas, int mouseX, int mouseY, long time, Element focused) {
        this(canvas, mouseX, mouseY, time, focused, false);
    }

    /** True when the element is under the pointer or holds the keyboard focus. */
    public boolean hot(Element element) {
        if (this.blocked) {
            return false;
        }
        return element.enabled()
                && (element.contains(this.mouseX, this.mouseY) || this.focused == element);
    }

    /** True when the pointer is inside the rectangle — for areas that are not elements. */
    public boolean over(int x, int y, int width, int height) {
        return !this.blocked
                && this.mouseX >= x && this.mouseX < x + width
                && this.mouseY >= y && this.mouseY < y + height;
    }

    /**
     * The same frame with the pointer let through: what whatever is on top draws with.
     *
     * <p>An open menu is the one thing the pointer is really on, so it asks the same
     * questions as any other frame and gets real answers.
     */
    public Paint unblocked() {
        return new Paint(this.canvas, this.mouseX, this.mouseY, this.time, this.focused, false);
    }
}
