/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.skin;

import java.util.List;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.resources.Identifier;

/**
 * The thumbnails of one category, as a single sheet of front views.
 *
 * <p>One texture per category rather than one per element: a category runs to
 * hundreds of elements, and a texture each would be hundreds of uploads to make and
 * hundreds of names to release. The sheet is laid out in rows of
 * {@value #COLUMNS}, so an element's thumbnail is a rectangle of it.
 *
 * <p>The raw buffers are kept alongside: they are 64x64 skins in their own right,
 * which is what the preview needs to show an element on the model without asking the
 * server to compose a single-element project.
 */
public final class CategorySprites implements AutoCloseable {
    public static final int COLUMNS = 16;

    private final ManagedTexture texture;
    private final List<byte[]> buffers;
    private final int rows;

    private CategorySprites(ManagedTexture texture, List<byte[]> buffers, int rows) {
        this.texture = texture;
        this.buffers = buffers;
        this.rows = rows;
    }

    /**
     * Projects every buffer of an atlas and uploads the sheet. Must run on the client
     * thread, since it ends in a texture upload.
     *
     * <p>Thumbnails are drawn from the classic model whatever the player previews
     * with: the two models differ by one pixel down each arm, which no 16-pixel-wide
     * thumbnail shows, and building the sheet again on every model toggle would cost a
     * full re-upload for nothing.
     */
    public static CategorySprites of(String categoryId, List<byte[]> buffers) {
        int rows = Math.max(1, (buffers.size() + COLUMNS - 1) / COLUMNS);
        int width = COLUMNS * FrontSprite.WIDTH;
        int height = rows * FrontSprite.HEIGHT;

        int[] pixels = new int[width * height];
        for (int index = 0; index < buffers.size(); index++) {
            FrontSprite.draw(
                    buffers.get(index),
                    false,
                    pixels,
                    width,
                    index % COLUMNS * FrontSprite.WIDTH,
                    index / COLUMNS * FrontSprite.HEIGHT);
        }

        NativeImage image = new NativeImage(width, height, false);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setPixel(x, y, pixels[y * width + x]);
            }
        }

        ManagedTexture texture = new ManagedTexture(categoryId);
        texture.upload(image);
        return new CategorySprites(texture, List.copyOf(buffers), rows);
    }

    public Identifier texture() {
        return this.texture.id();
    }

    public int count() {
        return this.buffers.size();
    }

    public int sheetWidth() {
        return COLUMNS * FrontSprite.WIDTH;
    }

    public int sheetHeight() {
        return this.rows * FrontSprite.HEIGHT;
    }

    /** The left edge of an element's thumbnail inside the sheet. */
    public int spriteU(int index) {
        return index % COLUMNS * FrontSprite.WIDTH;
    }

    /** The top edge of an element's thumbnail inside the sheet. */
    public int spriteV(int index) {
        return index / COLUMNS * FrontSprite.HEIGHT;
    }

    /** An element's own 64x64 skin, as RGBA, or {@code null} past the end. */
    public byte[] buffer(int index) {
        return index >= 0 && index < this.buffers.size() ? this.buffers.get(index) : null;
    }

    @Override
    public void close() {
        this.texture.close();
    }
}
