/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import fr.clixmods.mcsc.mod.catalog.Catalog;
import fr.clixmods.mcsc.mod.catalog.CatalogCategory;
import fr.clixmods.mcsc.mod.catalog.CatalogItem;
import fr.clixmods.mcsc.mod.catalog.CatalogText;
import fr.clixmods.mcsc.mod.skin.CategorySprites;
import fr.clixmods.mcsc.mod.skin.FrontSprite;
import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

/**
 * The library panel: regions, then the categories of the chosen region as a row of
 * icons, then that category's elements as scrolling inventory slots.
 *
 * <p>Shaped after the site's left-hand panel, because that arrangement has already
 * been through players' hands: the two choosers stay put and only the strip of
 * thumbnails moves, so picking an element never shifts what you were aiming at.
 *
 * <p><strong>A Minecraft screen has no layout.</strong> There is no flexbox and no
 * grid: {@link #layout} positions everything in pixels and {@link #render} draws it,
 * and the scrolling is counted out by hand. Which is why the arithmetic is gathered
 * in one method instead of being spread through the drawing - the panel has to hold
 * together at any window size and any of the four GUI scales, and that is easier to
 * see when the sizes are all in one place.
 */
final class ElementLibrary {
    /** What the panel tells the screen about. */
    interface Listener {
        void onElementPicked(CatalogCategory category, CatalogItem item);

        /**
         * The categories on screen have changed, so their thumbnails are what is now
         * worth fetching. Fired on the first catalogue and on every region change.
         */
        void onCategoriesChanged();
    }

    private static final int TAB_HEIGHT = 14;
    private static final int TAB_INNER_PADDING = 4;
    private static final int GAP = 2;
    private static final int ICON_SLOT = 20;
    private static final int THUMBNAIL_SLOT_WIDTH = FrontSprite.WIDTH + 4;
    private static final int THUMBNAIL_SLOT_HEIGHT = FrontSprite.HEIGHT + 4;
    private static final int SCROLLBAR_WIDTH = 3;
    private static final int MIN_COLUMNS = 2;
    private static final int MAX_COLUMNS = 6;
    private static final int SCROLL_STEP = THUMBNAIL_SLOT_HEIGHT / 2;
    /** Enough for the head and shoulders of a thumbnail, which is what an icon shows. */
    private static final int ICON_SOURCE_HEIGHT = 16;

    private static final int PANEL_BACKGROUND = 0xC0101010;
    private static final int PANEL_BORDER = 0xFF5A5A5A;
    private static final int SLOT_BACKGROUND = 0xFF2B2B2B;
    private static final int SLOT_SHADOW = 0xFF161616;
    private static final int SLOT_HIGHLIGHT = 0xFF6E6E6E;
    private static final int SELECTED = 0xFF55FF55;
    private static final int HOVERED = 0xFFFFFFFF;
    private static final int LABEL = 0xFFE0E0E0;
    private static final int LABEL_DIM = 0xFF9A9A9A;
    private static final int SCROLLBAR = 0xFF8B8B8B;

    private final Listener listener;
    /** How the panel gets at a category's thumbnails; {@code null} until they arrive. */
    private final Function<CatalogCategory, CategorySprites> sprites;

    private Catalog catalog = Catalog.EMPTY;
    private List<String> regions = List.of();
    private List<CatalogCategory> categories = List.of();
    private int region;
    private int category;
    private String selectedCategoryId = "";
    private String selectedItemId = "";

    private Rect panel = Rect.EMPTY;
    private Rect viewport = Rect.EMPTY;
    private Rect labelLine = Rect.EMPTY;
    private List<Rect> regionTabs = List.of();
    private List<Rect> categoryIcons = List.of();
    private int columns = 1;
    private int scroll;

    ElementLibrary(Listener listener, Function<CatalogCategory, CategorySprites> sprites) {
        this.listener = listener;
        this.sprites = sprites;
    }

    void catalog(Catalog catalog) {
        this.catalog = catalog;
        this.regions = catalog.regions();
        this.region = 0;
        this.category = 0;
        this.categories = this.regions.isEmpty() ? List.of() : catalog.categoriesIn(this.regions.get(0));
        this.scroll = 0;
        this.layout(this.panel);
        this.listener.onCategoriesChanged();
    }

    /** Whether a catalogue has been handed over, and so whether browsing can start. */
    boolean hasCatalog() {
        return !this.catalog.isEmpty();
    }

    /** The category whose elements are on screen, or {@code null} when there is none. */
    CatalogCategory openCategory() {
        return this.category >= 0 && this.category < this.categories.size()
                ? this.categories.get(this.category)
                : null;
    }

    /** Every category the panel may need thumbnails for right now: the open region's. */
    List<CatalogCategory> visibleCategories() {
        return this.categories;
    }

    void layout(int x, int y, int width, int height) {
        this.layout(new Rect(x, y, width, height));
    }

    /**
     * Places the two choosers and whatever height is left over becomes the scrolling
     * strip. Both choosers wrap onto as many rows as they need, so a narrow panel
     * loses height rather than pushing entries off the side.
     */
    private void layout(Rect panel) {
        this.panel = panel;
        int contentX = panel.x() + GAP;
        int contentWidth = Math.max(1, panel.width() - GAP * 2);
        int cursorY = panel.y() + GAP;

        List<Rect> tabs = new ArrayList<>(this.regions.size());
        int rowX = contentX;
        for (String region : this.regions) {
            int tabWidth = Math.min(contentWidth,
                    textWidth(regionLabel(region)) + TAB_INNER_PADDING * 2);
            if (rowX > contentX && rowX + tabWidth > contentX + contentWidth) {
                rowX = contentX;
                cursorY += TAB_HEIGHT + GAP;
            }
            tabs.add(new Rect(rowX, cursorY, tabWidth, TAB_HEIGHT));
            rowX += tabWidth + GAP;
        }
        this.regionTabs = List.copyOf(tabs);
        if (!tabs.isEmpty()) {
            cursorY += TAB_HEIGHT + GAP * 2;
        }

        List<Rect> icons = new ArrayList<>(this.categories.size());
        rowX = contentX;
        for (int index = 0; index < this.categories.size(); index++) {
            if (rowX > contentX && rowX + ICON_SLOT > contentX + contentWidth) {
                rowX = contentX;
                cursorY += ICON_SLOT + GAP;
            }
            icons.add(new Rect(rowX, cursorY, ICON_SLOT, ICON_SLOT));
            rowX += ICON_SLOT + GAP;
        }
        this.categoryIcons = List.copyOf(icons);
        if (!icons.isEmpty()) {
            cursorY += ICON_SLOT + GAP * 2;
        }

        int labelHeight = lineHeight() + GAP;
        int bottom = panel.y() + panel.height() - GAP;
        this.labelLine = new Rect(contentX, bottom - labelHeight + GAP, contentWidth, labelHeight);
        this.viewport = new Rect(contentX, cursorY, contentWidth,
                Math.max(0, bottom - labelHeight - cursorY));
        // The scrollbar gets its own strip on the right: counting columns across the
        // whole width would put the last one underneath it.
        int slotSpace = this.viewport.width() - SCROLLBAR_WIDTH - GAP;
        this.columns = Math.max(1, (slotSpace + GAP) / (THUMBNAIL_SLOT_WIDTH + GAP));
        this.clampScroll();
    }

    /**
     * The widest panel that fits in {@code available} while holding whole slot
     * columns, between {@value #MIN_COLUMNS} and {@value #MAX_COLUMNS} of them.
     *
     * <p>Rounded to whole columns because a panel wider than its columns is a strip of
     * unused background down the side, and bounded because a fixed width is a narrow
     * column on a wide window while more columns than this is a wall of thumbnails.
     */
    static int preferredWidth(int available) {
        int forSlots = available - GAP * 2 - SCROLLBAR_WIDTH - GAP;
        int columns = Math.clamp((forSlots + GAP) / (THUMBNAIL_SLOT_WIDTH + GAP),
                MIN_COLUMNS, MAX_COLUMNS);
        return (THUMBNAIL_SLOT_WIDTH + GAP) * columns - GAP + SCROLLBAR_WIDTH + GAP + GAP * 2;
    }

    /**
     * @param noCatalog what to say when there is no library to browse at all
     * @param noThumbnails what to say while a category's atlas has yet to arrive
     */
    void render(Painter painter, int mouseX, int mouseY, Component noCatalog, Component noThumbnails) {
        painter.fill(this.panel.x(), this.panel.y(),
                this.panel.x() + this.panel.width(), this.panel.y() + this.panel.height(),
                PANEL_BACKGROUND);
        painter.frame(this.panel.x(), this.panel.y(), this.panel.width(), this.panel.height(),
                PANEL_BORDER);

        if (this.regions.isEmpty()) {
            int width = Math.max(1, this.panel.width() - GAP * 2);
            painter.wrappedText(noCatalog, this.panel.x() + GAP,
                    this.panel.y() + Math.max(GAP, (this.panel.height() - wrappedHeight(noCatalog, width)) / 2),
                    width, LABEL_DIM);
            return;
        }

        this.renderRegionTabs(painter, mouseX, mouseY);
        this.renderCategoryIcons(painter, mouseX, mouseY);
        this.renderThumbnails(painter, mouseX, mouseY, noThumbnails);
    }

    private void renderRegionTabs(Painter painter, int mouseX, int mouseY) {
        for (int index = 0; index < this.regionTabs.size(); index++) {
            Rect tab = this.regionTabs.get(index);
            boolean open = index == this.region;
            boolean hovered = tab.contains(mouseX, mouseY);

            painter.fill(tab.x(), tab.y(), tab.x() + tab.width(), tab.y() + tab.height(),
                    open ? SLOT_HIGHLIGHT : SLOT_BACKGROUND);
            if (open || hovered) {
                painter.frame(tab.x(), tab.y(), tab.width(), tab.height(), open ? SELECTED : HOVERED);
            }
            painter.centeredText(regionLabel(this.regions.get(index)),
                    tab.x() + tab.width() / 2,
                    tab.y() + (tab.height() - lineHeight()) / 2 + 1,
                    open ? HOVERED : LABEL);
        }
    }

    private void renderCategoryIcons(Painter painter, int mouseX, int mouseY) {
        for (int index = 0; index < this.categoryIcons.size(); index++) {
            Rect icon = this.categoryIcons.get(index);
            CatalogCategory category = this.categories.get(index);
            boolean open = index == this.category;
            boolean hovered = icon.contains(mouseX, mouseY);

            this.renderSlot(painter, icon, open, hovered);

            // A category's icon is the head and shoulders of its first element, which
            // means it only appears once that category's atlas has arrived. Until then
            // the slot stands empty rather than the row rearranging itself later.
            CategorySprites loaded = this.sprites.apply(category);
            int first = category.items().isEmpty() ? -1 : category.items().get(0).atlasIndex();
            if (loaded != null && first >= 0 && first < loaded.count()) {
                painter.blit(loaded.texture(),
                        icon.x() + (icon.width() - FrontSprite.WIDTH) / 2,
                        icon.y() + (icon.height() - ICON_SOURCE_HEIGHT) / 2,
                        loaded.spriteU(first), loaded.spriteV(first),
                        FrontSprite.WIDTH, ICON_SOURCE_HEIGHT,
                        loaded.sheetWidth(), loaded.sheetHeight());
            }

            if (hovered) {
                painter.tooltip(name(category.name()), mouseX, mouseY);
            }
        }
    }

    private void renderThumbnails(Painter painter, int mouseX, int mouseY, Component noThumbnails) {
        CatalogCategory category = this.openCategory();
        if (category == null || this.viewport.height() <= 0) {
            return;
        }

        CategorySprites loaded = this.sprites.apply(category);
        if (loaded == null) {
            painter.wrappedText(noThumbnails, this.viewport.x(),
                    this.viewport.y()
                            + Math.max(0, (this.viewport.height()
                                    - wrappedHeight(noThumbnails, this.viewport.width())) / 2),
                    this.viewport.width(), LABEL_DIM);
            return;
        }

        List<CatalogItem> items = category.items();
        CatalogItem hoveredItem = null;

        // Only the strip scrolls, so only the strip is clipped: the choosers above it
        // are outside the scissor and cannot be scrolled out of view.
        painter.pushScissor(this.viewport.x(), this.viewport.y(),
                this.viewport.width(), this.viewport.height());
        for (int index = 0; index < items.size(); index++) {
            Rect slot = this.thumbnailSlot(index);
            if (slot.y() + slot.height() < this.viewport.y()
                    || slot.y() > this.viewport.y() + this.viewport.height()) {
                continue;
            }

            CatalogItem item = items.get(index);
            boolean selected = category.id().equals(this.selectedCategoryId)
                    && item.id().equals(this.selectedItemId);
            boolean hovered = slot.contains(mouseX, mouseY) && this.viewport.contains(mouseX, mouseY);
            if (hovered) {
                hoveredItem = item;
            }

            this.renderSlot(painter, slot, selected, hovered);
            if (item.atlasIndex() < loaded.count()) {
                painter.blit(loaded.texture(),
                        slot.x() + (slot.width() - FrontSprite.WIDTH) / 2,
                        slot.y() + (slot.height() - FrontSprite.HEIGHT) / 2,
                        loaded.spriteU(item.atlasIndex()), loaded.spriteV(item.atlasIndex()),
                        FrontSprite.WIDTH, FrontSprite.HEIGHT,
                        loaded.sheetWidth(), loaded.sheetHeight());
            }
        }
        painter.popScissor();

        this.renderScrollbar(painter, items.size());

        // The name of whatever is under the cursor, falling back to what is selected, so
        // the line never goes blank while the player moves around the strip.
        Component label = hoveredItem != null
                ? name(hoveredItem.name())
                : selectedItemLabel(category);
        if (label != null) {
            painter.text(ellipsize(label, this.labelLine.width()),
                    this.labelLine.x(), this.labelLine.y(),
                    hoveredItem != null ? LABEL : LABEL_DIM);
        }
        if (hoveredItem != null) {
            painter.tooltip(name(hoveredItem.name()), mouseX, mouseY);
        }
    }

    private Component selectedItemLabel(CatalogCategory category) {
        if (!category.id().equals(this.selectedCategoryId)) {
            return null;
        }
        for (CatalogItem item : category.items()) {
            if (item.id().equals(this.selectedItemId)) {
                return name(item.name());
            }
        }
        return null;
    }

    private void renderSlot(Painter painter, Rect slot, boolean selected, boolean hovered) {
        painter.fill(slot.x(), slot.y(), slot.x() + slot.width(), slot.y() + slot.height(),
                SLOT_BACKGROUND);
        // Two edges light, two dark: the sunken slot of an inventory.
        painter.fill(slot.x(), slot.y(), slot.x() + slot.width(), slot.y() + 1, SLOT_SHADOW);
        painter.fill(slot.x(), slot.y(), slot.x() + 1, slot.y() + slot.height(), SLOT_SHADOW);
        painter.fill(slot.x(), slot.y() + slot.height() - 1,
                slot.x() + slot.width(), slot.y() + slot.height(), SLOT_HIGHLIGHT);
        painter.fill(slot.x() + slot.width() - 1, slot.y(),
                slot.x() + slot.width(), slot.y() + slot.height(), SLOT_HIGHLIGHT);

        if (selected) {
            painter.frame(slot.x(), slot.y(), slot.width(), slot.height(), SELECTED);
        } else if (hovered) {
            painter.frame(slot.x(), slot.y(), slot.width(), slot.height(), HOVERED);
        }
    }

    private void renderScrollbar(Painter painter, int itemCount) {
        int content = this.contentHeight(itemCount);
        if (content <= this.viewport.height()) {
            return;
        }

        int trackHeight = this.viewport.height();
        int thumbHeight = Math.max(8, trackHeight * trackHeight / content);
        int travel = trackHeight - thumbHeight;
        int offset = travel * this.scroll / Math.max(1, content - trackHeight);
        int x = this.viewport.x() + this.viewport.width() - SCROLLBAR_WIDTH;
        painter.fill(x, this.viewport.y() + offset, x + SCROLLBAR_WIDTH,
                this.viewport.y() + offset + thumbHeight, SCROLLBAR);
    }

    boolean mouseClicked(double mouseX, double mouseY) {
        for (int index = 0; index < this.regionTabs.size(); index++) {
            if (this.regionTabs.get(index).contains(mouseX, mouseY)) {
                this.openRegion(index);
                return true;
            }
        }
        for (int index = 0; index < this.categoryIcons.size(); index++) {
            if (this.categoryIcons.get(index).contains(mouseX, mouseY)) {
                this.openCategory(index);
                return true;
            }
        }

        CatalogCategory category = this.openCategory();
        if (category == null || !this.viewport.contains(mouseX, mouseY)) {
            return false;
        }
        for (int index = 0; index < category.items().size(); index++) {
            if (this.thumbnailSlot(index).contains(mouseX, mouseY)) {
                CatalogItem item = category.items().get(index);
                this.selectedCategoryId = category.id();
                this.selectedItemId = item.id();
                this.listener.onElementPicked(category, item);
                return true;
            }
        }
        return this.panel.contains(mouseX, mouseY);
    }

    boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!this.viewport.contains(mouseX, mouseY)) {
            return false;
        }
        this.scroll -= (int) Math.round(amount * SCROLL_STEP);
        this.clampScroll();
        return true;
    }

    private void openRegion(int index) {
        if (index == this.region || index < 0 || index >= this.regions.size()) {
            return;
        }
        this.region = index;
        this.categories = this.catalog.categoriesIn(this.regions.get(index));
        this.category = 0;
        this.scroll = 0;
        this.layout(this.panel);
        this.listener.onCategoriesChanged();
    }

    private void openCategory(int index) {
        if (index == this.category || index < 0 || index >= this.categories.size()) {
            return;
        }
        this.category = index;
        this.scroll = 0;
        this.clampScroll();
    }

    private Rect thumbnailSlot(int index) {
        int column = index % this.columns;
        int row = index / this.columns;
        return new Rect(
                this.viewport.x() + column * (THUMBNAIL_SLOT_WIDTH + GAP),
                this.viewport.y() + row * (THUMBNAIL_SLOT_HEIGHT + GAP) - this.scroll,
                THUMBNAIL_SLOT_WIDTH,
                THUMBNAIL_SLOT_HEIGHT);
    }

    private int contentHeight(int itemCount) {
        int rows = (itemCount + this.columns - 1) / this.columns;
        return Math.max(0, rows * (THUMBNAIL_SLOT_HEIGHT + GAP) - GAP);
    }

    private void clampScroll() {
        CatalogCategory category = this.openCategory();
        int content = category == null ? 0 : this.contentHeight(category.items().size());
        this.scroll = Math.max(0, Math.min(this.scroll, Math.max(0, content - this.viewport.height())));
    }

    /**
     * A region's label. Regions are catalogue keys rather than translated text, so the
     * mod translates them itself and shows the raw key for one it does not know - the
     * same rule the site's dictionaries follow for colour keys.
     */
    private static Component regionLabel(String region) {
        if (region.isEmpty()) {
            return Component.translatable("library.mcskincreator.region.all");
        }
        String key = "library.mcskincreator.region." + region;
        return Language.getInstance().has(key) ? Component.translatable(key) : Component.literal(region);
    }

    private static Component name(CatalogText text) {
        String label = text.forLanguage(Minecraft.getInstance().getLanguageManager().getSelected());
        return label.isBlank() ? Component.literal("?") : Component.literal(label);
    }

    private static int textWidth(Component text) {
        return Minecraft.getInstance().font.width(text);
    }

    /** How tall {@code text} is once broken to {@code width}. */
    static int wrappedHeight(Component text, int width) {
        return Math.max(1, Minecraft.getInstance().font.split(text, Math.max(1, width)).size())
                * lineHeight();
    }

    /**
     * {@code text} cut to {@code width} with an ellipsis, for the one line that has to
     * stay one line. An element's name comes from the catalogue, so its length is not
     * the mod's to decide and a long one would otherwise run past the panel.
     */
    private static Component ellipsize(Component text, int width) {
        if (textWidth(text) <= width) {
            return text;
        }
        String ellipsis = "…";
        int room = Math.max(0, width - textWidth(Component.literal(ellipsis)));
        return Component.literal(
                Minecraft.getInstance().font.plainSubstrByWidth(text.getString(), room) + ellipsis);
    }

    private static int lineHeight() {
        return Minecraft.getInstance().font.lineHeight;
    }

    /** A rectangle in screen pixels. */
    private record Rect(int x, int y, int width, int height) {
        static final Rect EMPTY = new Rect(0, 0, 0, 0);

        boolean contains(double pointX, double pointY) {
            return pointX >= this.x && pointX < this.x + this.width
                    && pointY >= this.y && pointY < this.y + this.height;
        }
    }
}
