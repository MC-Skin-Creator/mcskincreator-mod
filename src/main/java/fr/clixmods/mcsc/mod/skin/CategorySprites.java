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
import fr.clixmods.mcsc.mod.catalog.ThumbCrop;
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
    /**
     * The box of drawn pixels in each sprite, as {@code minX, minY, maxX, maxY}, or
     * an empty box when the sprite has none. Measured while the sheet is built, which
     * already walks every pixel, and used to tell a crop that would come out blank
     * from one that will not - the pixels are on the GPU by the time anything draws.
     */
    private final int[] bounds;

    private CategorySprites(ManagedTexture texture, List<byte[]> buffers, int rows, int[] bounds) {
        this.texture = texture;
        this.buffers = buffers;
        this.rows = rows;
        this.bounds = bounds;
    }

    /**
     * Projects every buffer of an atlas and uploads the sheet. Must run on the client
     * thread, since it ends in a texture upload.
     *
     * <p>One sprite per atlas buffer, which means both model variants of an element
     * get one. Indexing the sheet by the buffer's own rank is then the whole of the
     * bookkeeping, and the preview can reach either variant without the sheet being
     * built again.
     */
    public static CategorySprites of(String categoryId, List<byte[]> buffers) {
        int rows = Math.max(1, (buffers.size() + COLUMNS - 1) / COLUMNS);
        int width = COLUMNS * FrontSprite.WIDTH;
        int height = rows * FrontSprite.HEIGHT;

        int[] pixels = new int[width * height];
        int[] bounds = new int[buffers.size() * 4];
        for (int index = 0; index < buffers.size(); index++) {
            int left = index % COLUMNS * FrontSprite.WIDTH;
            int top = index / COLUMNS * FrontSprite.HEIGHT;
            FrontSprite.draw(buffers.get(index), false, pixels, width, left, top);
            measure(pixels, width, left, top, bounds, index * 4);
        }

        NativeImage image = new NativeImage(width, height, false);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setPixel(x, y, pixels[y * width + x]);
            }
        }

        ManagedTexture texture = new ManagedTexture(categoryId);
        texture.upload(image);
        return new CategorySprites(texture, List.copyOf(buffers), rows, bounds);
    }

    /** Records the box of drawn pixels of the sprite at {@code left, top}. */
    private static void measure(int[] pixels, int stride, int left, int top, int[] into, int at) {
        int minX = FrontSprite.WIDTH;
        int minY = FrontSprite.HEIGHT;
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < FrontSprite.HEIGHT; y++) {
            for (int x = 0; x < FrontSprite.WIDTH; x++) {
                if ((pixels[(top + y) * stride + left + x] >>> 24) != 0) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        into[at] = minX;
        into[at + 1] = minY;
        into[at + 2] = maxX;
        into[at + 3] = maxY;
    }

    /**
     * Whether {@code crop} would show anything of the sprite at {@code index}.
     *
     * <p>A handful of elements in the library are drawn outside the part of the body
     * their category claims - and a few are drawn only on faces a front view cannot
     * show at all, for which no crop helps. This tells the first case from the second,
     * so the panel can widen the crop for the one it can rescue.
     */
    public boolean covers(int index, ThumbCrop crop) {
        if (index < 0 || index >= this.buffers.size()) {
            return false;
        }
        int at = index * 4;
        int maxX = this.bounds[at + 2];
        int maxY = this.bounds[at + 3];
        if (maxX < 0 || maxY < 0) {
            return false;
        }
        return this.bounds[at] <= crop.x() + crop.width() - 1
                && crop.x() <= maxX
                && this.bounds[at + 1] <= crop.y() + crop.height() - 1
                && crop.y() <= maxY;
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
