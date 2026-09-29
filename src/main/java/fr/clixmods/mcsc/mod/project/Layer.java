/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.project;

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
 */
public final class Layer {
    private final String categoryId;
    private final String region;
    private final String itemId;
    private final Component name;
    private final Component categoryName;
    private final int atlasIndex;
    private final int slimAtlasIndex;
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

    /** True while nothing has been adjusted, which is what lets the JSON stay short. */
    public boolean isUnadjusted() {
        return this.hue == 0 && this.saturation == 100 && this.brightness == 0;
    }

    public void resetAdjustments() {
        this.hue = 0;
        this.saturation = 100;
        this.brightness = 0;
    }
}
