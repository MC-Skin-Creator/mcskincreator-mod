/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.project;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import fr.clixmods.mcsc.mod.catalog.CatalogCategory;
import fr.clixmods.mcsc.mod.catalog.ThumbCrop;
import fr.clixmods.mcsc.mod.catalog.CatalogItem;
import net.minecraft.network.chat.Component;

/**
 * One entry of the layer stack: an element of the catalogue, and what has been done
 * to it since it was stacked.
 *
 * <p>The bounds below are the server's, not the mod's: its project validator refuses
 * an opacity outside 0 to 100 and an adjustment outside its own range, naming the
 * path it refused. They are clamped on the way in rather than checked on the way out,
 * so a slider dragged past its end cannot build a project the server would send back.
 *
 * <p>The element's own labels stay where they came from. {@code name} is resolved
 * from the catalogue's {@link fr.clixmods.mcsc.mod.catalog.CatalogText} once, when
 * the layer is made, because that is the only moment the player's language is
 * relevant and the stack has no business re-deciding it every frame.
 *
 * <p>The colours are per key, and the keys are the element's: a layer can recolour
 * what the catalogue says the element is made of and nothing else, because the server
 * recolours by a map of those same keys and would have nothing to apply another to.
 * The catalogue's own colour of each key is kept beside the chosen one, so a key put
 * back to where it started is left out of the project again.
 */
public final class Layer {
    private final String categoryId;
    private final String region;
    private final String itemId;
    private final Component name;
    private final Component categoryName;
    private final int atlasIndex;
    private final int slimAtlasIndex;
    /** The element's colour of each key, in the catalogue's order. Never changes. */
    private final Map<String, Integer> defaultColors;
    /** The element itself, which is what recolouring reads its zone map from. */
    private final CatalogItem item;
    /** The colour each key is shown in now, same keys, same order. */
    private final Map<String, Integer> colors;
    /**
     * The part of the body this layer's picture shows.
     *
     * <p>Kept on the layer because the row that draws it has the layer and not the
     * category it came from. Without it the row drew the whole 16 by 32 front view into
     * a box fifteen pixels square, at the only whole scale that fits — which is one — so
     * what you saw was the middle fifteen rows of the body. For anything worn on the
     * head, that is fifteen rows of nothing, and the picture looked broken.
     */
    private final ThumbCrop thumbCrop;

    private boolean visible = true;
    private int opacity = 100;
    private int hue;
    private int saturation = 100;
    private int brightness;

    public Layer(CatalogCategory category, CatalogItem item, String languageCode) {
        this.categoryId = category.id();
        this.region = category.region();
        this.itemId = item.id();
        this.name = Component.literal(item.name().forLanguage(languageCode));
        this.categoryName = Component.literal(category.name().forLanguage(languageCode));
        this.atlasIndex = item.atlasIndex();
        this.slimAtlasIndex = item.slimAtlasIndex();
        this.defaultColors = item.colors();
        this.item = item;
        this.colors = new LinkedHashMap<>(item.colors());
        this.thumbCrop = category.thumbCrop(item);
    }

    private Layer(Layer source) {
        this.categoryId = source.categoryId;
        this.region = source.region;
        this.itemId = source.itemId;
        this.name = source.name;
        this.categoryName = source.categoryName;
        this.atlasIndex = source.atlasIndex;
        this.slimAtlasIndex = source.slimAtlasIndex;
        this.defaultColors = source.defaultColors;
        this.item = source.item;
        this.colors = new LinkedHashMap<>(source.colors);
        this.thumbCrop = source.thumbCrop;
        this.visible = source.visible;
        this.opacity = source.opacity;
        this.hue = source.hue;
        this.saturation = source.saturation;
        this.brightness = source.brightness;
    }

    /** A detached copy, which is what a history snapshot stores. */
    public Layer copy() {
        return new Layer(this);
    }

    public String categoryId() {
        return this.categoryId;
    }

    public String region() {
        return this.region;
    }

    public String itemId() {
        return this.itemId;
    }

    public Component name() {
        return this.name;
    }

    public Component categoryName() {
        return this.categoryName;
    }

    /** The part of the body this layer's picture shows. */
    public ThumbCrop thumbCrop() {
        return this.thumbCrop;
    }

    /** The element's buffer in its category's atlas, for the model on show. */
    public int atlasIndex(boolean slim) {
        return slim && this.slimAtlasIndex != CatalogItem.NONE ? this.slimAtlasIndex : this.atlasIndex;
    }

    public boolean visible() {
        return this.visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public int opacity() {
        return this.opacity;
    }

    public void setOpacity(int opacity) {
        this.opacity = Math.max(0, Math.min(100, opacity));
    }

    public int hue() {
        return this.hue;
    }

    public void setHue(int hue) {
        this.hue = Math.max(-180, Math.min(180, hue));
    }

    public int saturation() {
        return this.saturation;
    }

    public void setSaturation(int saturation) {
        this.saturation = Math.max(0, Math.min(200, saturation));
    }

    public int brightness() {
        return this.brightness;
    }

    public void setBrightness(int brightness) {
        this.brightness = Math.max(-50, Math.min(50, brightness));
    }

    /** The catalogue's element this layer stacks. */
    public CatalogItem item() {
        return this.item;
    }

    /** The keys the element can be recoloured by, in the catalogue's order. */
    public List<String> colorKeys() {
        return List.copyOf(this.colors.keySet());
    }

    /** The colour {@code key} is shown in now, as {@code 0xRRGGBB}. */
    public int color(String key) {
        Integer rgb = this.colors.get(key);
        if (rgb == null) {
            throw new IllegalArgumentException("The element has no colour key " + key);
        }
        return rgb;
    }

    /** The colour the element was drawn in for {@code key}. */
    public int defaultColor(String key) {
        Integer rgb = this.defaultColors.get(key);
        if (rgb == null) {
            throw new IllegalArgumentException("The element has no colour key " + key);
        }
        return rgb;
    }

    /**
     * Recolours one key.
     *
     * @return false, changing nothing, when the element has no such key — a stored
     *     project can name one the catalogue has since dropped
     */
    public boolean setColor(String key, int rgb) {
        if (!this.colors.containsKey(key)) {
            return false;
        }
        this.colors.put(key, rgb & 0xFFFFFF);
        return true;
    }

    /** Only the keys moved off the element's own colour: what the project carries. */
    public Map<String, Integer> changedColors() {
        Map<String, Integer> changed = new LinkedHashMap<>();
        this.colors.forEach((key, rgb) -> {
            if (!rgb.equals(this.defaultColors.get(key))) {
                changed.put(key, rgb);
            }
        });
        return Collections.unmodifiableMap(changed);
    }

    /** True while nothing has been adjusted, which is what lets the JSON stay short. */
    public boolean isUnadjusted() {
        return this.hue == 0 && this.saturation == 100 && this.brightness == 0;
    }

    /** Puts back everything the element came with: its colours, and no shift over them. */
    public void resetAdjustments() {
        this.hue = 0;
        this.saturation = 100;
        this.brightness = 0;
        this.colors.putAll(this.defaultColors);
    }
}
