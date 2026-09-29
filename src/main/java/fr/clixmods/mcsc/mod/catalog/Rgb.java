/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.catalog;

import java.util.Locale;
import java.util.OptionalInt;

/**
 * A colour of the skin, as the catalogue and the project write it: {@code #rrggbb}.
 *
 * <p>Held as a plain {@code 0xRRGGBB} int. The server's validator refuses anything
 * else under a layer's {@code colors} — a name, a short {@code #rgb}, an alpha — so
 * this reads exactly that and writes exactly that.
 *
 * <p>The HSL conversion is what the colour window's three sliders move through. It
 * works in whole degrees and whole percentages because that is what a slider holds,
 * and the round trip lands within one step of the colour it started from.
 */
public final class Rgb {
    private Rgb() {
    }

    /** {@code #rrggbb}, the leading hash optional; anything else is empty. */
    public static OptionalInt parse(String text) {
        if (text == null) {
            return OptionalInt.empty();
        }
        String hex = text.trim();
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        if (hex.length() != 6) {
            return OptionalInt.empty();
        }
        for (int index = 0; index < hex.length(); index++) {
            if (Character.digit(hex.charAt(index), 16) < 0) {
                return OptionalInt.empty();
            }
        }
        return OptionalInt.of(Integer.parseInt(hex, 16));
    }

    /** {@code #rrggbb}, lower case, which is the only form the server accepts. */
    public static String format(int rgb) {
        return String.format(Locale.ROOT, "#%06x", rgb & 0xFFFFFF);
    }

    /** The colour, opaque, for drawing. */
    public static int argb(int rgb) {
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }

    /** @return hue in degrees 0..359, saturation and lightness in percent 0..100 */
    public static int[] toHsl(int rgb) {
        double red = ((rgb >> 16) & 0xFF) / 255.0;
        double green = ((rgb >> 8) & 0xFF) / 255.0;
        double blue = (rgb & 0xFF) / 255.0;
        double max = Math.max(red, Math.max(green, blue));
        double min = Math.min(red, Math.min(green, blue));
        double lightness = (max + min) / 2;
        double delta = max - min;

        double hue = 0;
        double saturation = 0;
        if (delta > 0) {
            saturation = delta / (1 - Math.abs(2 * lightness - 1));
            if (max == red) {
                hue = 60 * (((green - blue) / delta) % 6);
            } else if (max == green) {
                hue = 60 * ((blue - red) / delta + 2);
            } else {
                hue = 60 * ((red - green) / delta + 4);
            }
        }
        int wholeHue = (int) Math.round(hue < 0 ? hue + 360 : hue) % 360;
        return new int[] {wholeHue, (int) Math.round(saturation * 100), (int) Math.round(lightness * 100)};
    }

    /** The inverse of {@link #toHsl}, clamping whatever it is handed. */
    public static int fromHsl(int hue, int saturation, int lightness) {
        double h = ((hue % 360) + 360) % 360;
        double s = Math.max(0, Math.min(100, saturation)) / 100.0;
        double l = Math.max(0, Math.min(100, lightness)) / 100.0;

        double chroma = (1 - Math.abs(2 * l - 1)) * s;
        double x = chroma * (1 - Math.abs((h / 60) % 2 - 1));
        double m = l - chroma / 2;
        double red;
        double green;
        double blue;
        if (h < 60) {
            red = chroma; green = x; blue = 0;
        } else if (h < 120) {
            red = x; green = chroma; blue = 0;
        } else if (h < 180) {
            red = 0; green = chroma; blue = x;
        } else if (h < 240) {
            red = 0; green = x; blue = chroma;
        } else if (h < 300) {
            red = x; green = 0; blue = chroma;
        } else {
            red = chroma; green = 0; blue = x;
        }
        return channel(red + m) << 16 | channel(green + m) << 8 | channel(blue + m);
    }

    private static int channel(double value) {
        return (int) Math.round(Math.max(0, Math.min(1, value)) * 255);
    }
}
