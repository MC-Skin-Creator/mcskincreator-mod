/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.preview;

import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A grid of pixels with a clip stack, which is the whole of what this interface needs
 * a graphics library for.
 *
 * <p>Java2D is not used to draw anything here. Everything this screen puts on the
 * glass is a rectangle of one colour or a rectangle of another image sampled at a
 * whole scale, and doing that by hand is both shorter than configuring a
 * {@code Graphics2D} not to interpolate and exactly what the game does — an interface
 * of hard pixels drawn softly would be a preview of a screen nobody has.
 */
final class Raster {
    private final int width;
    private final int height;
    /**
     * Screen pixels to one interface pixel.
     *
     * <p>The preview used to be drawn at interface size and blown up afterwards, which
     * was the same picture and one size too coarse to hold the half-size text: at a
     * scale of two, half a font pixel is a whole screen pixel, and there is nowhere to
     * put it in a buffer that has not got them. So the buffer is the screen's, every
     * call still speaks in interface pixels, and this is the only place the two meet.
     */
    private final int scale;
    private final int[] pixels;
    private final Deque<int[]> clips = new ArrayDeque<>();

    Raster(int width, int height, int scale) {
        this.width = width;
        this.height = height;
        this.scale = Math.max(1, scale);
        this.pixels = new int[width * this.scale * height * this.scale];
        this.clips.push(new int[] {0, 0, width * this.scale, height * this.scale});
    }

    int width() {
        return this.width;
    }

    int height() {
        return this.height;
    }

    int scale() {
        return this.scale;
    }

    private int deviceWidth() {
        return this.width * this.scale;
    }

    private int deviceHeight() {
        return this.height * this.scale;
    }

    /** Clips to the intersection of this rectangle and whatever is already clipped. */
    void pushClip(int x, int y, int width, int height) {
        int[] current = this.clips.peek();
        int left = Math.max(current[0], x * this.scale);
        int top = Math.max(current[1], y * this.scale);
        int right = Math.min(current[0] + current[2], (x + width) * this.scale);
        int bottom = Math.min(current[1] + current[3], (y + height) * this.scale);
        this.clips.push(new int[] {left, top, Math.max(0, right - left), Math.max(0, bottom - top)});
    }

    void popClip() {
        if (this.clips.size() > 1) {
            this.clips.pop();
        }
    }

    private boolean clipped(int x, int y) {
        int[] clip = this.clips.peek();
        return x < clip[0] || y < clip[1] || x >= clip[0] + clip[2] || y >= clip[1] + clip[3];
    }

    /** One interface pixel, which is a square of screen pixels. */
    void blend(int x, int y, int argb) {
        blendUnit(x * this.scale, y * this.scale, this.scale, argb);
    }

    /**
     * A square of screen pixels, placed in screen pixels.
     *
     * <p>What the half-size text needs: its glyph pixels are half an interface pixel
     * across and sit on the screen's grid, not on the interface's.
     */
    void blendUnit(int deviceX, int deviceY, int unit, int argb) {
        for (int row = 0; row < unit; row++) {
            for (int column = 0; column < unit; column++) {
                blendDevice(deviceX + column, deviceY + row, argb);
            }
        }
    }

    private void blendDevice(int x, int y, int argb) {
        if (x < 0 || y < 0 || x >= deviceWidth() || y >= deviceHeight() || clipped(x, y)) {
            return;
        }
        int alpha = (argb >>> 24) & 0xFF;
        if (alpha == 0) {
            return;
        }
        int index = y * deviceWidth() + x;
        if (alpha == 0xFF) {
            this.pixels[index] = argb;
            return;
        }
        int under = this.pixels[index];
        this.pixels[index] = 0xFF000000
                | mix((under >> 16) & 0xFF, (argb >> 16) & 0xFF, alpha) << 16
                | mix((under >> 8) & 0xFF, (argb >> 8) & 0xFF, alpha) << 8
                | mix(under & 0xFF, argb & 0xFF, alpha);
    }

    private static int mix(int under, int over, int alpha) {
        return (over * alpha + under * (0xFF - alpha)) / 0xFF;
    }

    void fill(int x, int y, int width, int height, int argb) {
        // Straight to screen pixels rather than a square at a time: a panel is a
        // million of them and the preview is run on every build.
        int left = x * this.scale;
        int top = y * this.scale;
        for (int row = 0; row < height * this.scale; row++) {
            for (int column = 0; column < width * this.scale; column++) {
                blendDevice(left + column, top + row, argb);
            }
        }
    }

    /**
     * Draws a rectangle of {@code source} into a rectangle of this one, sampling
     * nearest-neighbour and multiplying by a tint.
     *
     * <p>Nearest-neighbour is not a shortcut: it is what the game does, and a smoothed
     * preview would hide exactly the kind of mistake — a sprite stretched off its
     * grid — that this is here to show.
     */
    void draw(BufferedImage source, int x, int y, int width, int height,
              int sourceX, int sourceY, int sourceWidth, int sourceHeight, int tint) {
        if (source == null || width <= 0 || height <= 0 || sourceWidth <= 0 || sourceHeight <= 0) {
            return;
        }
        int left = x * this.scale;
        int top = y * this.scale;
        int deviceW = width * this.scale;
        int deviceH = height * this.scale;
        for (int row = 0; row < deviceH; row++) {
            int sampleY = sourceY + row * sourceHeight / deviceH;
            if (sampleY < 0 || sampleY >= source.getHeight()) {
                continue;
            }
            for (int column = 0; column < deviceW; column++) {
                int sampleX = sourceX + column * sourceWidth / deviceW;
                if (sampleX < 0 || sampleX >= source.getWidth()) {
                    continue;
                }
                blendDevice(left + column, top + row,
                        multiply(source.getRGB(sampleX, sampleY), tint));
            }
        }
    }

    private static int multiply(int argb, int tint) {
        if (tint == 0xFFFFFFFF) {
            return argb;
        }
        return channel(argb, 24, tint) << 24 | channel(argb, 16, tint) << 16
                | channel(argb, 8, tint) << 8 | channel(argb, 0, tint);
    }

    private static int channel(int argb, int shift, int tint) {
        return ((argb >>> shift) & 0xFF) * ((tint >>> shift) & 0xFF) / 0xFF;
    }

    /** The picture, at screen size: what a photograph of that window would show. */
    BufferedImage toImage() {
        int deviceWidth = deviceWidth();
        int deviceHeight = deviceHeight();
        BufferedImage image = new BufferedImage(deviceWidth, deviceHeight,
                BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, deviceWidth, deviceHeight, this.pixels, 0, deviceWidth);
        return image;
    }
}
