/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui.window;

import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;

import fr.clixmods.mcsc.mod.catalog.Rgb;
import fr.clixmods.mcsc.mod.project.History;
import fr.clixmods.mcsc.mod.project.Layer;
import fr.clixmods.mcsc.mod.project.SkinProject;
import fr.clixmods.mcsc.mod.style.Metrics;
import fr.clixmods.mcsc.mod.style.Palette;
import fr.clixmods.mcsc.mod.style.Surface;
import fr.clixmods.mcsc.mod.ui.Canvas;
import fr.clixmods.mcsc.mod.ui.Element;
import fr.clixmods.mcsc.mod.ui.Marquee;
import fr.clixmods.mcsc.mod.ui.Paint;
import fr.clixmods.mcsc.mod.ui.widget.PixelButton;
import fr.clixmods.mcsc.mod.ui.widget.Slider;
import fr.clixmods.mcsc.mod.ui.widget.TextInput;
import net.minecraft.network.chat.Component;

/**
 * Choosing the colour of one key of one layer.
 *
 * <p>Hue, saturation and lightness rather than three channels, because that is how a
 * colour is looked for — "the same green, darker" is one slider here and three in RGB.
 * The hexadecimal field is there for the other way round: a colour copied from the
 * site, or from anywhere else, pasted in whole.
 *
 * <p>The colour is applied while it is being chosen, so the model shows it as the
 * sliders move; composing is debounced by the screen, so a drag costs one request, not
 * one per frame. The window is kept in its own three numbers rather than read back from
 * the colour each time: a grey has no hue, and a hue slider that jumped to zero the
 * moment saturation touched nothing would lose what the player was holding.
 *
 * <p>Everything done in the window is one entry in the history, recorded on the first
 * change and not before, so opening a window and closing it leaves no step behind.
 * Cancel puts the colour back as it was when the window opened.
 */
public class ColorWindow extends ModalWindow {
    private static final int MAX_HEX_CHARS = 7;

    private final SkinProject project;
    private final History history;
    private final Layer layer;
    private final String key;
    private final Component keyName;
    private final int opening;
    private final int[] hsl;
    private final TextInput hex;

    private boolean recorded;
    /** True while the field is being rewritten from the sliders, so it does not answer itself. */
    private boolean syncing;

    public ColorWindow(SkinProject project, History history, Layer layer, String key) {
        super("window.mcskincreator.color", null);
        this.project = project;
        this.history = history;
        this.layer = layer;
        this.key = key;
        this.keyName = keyName(key);
        this.opening = layer.color(key);
        this.hsl = Rgb.toHsl(this.opening);
        this.hex = new TextInput(Component.literal(Rgb.format(this.opening)), MAX_HEX_CHARS,
                this::typed);
        syncField();
    }

    /**
     * What a colour key is called, in the player's language.
     *
     * <p>The names are the site's own table, key for key. A key the table does not name
     * yet is shown as it is written in the catalogue rather than hidden: it can still be
     * recoloured, and a raw name is better than a swatch nobody can tell apart.
     */
    public static Component keyName(String key) {
        return Component.translatableWithFallback("color.mcskincreator." + key, key);
    }

    private void typed(String text) {
        if (this.syncing) {
            return;
        }
        Rgb.parse(text).ifPresent(rgb -> {
            int[] read = Rgb.toHsl(rgb);
            System.arraycopy(read, 0, this.hsl, 0, 3);
            write(rgb);
        });
    }

    private void recordOnce() {
        if (!this.recorded) {
            this.recorded = true;
            this.history.record();
        }
    }

    private void write(int rgb) {
        if (this.layer.color(this.key) == rgb) {
            return;
        }
        recordOnce();
        this.layer.setColor(this.key, rgb);
        this.project.touch();
    }

    private void fromSliders() {
        write(Rgb.fromHsl(this.hsl[0], this.hsl[1], this.hsl[2]));
        syncField();
    }

    private void syncField() {
        this.syncing = true;
        this.hex.setValue(Rgb.format(this.layer.color(this.key)));
        this.syncing = false;
    }

    @Override
    protected int contentHeight(Canvas canvas) {
        return canvas.lineHeight() + Metrics.PAD
                + Metrics.BUTTON_HEIGHT_COMPACT + Metrics.PAD
                + (Slider.heightFor(canvas) + Metrics.PAD_TIGHT) * 3;
    }

    @Override
    protected void layoutBody(Canvas canvas, int left, int top, int width) {
        int cursorY = top + canvas.lineHeight() + Metrics.PAD;

        // The swatch is drawn, not laid out: it is the colour, not a control.
        int fieldLeft = left + Metrics.BUTTON_HEIGHT_COMPACT * 2 + Metrics.PAD;
        this.hex.setBounds(fieldLeft, cursorY, Math.max(0, left + width - fieldLeft),
                Metrics.BUTTON_HEIGHT_COMPACT);
        addBodyChild(this.hex);
        cursorY += Metrics.BUTTON_HEIGHT_COMPACT + Metrics.PAD;

        cursorY = slider(canvas, left, cursorY, width, "hue", 0, 359,
                () -> this.hsl[0], value -> this.hsl[0] = value, value -> Component.literal(value + "°"));
        cursorY = slider(canvas, left, cursorY, width, "saturation", 0, 100,
                () -> this.hsl[1], value -> this.hsl[1] = value, value -> Component.literal(value + "%"));
        slider(canvas, left, cursorY, width, "lightness", 0, 100,
                () -> this.hsl[2], value -> this.hsl[2] = value, value -> Component.literal(value + "%"));
    }

    private int slider(Canvas canvas, int left, int top, int width, String id,
                       int minimum, int maximum, IntSupplier read, IntConsumer write,
                       IntFunction<Component> format) {
        Slider slider = new Slider(Component.translatable("setting.mcskincreator." + id),
                minimum, maximum, read,
                value -> {
                    write.accept(value);
                    fromSliders();
                },
                format, () -> { }, () -> { });
        slider.setBounds(left, top, width, Slider.heightFor(canvas));
        addBodyChild(slider);
        return top + Slider.heightFor(canvas) + Metrics.PAD_TIGHT;
    }

    @Override
    protected void drawBody(Paint paint, int left, int top, int width) {
        Canvas canvas = paint.canvas();
        String name = this.keyName.getString();
        canvas.text(Component.literal(name), left, top, Palette.INK);
        int nameWidth = canvas.textWidth(name);
        int room = Math.max(0, width - nameWidth - Metrics.PAD);
        canvas.text(Component.literal(Marquee.cut(canvas, this.layer.name().getString(), room)),
                left + nameWidth + Metrics.PAD, top, Palette.INK_MUTED);

        // Before and after, side by side: the colour the window opened on, and the one
        // it would leave.
        int swatchY = top + canvas.lineHeight() + Metrics.PAD;
        int size = Metrics.BUTTON_HEIGHT_COMPACT;
        swatch(canvas, left, swatchY, size, this.opening);
        swatch(canvas, left + size, swatchY, size, this.layer.color(this.key));
    }

    private static void swatch(Canvas canvas, int x, int y, int size, int rgb) {
        Surface.slot(canvas, x, y, size, size);
        int inset = Metrics.SLOT_INSET + 1;
        canvas.fill(x + inset, y + inset, size - inset * 2, size - inset * 2, Rgb.argb(rgb));
    }

    @Override
    public Element initialFocus() {
        return null;
    }

    @Override
    protected List<PixelButton> footer(Canvas canvas, Runnable close) {
        PixelButton original = new PixelButton(Component.translatable("gui.mcskincreator.color_original"),
                PixelButton.Style.NORMAL, () -> {
                    int rgb = this.layer.defaultColor(this.key);
                    System.arraycopy(Rgb.toHsl(rgb), 0, this.hsl, 0, 3);
                    write(rgb);
                    syncField();
                });
        original.withTooltip(Component.translatable("gui.mcskincreator.color_original.tooltip"));
        return List.of(
                original,
                new PixelButton(Component.translatable("gui.mcskincreator.cancel"),
                        PixelButton.Style.NORMAL, () -> {
                            if (this.layer.color(this.key) != this.opening) {
                                this.layer.setColor(this.key, this.opening);
                                this.project.touch();
                            }
                            close.run();
                        }),
                new PixelButton(Component.translatable("gui.mcskincreator.confirm"),
                        PixelButton.Style.NORMAL, close));
    }
}
