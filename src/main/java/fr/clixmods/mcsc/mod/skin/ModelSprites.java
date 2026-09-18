/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.skin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.mojang.blaze3d.platform.NativeImage;
import fr.clixmods.mcsc.mod.catalog.Catalog;
import fr.clixmods.mcsc.mod.catalog.CatalogCategory;
import fr.clixmods.mcsc.mod.catalog.CatalogItem;
import fr.clixmods.mcsc.mod.catalog.CatalogModel;
import net.minecraft.resources.Identifier;

/**
 * The pictures of the catalogue's ready-made stacks, on one sheet.
 *
 * <p>Each one is its pieces stacked and then seen from the front — the same two steps
 * the editor's preview goes through, so the picture is what choosing it will actually
 * put on the model. It is <em>not</em> always what the site shows for the same entry:
 * the catalogue gives some pieces a colour override, which a layer here cannot hold
 * yet, so those are drawn in the colours the element was drawn in. Showing what the
 * mod will do is the more useful of the two.
 *
 * <p>One sheet for all of them, as {@link CategorySprites} does for a category: two
 * hundred entries would otherwise be two hundred textures.
 *
 * <p>A stack whose categories have not all arrived is left blank rather than drawn
 * half-dressed, and {@link #has} says which — the window shows those as still coming.
 */
public final class ModelSprites implements AutoCloseable {
    public static final int COLUMNS = 16;

    private final ManagedTexture texture;
    private final Map<String, Integer> index;
    private final int rows;

    private ModelSprites(ManagedTexture texture, Map<String, Integer> index, int rows) {
        this.texture = texture;
        this.index = index;
        this.rows = rows;
    }

    /**
     * Draws every ready-made stack it has the pixels for. Must run on the client
     * thread: it ends in a texture upload.
     *
     * @param sprites the sheet of a category, or null while its atlas is on its way
     * @param slim    the model the pictures are posed on, so an element with a slim
     *                variant is shown the way it will be worn
     */
    public static ModelSprites of(List<CatalogModel> models, Catalog catalog,
                                  Function<String, CategorySprites> sprites, boolean slim) {
        int rows = Math.max(1, (models.size() + COLUMNS - 1) / COLUMNS);
        int width = COLUMNS * FrontSprite.WIDTH;
        int height = rows * FrontSprite.HEIGHT;

        int[] pixels = new int[width * height];
        Map<String, Integer> index = new HashMap<>();
        for (int slot = 0; slot < models.size(); slot++) {
            CatalogModel model = models.get(slot);
            List<byte[]> buffers = buffers(model, catalog, sprites, slim);
            if (buffers == null) {
                continue;
            }
            // A model brings its own player model, so it is posed on that one rather
            // than on whatever the editor happens to be showing.
            boolean posed = model.kind() == CatalogModel.Kind.MODEL ? model.slim() : slim;
            FrontSprite.draw(Composite.of(buffers), posed, pixels, width,
                    slot % COLUMNS * FrontSprite.WIDTH, slot / COLUMNS * FrontSprite.HEIGHT);
            index.put(key(model), slot);
        }

        NativeImage image = new NativeImage(width, height, false);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setPixel(x, y, pixels[y * width + x]);
            }
        }

        ManagedTexture texture = new ManagedTexture("models");
        texture.upload(image);
        return new ModelSprites(texture, Map.copyOf(index), rows);
    }

    /**
     * The pieces' own buffers, bottom first, or null when any of them is missing.
     *
     * <p>All or nothing on purpose: a stack drawn without its trousers looks like a
     * stack that comes without trousers.
     */
    private static List<byte[]> buffers(CatalogModel model, Catalog catalog,
                                        Function<String, CategorySprites> sprites, boolean slim) {
        boolean posed = model.kind() == CatalogModel.Kind.MODEL ? model.slim() : slim;
        List<byte[]> buffers = new ArrayList<>(model.pieces().size());
        for (CatalogModel.Piece piece : model.pieces()) {
            CatalogCategory category = catalog.category(piece.categoryId()).orElse(null);
            if (category == null) {
                // A piece the catalogue has dropped is skipped, exactly as stacking it
                // would skip it.
                continue;
            }
            CategorySprites sheet = sprites.apply(piece.categoryId());
            if (sheet == null) {
                // Its category is still downloading: try again when it lands.
                return null;
            }
            CatalogItem item = category.items().stream()
                    .filter(candidate -> candidate.id().equals(piece.itemId()))
                    .findFirst()
                    .orElse(null);
            if (item == null) {
                continue;
            }
            byte[] buffer = sheet.buffer(item.atlasIndex(posed));
            if (buffer == null) {
                return null;
            }
            buffers.add(buffer);
        }
        return buffers.isEmpty() ? null : buffers;
    }

    /** Whether this sheet holds a picture for a stack, or it is still waiting on pixels. */
    public boolean has(CatalogModel model) {
        return this.index.containsKey(key(model));
    }

    /**
     * Models and outfits are two lists of the site's, so nothing stops one of each
     * carrying the same id; the kind goes in the key rather than trusting that.
     */
    private static String key(CatalogModel model) {
        return model.kind() + "/" + model.id();
    }

    public Identifier texture() {
        return this.texture.id();
    }

    public int sheetWidth() {
        return COLUMNS * FrontSprite.WIDTH;
    }

    public int sheetHeight() {
        return this.rows * FrontSprite.HEIGHT;
    }

    /** The left edge of a stack's picture inside the sheet, or -1 when it has none. */
    public int spriteU(CatalogModel model) {
        Integer slot = this.index.get(key(model));
        return slot == null ? -1 : slot % COLUMNS * FrontSprite.WIDTH;
    }

    /** The top edge of a stack's picture inside the sheet, or -1 when it has none. */
    public int spriteV(CatalogModel model) {
        Integer slot = this.index.get(key(model));
        return slot == null ? -1 : slot / COLUMNS * FrontSprite.HEIGHT;
    }

    @Override
    public void close() {
        this.texture.close();
    }
}
