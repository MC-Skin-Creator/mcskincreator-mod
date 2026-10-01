/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
//~ figure
package fr.clixmods.mcsc.mod.preview;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;

import fr.clixmods.mcsc.mod.ui.Canvas;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The {@link Canvas} that paints into an image instead of onto the game.
 *
 * <p>Every call a panel makes is the call the game would have received, answered with
 * the same pixels: the game's font, the game's sprites, the game's nine-slicing. What
 * comes out is not an impression of the screen, it is the screen — minus the two
 * things that need a running client, the player figure and any texture the mod uploads
 * at runtime.
 */
public final class ImageCanvas implements Canvas {
    private static final int TILE_LIMIT = 512;

    private final Raster raster;
    private final PixelFont font = PixelFont.vanilla();
    /** Textures the preview made up, so a sheet that only exists on the GPU can stand in. */
    private final Map<Identifier, BufferedImage> supplied = new HashMap<>();
    private final Map<String, JsonObject> scalings = new HashMap<>();

    /**
     * @param scale screen pixels to one interface pixel — the GUI scale that window
     *              takes. The picture comes out at screen size, because the half-size
     *              text only exists there.
     */
    public ImageCanvas(int width, int height, int scale) {
        this.raster = new Raster(width, height, scale);
    }

    public BufferedImage image() {
        return this.raster.toImage();
    }

    /** Hands the preview a picture to answer a texture name with. */
    public void supply(Identifier texture, BufferedImage image) {
        this.supplied.put(texture, image);
    }

    @Override
    public int lineHeight() {
        return PixelFont.LINE_HEIGHT;
    }

    @Override
    public int textWidth(Component text) {
        return this.font.width(text.getString());
    }

    @Override
    public int textWidth(String text) {
        return this.font.width(text);
    }

    @Override
    public String trimToWidth(String text, int room) {
        return this.font.trim(text, room);
    }

    @Override
    public void fill(int x, int y, int width, int height, int argb) {
        this.raster.fill(x, y, width, height, argb);
    }

    @Override
    public void text(Component text, int x, int y, int argb) {
        this.font.draw(this.raster, text.getString(), x, y, argb, true);
    }

    @Override
    public void textFlat(Component text, int x, int y, int argb) {
        this.font.draw(this.raster, text.getString(), x, y, argb, false);
    }

    @Override
    public int smallLineHeight() {
        return (lineHeight() + 1) / 2;
    }

    @Override
    public int smallTextWidth(Component text) {
        return (textWidth(text) + 1) / 2;
    }

    @Override
    public int smallTextWidth(String text) {
        return (textWidth(text) + 1) / 2;
    }

    @Override
    public String trimToSmallWidth(String text, int room) {
        return this.font.trim(text, room * 2);
    }

    @Override
    public void textSmall(Component text, int x, int y, int argb) {
        this.font.drawSmall(this.raster, text.getString(), x, y, argb);
    }

    @Override
    public void textTracked(String text, int x, int y, int argb, int tracking) {
        int cursor = x;
        for (int index = 0; index < text.length(); index++) {
            String glyph = text.substring(index, index + 1);
            this.font.draw(this.raster, glyph, cursor, y, argb, false);
            cursor += this.font.width(glyph) + tracking;
        }
    }

    @Override
    public int trackedWidth(String text, int tracking) {
        int total = 0;
        for (int index = 0; index < text.length(); index++) {
            total += this.font.width(text.substring(index, index + 1)) + tracking;
        }
        return Math.max(0, total - tracking);
    }

    @Override
    public void textCentered(Component text, int centerX, int y, int argb) {
        String string = text.getString();
        this.font.draw(this.raster, string, centerX - this.font.width(string) / 2, y, argb, true);
    }

    @Override
    public void textRinged(Component text, int x, int y, int argb) {
        textFlat(text, x - 1, y, 0xFF000000);
        textFlat(text, x + 1, y, 0xFF000000);
        textFlat(text, x, y - 1, 0xFF000000);
        textFlat(text, x, y + 1, 0xFF000000);
        textFlat(text, x, y, argb);
    }

    @Override
    public void textWrapped(Component text, int x, int y, int lineWidth, int argb) {
        int cursorY = y;
        for (String line : wrap(text.getString(), lineWidth)) {
            this.font.draw(this.raster, line, x, cursorY, argb, true);
            cursorY += PixelFont.LINE_HEIGHT;
        }
    }

    @Override
    public int wrappedHeight(Component text, int lineWidth) {
        return wrap(text.getString(), lineWidth).size() * PixelFont.LINE_HEIGHT;
    }

    private List<String> wrap(String text, int lineWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (this.font.width(candidate) > Math.max(1, lineWidth) && !current.isEmpty()) {
                lines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(candidate);
            }
        }
        lines.add(current.toString());
        return lines;
    }

    @Override
    public void blit(Identifier texture, int x, int y, int width, int height,
                     float u, float v, int sourceWidth, int sourceHeight,
                     int textureWidth, int textureHeight) {
        blitTinted(texture, x, y, width, height, u, v, sourceWidth, sourceHeight,
                textureWidth, textureHeight, 0xFFFFFFFF);
    }

    @Override
    public void blitTinted(Identifier texture, int x, int y, int width, int height,
                           float u, float v, int sourceWidth, int sourceHeight,
                           int textureWidth, int textureHeight, int tint) {
        BufferedImage image = texture(texture);
        if (image == null) {
            return;
        }
        this.raster.draw(image, x, y, width, height,
                (int) u, (int) v, sourceWidth, sourceHeight, tint);
    }

    @Override
    public void sprite(Identifier sprite, int x, int y, int width, int height) {
        spriteTinted(sprite, x, y, width, height, 0xFFFFFFFF);
    }

    /**
     * A vanilla sprite at the size asked for, sliced the way its own metadata says.
     *
     * <p>The metadata is the point. A nine-sliced sprite keeps its corners whole and
     * tiles what is between them; stretching the whole picture instead would give a
     * preview of smeared borders that the game never draws, and would hide the one
     * thing worth checking — whether a control was given a size its sprite can carry.
     */
    @Override
    public void spriteTinted(Identifier sprite, int x, int y, int width, int height, int tint) {
        if (width <= 0 || height <= 0) {
            return;
        }
        BufferedImage image = texture(withPrefix(sprite, "gui/sprites/"));
        if (image == null) {
            return;
        }
        JsonObject scaling = scaling(sprite);
        if (scaling == null) {
            this.raster.draw(image, x, y, width, height, 0, 0,
                    image.getWidth(), image.getHeight(), tint);
            return;
        }
        switch (scaling.get("type").getAsString()) {
            case "nine_slice" -> nineSlice(image, scaling, x, y, width, height, tint);
            case "tile" -> tile(image, x, y, width, height,
                    scaling.get("width").getAsInt(), scaling.get("height").getAsInt(),
                    0, 0, image.getWidth(), image.getHeight(), tint);
            default -> this.raster.draw(image, x, y, width, height, 0, 0,
                    image.getWidth(), image.getHeight(), tint);
        }
    }

    private void nineSlice(BufferedImage image, JsonObject scaling,
                           int x, int y, int width, int height, int tint) {
        int sheetWidth = scaling.get("width").getAsInt();
        int sheetHeight = scaling.get("height").getAsInt();
        int[] border = border(scaling);
        int left = Math.min(border[0], width / 2);
        int top = Math.min(border[1], height / 2);
        int right = Math.min(border[2], width / 2);
        int bottom = Math.min(border[3], height / 2);

        int innerWidth = Math.max(0, width - left - right);
        int innerHeight = Math.max(0, height - top - bottom);
        int sourceInnerWidth = Math.max(1, sheetWidth - border[0] - border[2]);
        int sourceInnerHeight = Math.max(1, sheetHeight - border[1] - border[3]);

        // Corners whole.
        this.raster.draw(image, x, y, left, top, 0, 0, left, top, tint);
        this.raster.draw(image, x + width - right, y, right, top,
                sheetWidth - right, 0, right, top, tint);
        this.raster.draw(image, x, y + height - bottom, left, bottom,
                0, sheetHeight - bottom, left, bottom, tint);
        this.raster.draw(image, x + width - right, y + height - bottom, right, bottom,
                sheetWidth - right, sheetHeight - bottom, right, bottom, tint);

        // Edges and middle, tiled — which is what keeps a grain a grain.
        tile(image, x + left, y, innerWidth, top, sourceInnerWidth, top,
                border[0], 0, sourceInnerWidth, top, tint);
        tile(image, x + left, y + height - bottom, innerWidth, bottom,
                sourceInnerWidth, bottom, border[0], sheetHeight - bottom,
                sourceInnerWidth, bottom, tint);
        tile(image, x, y + top, left, innerHeight, left, sourceInnerHeight,
                0, border[1], left, sourceInnerHeight, tint);
        tile(image, x + width - right, y + top, right, innerHeight, right, sourceInnerHeight,
                sheetWidth - right, border[1], right, sourceInnerHeight, tint);
        tile(image, x + left, y + top, innerWidth, innerHeight,
                sourceInnerWidth, sourceInnerHeight,
                border[0], border[1], sourceInnerWidth, sourceInnerHeight, tint);
    }

    private static int[] border(JsonObject scaling) {
        var element = scaling.get("border");
        if (element.isJsonObject()) {
            JsonObject sides = element.getAsJsonObject();
            return new int[] {sides.get("left").getAsInt(), sides.get("top").getAsInt(),
                    sides.get("right").getAsInt(), sides.get("bottom").getAsInt()};
        }
        int all = element.getAsInt();
        return new int[] {all, all, all, all};
    }

    /** Repeats a piece of the sheet across a rectangle, clipping the last row and column. */
    private void tile(BufferedImage image, int x, int y, int width, int height,
                      int stepX, int stepY, int sourceX, int sourceY,
                      int sourceWidth, int sourceHeight, int tint) {
        if (width <= 0 || height <= 0 || stepX <= 0 || stepY <= 0) {
            return;
        }
        this.raster.pushClip(x, y, width, height);
        int rows = Math.min(TILE_LIMIT, (height + stepY - 1) / stepY);
        int columns = Math.min(TILE_LIMIT, (width + stepX - 1) / stepX);
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                this.raster.draw(image, x + column * stepX, y + row * stepY, stepX, stepY,
                        sourceX, sourceY, sourceWidth, sourceHeight, tint);
            }
        }
        this.raster.popClip();
    }

    private JsonObject scaling(Identifier sprite) {
        JsonObject metadata = this.scalings.computeIfAbsent(sprite.toString(), key -> {
            JsonObject read = GameAssets.json(
                    GameAssets.texturePath(withPrefix(sprite, "gui/sprites/").toString()) + ".mcmeta");
            return read == null ? new JsonObject() : read;
        });
        return metadata.has("gui") && metadata.getAsJsonObject("gui").has("scaling")
                ? metadata.getAsJsonObject("gui").getAsJsonObject("scaling")
                : null;
    }

    private static Identifier withPrefix(Identifier sprite, String prefix) {
        return sprite.withPath(prefix + sprite.getPath() + ".png");
    }

    private BufferedImage texture(Identifier texture) {
        BufferedImage given = this.supplied.get(texture);
        return given != null ? given : GameAssets.image(GameAssets.texturePath(texture.toString()));
    }

    /**
     * The game's own widgets are not drawn here: the only one this interface uses is
     * the player, and the preview stands something else in its place.
     */
    @Override
    public void widget(Renderable widget, int mouseX, int mouseY, float delta) {
    }

    /**
     * Nothing, like {@link #widget}: a render state is drawn by the game's entity
     * renderers, which want a GPU. The preview stands a flat shape in the figure's place
     * instead — see {@code PreviewFigure} — so the layout around it can still be looked
     * at.
     */
    @Override
    public void entity(AvatarRenderState state, float scale, Vector3f translation,
                       Quaternionf rotation, Quaternionf overrideCameraAngle,
                       int x, int y, int width, int height) {
    }

    @Override
    public void pushScissor(int x, int y, int width, int height) {
        this.raster.pushClip(x, y, width, height);
    }

    @Override
    public void popScissor() {
        this.raster.popClip();
    }

    /** A tooltip is a frame the game draws over everything; a still picture has none. */
    @Override
    public void tooltip(List<Component> lines, int mouseX, int mouseY) {
    }
}
