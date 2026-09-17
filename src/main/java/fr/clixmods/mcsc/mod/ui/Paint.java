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
 */
public record Paint(Canvas canvas, int mouseX, int mouseY, long time, Element focused) {

    /** True when the element is under the pointer or holds the keyboard focus. */
    public boolean hot(Element element) {
        return element.enabled() && (element.contains(this.mouseX, this.mouseY) || this.focused == element);
    }

    /** True when the pointer is inside the rectangle — for areas that are not elements. */
    public boolean over(int x, int y, int width, int height) {
        return this.mouseX >= x && this.mouseX < x + width
                && this.mouseY >= y && this.mouseY < y + height;
    }
}
