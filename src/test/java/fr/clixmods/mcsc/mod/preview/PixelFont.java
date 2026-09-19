/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.preview;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * The game's own font, read from the same textures the client reads.
 *
 * <p>This matters more than it looks. Every width in this interface is a width the
 * font decided — how wide a button is, whether a label fits its slot, where a row of
 * tabs wraps — so a preview drawn with an approximation of the font would be a preview
 * of a layout that does not exist. The advances here are computed the way
 * {@code BitmapProvider} computes them: the rightmost pixel of the glyph, scaled to
 * the provider's height, plus the one pixel of air between letters.
 *
 * <p>Only the bitmap and space providers of {@code font/default} are read. They are
 * what the Latin alphabets use; a codepoint none of them carries falls back to a blank
 * six pixels wide, which is what the unicode fallback would have been.
 */
final class PixelFont {
    /** A line of the game's font, which is eight pixels of glyph and one of air. */
    static final int LINE_HEIGHT = 9;
    /** Where the baseline sits in a line, and what every provider's ascent is measured against. */
    private static final int BASELINE = 7;
    private static final int FALLBACK_ADVANCE = 6;

    /**
     * One glyph: where it is on its sheet, how big its cell is, what it is scaled by,
     * and how far the cursor moves after it.
     */
    private record Glyph(BufferedImage sheet, int sourceX, int sourceY, int sourceWidth,
                         int sourceHeight, int drawWidth, int drawHeight, int top, int advance) {
    }

    private final Map<Integer, Glyph> glyphs = new HashMap<>();
    private final Map<Integer, Integer> spaces = new HashMap<>();

    private PixelFont() {
    }

    static PixelFont vanilla() {
        PixelFont font = new PixelFont();
        font.read("assets/minecraft/font/default.json");
        return font;
    }

    /**
     * Reads a font definition, following its references.
     *
     * <p>The first provider to carry a codepoint keeps it, which is the game's own
     * order of precedence — it is why a space is four pixels wide and not the one pixel
     * the empty cell in {@code ascii.png} would give it.
     */
    private void read(String path) {
        JsonObject definition = GameAssets.json(path);
        if (definition == null || !definition.has("providers")) {
            return;
        }
        for (JsonElement element : definition.getAsJsonArray("providers")) {
            JsonObject provider = element.getAsJsonObject();
            switch (provider.get("type").getAsString()) {
                case "reference" -> read(fontPath(provider.get("id").getAsString()));
                case "space" -> readSpaces(provider);
                case "bitmap" -> readBitmap(provider);
                default -> {
                    // unihex and the rest are the unicode fallback, which the Latin
                    // alphabets this interface is written in never reach.
                }
            }
        }
    }

    private static String fontPath(String reference) {
        int colon = reference.indexOf(':');
        String namespace = colon < 0 ? "minecraft" : reference.substring(0, colon);
        String name = colon < 0 ? reference : reference.substring(colon + 1);
        return "assets/" + namespace + "/font/" + name + ".json";
    }

    private void readSpaces(JsonObject provider) {
        JsonObject advances = provider.getAsJsonObject("advances");
        for (String key : advances.keySet()) {
            this.spaces.putIfAbsent(key.codePointAt(0), advances.get(key).getAsInt());
        }
    }

    private void readBitmap(JsonObject provider) {
        BufferedImage sheet = GameAssets.image(GameAssets.texturePath(provider.get("file").getAsString()));
        if (sheet == null) {
            return;
        }
        JsonArray rows = provider.getAsJsonArray("chars");
        int ascent = provider.get("ascent").getAsInt();
        int height = provider.has("height") ? provider.get("height").getAsInt() : 8;

        int columns = rows.get(0).getAsString().codePointCount(0, rows.get(0).getAsString().length());
        int cellWidth = sheet.getWidth() / columns;
        int cellHeight = sheet.getHeight() / rows.size();
        float scale = (float) height / cellHeight;

        for (int row = 0; row < rows.size(); row++) {
            String line = rows.get(row).getAsString();
            int column = 0;
            for (int offset = 0; offset < line.length(); ) {
                int codepoint = line.codePointAt(offset);
                offset += Character.charCount(codepoint);
                int cellX = column * cellWidth;
                int cellY = row * cellHeight;
                column++;
                if (codepoint == 0) {
                    continue;
                }
                int pixels = glyphWidth(sheet, cellX, cellY, cellWidth, cellHeight);
                int advance = (int) (0.5 + pixels * scale) + 1;
                this.glyphs.putIfAbsent(codepoint, new Glyph(sheet, cellX, cellY,
                        cellWidth, cellHeight,
                        Math.round(cellWidth * scale), Math.round(cellHeight * scale),
                        BASELINE - ascent, advance));
            }
        }
    }

    /** How many columns of the cell the glyph actually reaches, right to left. */
    private static int glyphWidth(BufferedImage sheet, int cellX, int cellY, int width, int height) {
        for (int column = width - 1; column >= 0; column--) {
            for (int row = 0; row < height; row++) {
                if ((sheet.getRGB(cellX + column, cellY + row) >>> 24) != 0) {
                    return column + 1;
                }
            }
        }
        return 0;
    }

    int advance(int codepoint) {
        Integer space = this.spaces.get(codepoint);
        if (space != null) {
            return space;
        }
        Glyph glyph = this.glyphs.get(codepoint);
        return glyph == null ? FALLBACK_ADVANCE : glyph.advance();
    }

    int width(String text) {
        int total = 0;
        for (int offset = 0; offset < text.length(); ) {
            int codepoint = text.codePointAt(offset);
            offset += Character.charCount(codepoint);
            total += advance(codepoint);
        }
        return total;
    }

    /** As much of the string as fits, cut on a whole character. */
    String trim(String text, int room) {
        int total = 0;
        for (int offset = 0; offset < text.length(); ) {
            int codepoint = text.codePointAt(offset);
            int next = offset + Character.charCount(codepoint);
            total += advance(codepoint);
            if (total > room) {
                return text.substring(0, offset);
            }
            offset = next;
        }
        return text;
    }

    /**
     * Draws a string, and its shadow when it has one.
     *
     * <p>The shadow is the game's: the same glyphs one pixel down and right, in a
     * quarter of the colour. Drawn first, whole, so a letter's shadow never lands on
     * top of the letter before it.
     */
    void draw(Raster raster, String text, int x, int y, int argb, boolean shadow) {
        if (shadow) {
            drawGlyphs(raster, text, x + 1, y + 1, shadowOf(argb));
        }
        drawGlyphs(raster, text, x, y, argb);
    }

    private static int shadowOf(int argb) {
        return (argb & 0xFF000000)
                | (((argb >> 16) & 0xFF) / 4) << 16
                | (((argb >> 8) & 0xFF) / 4) << 8
                | ((argb & 0xFF) / 4);
    }

    private void drawGlyphs(Raster raster, String text, int x, int y, int argb) {
        int cursor = x;
        for (int offset = 0; offset < text.length(); ) {
            int codepoint = text.codePointAt(offset);
            offset += Character.charCount(codepoint);
            Glyph glyph = this.glyphs.get(codepoint);
            if (glyph != null) {
                drawGlyph(raster, glyph, cursor, y + glyph.top(), argb);
            }
            cursor += advance(codepoint);
        }
    }

    /**
     * One glyph, tinted.
     *
     * <p>The sheets are white silhouettes on transparency, so the colour is the tint
     * and the shape is the alpha — which is how the game gets every colour of text out
     * of one texture.
     */
    private static void drawGlyph(Raster raster, Glyph glyph, int x, int y, int argb) {
        for (int row = 0; row < glyph.drawHeight(); row++) {
            int sampleY = glyph.sourceY() + row * glyph.sourceHeight() / glyph.drawHeight();
            for (int column = 0; column < glyph.drawWidth(); column++) {
                int sampleX = glyph.sourceX() + column * glyph.sourceWidth() / glyph.drawWidth();
                int sample = glyph.sheet().getRGB(sampleX, sampleY);
                int alpha = (sample >>> 24) * ((argb >>> 24) & 0xFF) / 0xFF;
                if (alpha != 0) {
                    raster.blend(x + column, y + row, (alpha << 24) | (argb & 0x00FFFFFF));
                }
            }
        }
    }
}
