/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui.window;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import fr.clixmods.mcsc.mod.style.Metrics;
import fr.clixmods.mcsc.mod.style.Palette;
import fr.clixmods.mcsc.mod.style.Surface;
import fr.clixmods.mcsc.mod.ui.Canvas;
import fr.clixmods.mcsc.mod.ui.Element;
import fr.clixmods.mcsc.mod.ui.Paint;
import fr.clixmods.mcsc.mod.ui.ScrollPane;
import fr.clixmods.mcsc.mod.ui.widget.PixelButton;
import net.minecraft.network.chat.Component;

/**
 * One window at a time, over a dimmed screen.
 *
 * <p>Opening a window replaces whatever was showing, and a window can say what its
 * closing should bring back: shutting the provenance sheet opened from the templates
 * list returns to the list rather than to a bare screen.
 *
 * <p>Closing by clicking the backdrop only counts when the gesture <em>started</em> on
 * the backdrop. Otherwise selecting text in a field and letting go outside the window
 * closes it and throws the typing away, which is the kind of loss nobody reports as a
 * bug because it looks like their own mistake.
 */
public abstract class ModalWindow {
    private final String titleKey;
    private final ModalWindow returnsTo;
    /** The frame's own controls: the close cross, the footer. They never scroll. */
    private final List<Element> chrome = new ArrayList<>();
    /** The body's controls, which move with the scroll and are clipped to it. */
    private final List<Element> body = new ArrayList<>();
    private final ScrollPane scroll = new ScrollPane();

    private PixelButton closeButton;
    private int x;
    private int y;
    private int width;
    private int height;
    private int bodyTop;
    private int bodyHeight;
    private boolean pressStartedOnBackdrop;

    protected ModalWindow(String titleKey, ModalWindow returnsTo) {
        this.titleKey = titleKey;
        this.returnsTo = returnsTo;
    }

    /** The window to show once this one closes, or null to return to the screen. */
    public ModalWindow returnsTo() {
        return this.returnsTo;
    }

    protected int x() {
        return this.x;
    }

    protected int y() {
        return this.y;
    }

    protected int width() {
        return this.width;
    }

    protected int bodyTop() {
        return this.bodyTop;
    }

    protected int bodyHeight() {
        return this.bodyHeight;
    }

    protected int scrollOffset() {
        return this.scroll.offset();
    }

    /** Adds one of the frame's own controls. */
    protected <T extends Element> T addChild(T child) {
        this.chrome.add(child);
        return child;
    }

    /**
     * Adds a control that belongs to the body.
     *
     * <p>Body controls are clipped to the body and, once scrolled out of it, are left
     * out of {@link #children()} entirely — not merely hidden. Invisible but still
     * clickable is how people press buttons they never saw.
     */
    protected <T extends Element> T addBodyChild(T child) {
        this.body.add(child);
        return child;
    }

    protected void clearChildren() {
        this.chrome.clear();
        this.body.clear();
    }

    /** Everything that can be clicked or focused, scrolled-away body controls aside. */
    public List<Element> children() {
        List<Element> reachable = new ArrayList<>(this.chrome);
        for (Element child : this.body) {
            if (inBody(child)) {
                reachable.add(child);
            }
        }
        return reachable;
    }

    private boolean inBody(Element child) {
        return child.y() + child.height() > this.bodyTop
                && child.y() < this.bodyTop + this.bodyHeight;
    }

    /** How tall the body wants to be; the window clamps it and scrolls the rest. */
    protected abstract int contentHeight(Canvas canvas);

    /** Lays the body out, with {@code top} already in screen coordinates. */
    protected abstract void layoutBody(Canvas canvas, int left, int top, int width);

    protected abstract void drawBody(Paint paint, int left, int top, int width);

    /** The footer buttons, right aligned, the primary one last. */
    protected List<PixelButton> footer(Canvas canvas, Runnable close) {
        return List.of();
    }

    /**
     * What should hold the keyboard the moment the window opens, or null for nothing.
     *
     * <p>A window that asks for one thing should be ready to be typed into and
     * answered with Enter, without a click first.
     */
    public Element initialFocus() {
        return null;
    }

    /** A window reopened starts at the top of its scroll, never where it was left. */
    public void reset() {
        this.scroll.reset();
    }

    public void layout(Canvas canvas, int screenWidth, int screenHeight, Runnable close) {
        clearChildren();

        int headerHeight = canvas.lineHeight() + Metrics.PAD * 2;
        int footerHeight = Metrics.BUTTON_HEIGHT + Metrics.PAD * 2;
        List<PixelButton> footer = footer(canvas, close);
        if (footer.isEmpty()) {
            footerHeight = Metrics.PAD;
        }

        this.width = Math.min(Metrics.WINDOW_MAX_WIDTH, screenWidth - Metrics.PAD * 4);
        int maximum = (int) (screenHeight * Metrics.WINDOW_MAX_HEIGHT_RATIO);
        int wanted = headerHeight + contentHeight(canvas) + footerHeight + Metrics.PAD * 2;
        this.height = Math.min(maximum, wanted);
        this.x = (screenWidth - this.width) / 2;
        this.y = (screenHeight - this.height) / 2;

        this.bodyTop = this.y + headerHeight + Metrics.PAD_TIGHT;
        this.bodyHeight = this.height - headerHeight - footerHeight - Metrics.PAD_TIGHT;
        this.scroll.setContent(contentHeight(canvas), this.bodyHeight);

        this.closeButton = new PixelButton(Component.literal("x"), PixelButton.Style.GHOST, close);
        this.closeButton.fit(canvas);
        this.closeButton.withTooltip(Component.translatable("gui.mcskincreator.close.tooltip"));
        this.closeButton.setBounds(this.x + this.width - Metrics.PAD - this.closeButton.width(),
                this.y + (headerHeight - Metrics.BUTTON_HEIGHT) / 2,
                this.closeButton.width(), Metrics.BUTTON_HEIGHT);
        addChild(this.closeButton);

        int left = this.x + Metrics.PAD;
        layoutBody(canvas, left, this.bodyTop - this.scroll.offset(), this.width - Metrics.PAD * 2);

        int cursorX = this.x + this.width - Metrics.PAD;
        List<PixelButton> reversed = new ArrayList<>(footer);
        java.util.Collections.reverse(reversed);
        for (PixelButton button : reversed) {
            button.fit(canvas);
            cursorX -= button.width();
            button.setBounds(cursorX, this.y + this.height - Metrics.PAD - Metrics.BUTTON_HEIGHT,
                    button.width(), Metrics.BUTTON_HEIGHT);
            addChild(button);
            cursorX -= Metrics.PAD_TIGHT;
        }
    }

    public void draw(Paint paint, int screenWidth, int screenHeight) {
        Canvas canvas = paint.canvas();
        canvas.fill(0, 0, screenWidth, screenHeight, Palette.BACKDROP);
        Surface.window(canvas, this.x, this.y, this.width, this.height);

        int headerHeight = canvas.lineHeight() + Metrics.PAD * 2;
        Surface.flat(canvas, this.x + Metrics.OUTLINE, this.y + Metrics.OUTLINE,
                this.width - Metrics.OUTLINE * 2, headerHeight - Metrics.OUTLINE, Palette.PANEL_HEADER);
        String title = Component.translatable(this.titleKey).getString().toUpperCase(Locale.ROOT);
        canvas.textTracked(title, this.x + Metrics.PAD,
                this.y + (headerHeight - canvas.lineHeight()) / 2,
                Palette.INK, Metrics.TITLE_TRACKING);

        canvas.pushScissor(this.x, this.bodyTop, this.width, this.bodyHeight);
        drawBody(paint, this.x + Metrics.PAD, this.bodyTop - this.scroll.offset(),
                this.width - Metrics.PAD * 2);
        for (Element child : this.body) {
            if (inBody(child)) {
                child.draw(paint);
            }
        }
        canvas.popScissor();
        this.scroll.drawBar(canvas, this.x + this.width - Metrics.OUTLINE, this.bodyTop, this.bodyHeight);

        for (Element child : this.chrome) {
            child.draw(paint);
        }
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= this.x && mouseX < this.x + this.width
                && mouseY >= this.y && mouseY < this.y + this.height;
    }

    /** @return true when the press was on the backdrop and should close on release */
    public boolean pressOutside(double mouseX, double mouseY) {
        this.pressStartedOnBackdrop = !contains(mouseX, mouseY);
        return this.pressStartedOnBackdrop;
    }

    public boolean releaseClosesWindow(double mouseX, double mouseY) {
        boolean closes = this.pressStartedOnBackdrop && !contains(mouseX, mouseY);
        this.pressStartedOnBackdrop = false;
        return closes;
    }

    public boolean scroll(double amount) {
        return this.scroll.scroll(amount);
    }
}
