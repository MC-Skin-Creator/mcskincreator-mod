/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui;

import java.util.List;

import net.minecraft.network.chat.Component;

/**
 * Base of every control in this interface.
 *
 * <p>The mod draws its own widgets rather than dressing up the vanilla ones: the
 * site's look is a material and a bevel, not a skin over a button, and half of these
 * controls — a category tab, an element thumbnail, a layer row — have no vanilla
 * equivalent to dress. What the game keeps is the mechanics it is right about: font
 * size, GUI scale, and a focus ring that a keyboard can walk.
 *
 * <p>A screen owns an ordered list of these, walks it for focus, and hit-tests it
 * back to front. An element that is not visible is not hit-tested either — the site
 * learned that the hard way with layer actions, where invisible meant still
 * clickable and people deleted layers they could not see.
 */
public abstract class Element {
    protected int x;
    protected int y;
    protected int width;
    protected int height;

    private boolean enabled = true;
    private boolean visible = true;
    private boolean focusable = true;
    /** The band this element is allowed to be clicked in, or null for all of it. */
    private int[] clip;

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public int x() {
        return this.x;
    }

    public int y() {
        return this.y;
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }

    public boolean enabled() {
        return this.enabled && this.visible;
    }

    public Element setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public boolean visible() {
        return this.visible;
    }

    public Element setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }

    public boolean focusable() {
        return this.focusable && enabled();
    }

    public Element setFocusable(boolean focusable) {
        this.focusable = focusable;
        return this;
    }

    /**
     * Limits where the element answers to a click.
     *
     * <p>Scrolling content is drawn clipped to its band, and half a tile hanging below
     * that band is drawn as half a tile — but it was still a whole tile to the pointer,
     * so a click under the panel picked an element nobody could see. What is clipped
     * away is not merely invisible, it is out of reach.
     *
     * @return this, so a caller can place and clip in one line
     */
    public Element clipTo(int x, int y, int width, int height) {
        this.clip = new int[] {x, y, width, height};
        return this;
    }

    /** The rectangle the element actually answers to: its bounds, clipped. */
    public int[] hitBox() {
        if (this.clip == null) {
            return new int[] {this.x, this.y, this.width, this.height};
        }
        int left = Math.max(this.x, this.clip[0]);
        int top = Math.max(this.y, this.clip[1]);
        int right = Math.min(this.x + this.width, this.clip[0] + this.clip[2]);
        int bottom = Math.min(this.y + this.height, this.clip[1] + this.clip[3]);
        return new int[] {left, top, Math.max(0, right - left), Math.max(0, bottom - top)};
    }

    /** Hit test. A hidden or disabled element is never hit, only drawn or not drawn. */
    public boolean contains(double mouseX, double mouseY) {
        if (!enabled()) {
            return false;
        }
        int[] box = hitBox();
        return mouseX >= box[0] && mouseX < box[0] + box[2]
                && mouseY >= box[1] && mouseY < box[1] + box[3];
    }

    public abstract void draw(Paint paint);

    /** @return true when the element took the click */
    public boolean mouseDown(double mouseX, double mouseY, int button) {
        return false;
    }

    public void mouseDrag(double mouseX, double mouseY, double dragX, double dragY, int button) {
    }

    public void mouseUp(double mouseX, double mouseY, int button) {
    }

    /** @return true when the element consumed the scroll */
    public boolean scroll(double mouseX, double mouseY, double amount) {
        return false;
    }

    /** @return true when the element consumed the key */
    public boolean keyDown(int key, int modifiers) {
        return false;
    }

    /** @return true when the element consumed the character */
    public boolean charTyped(int codepoint) {
        return false;
    }

    /** Enter or Space on a focused element. */
    public boolean activate() {
        return false;
    }

    /**
     * What the tooltip says, or an empty list for none.
     *
     * <p>A tooltip says what the control does, never what it is called: the label
     * already says that. "New" is explained by "start over", not by "new".
     */
    public List<Component> tooltip() {
        return List.of();
    }

    /** True while the element is editing text, so shortcuts must stay out of the way. */
    public boolean capturesTyping() {
        return false;
    }

    /**
     * True while the element is showing something over the rest of the screen — an
     * open dropdown, for instance.
     *
     * <p>A screen draws every overlay after everything else and hit-tests them first,
     * so a menu cannot be painted over by the panel it belongs to, and a click on the
     * menu cannot fall through to whatever is behind it.
     */
    public boolean overlayActive() {
        return false;
    }

    public void drawOverlay(Paint paint) {
    }

    /** Whether a click at this point belongs to the overlay rather than the screen. */
    public boolean overlayContains(double mouseX, double mouseY) {
        return false;
    }

    /** Closes the overlay. Escape closes this and nothing else — not the window. */
    public void closeOverlay() {
    }

    /**
     * Gives up whatever the element was holding on to — the cursor in a field, above
     * all — because the click went somewhere else.
     *
     * <p>Without this a field keeps the caret after a button is pressed, goes on
     * swallowing every keystroke, and the shortcuts stop working with nothing on screen
     * to explain why.
     */
    public void blur() {
    }
}
