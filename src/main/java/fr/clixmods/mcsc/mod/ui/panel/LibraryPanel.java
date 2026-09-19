/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui.panel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import fr.clixmods.mcsc.mod.catalog.Catalog;
import fr.clixmods.mcsc.mod.catalog.CatalogCategory;
import fr.clixmods.mcsc.mod.catalog.CatalogItem;
import fr.clixmods.mcsc.mod.catalog.CatalogModel;
import fr.clixmods.mcsc.mod.catalog.CatalogText;
import fr.clixmods.mcsc.mod.catalog.ThumbCrop;
import fr.clixmods.mcsc.mod.remote.SearchResults;
import fr.clixmods.mcsc.mod.skin.CategorySprites;
import fr.clixmods.mcsc.mod.style.Metrics;
import fr.clixmods.mcsc.mod.style.Palette;
import fr.clixmods.mcsc.mod.style.Surface;
import fr.clixmods.mcsc.mod.ui.Canvas;
import fr.clixmods.mcsc.mod.ui.Element;
import fr.clixmods.mcsc.mod.ui.Paint;
import fr.clixmods.mcsc.mod.ui.ScrollPane;
import fr.clixmods.mcsc.mod.ui.widget.CategoryTab;
import fr.clixmods.mcsc.mod.ui.widget.Dropdown;
import fr.clixmods.mcsc.mod.ui.widget.ItemTile;
import fr.clixmods.mcsc.mod.ui.widget.PixelButton;
import fr.clixmods.mcsc.mod.ui.widget.TextInput;
import fr.clixmods.mcsc.mod.ui.widget.Thumbnail;
import net.minecraft.network.chat.Component;

/**
 * The left column: four bands, of which exactly one scrolls.
 *
 * <p>Regions, then categories as a fixed row of icons, then the search box, then the
 * elements. The category row deliberately sits outside the scroll and the body shows
 * one category at a time — the earlier version of the site stacked every open
 * category inside the scroll, and going from hair to hats meant travelling past forty
 * thumbnails.
 *
 * <p>Search is the one case where several batches show at once, because it crosses
 * regions: one searches for a beanie, not for a beanie in the head region. While a
 * search is showing, the green region tab goes out — it would otherwise be announcing
 * a category the panel is not displaying — and clicking any tab clears the search,
 * because a tab always shows its own category whatever came before.
 *
 * <p>The matching itself is the server's, which is the only way a name the player's
 * language does not show can be matched: the catalogue carries three, and only one of
 * them is on screen. Until the answer arrives — and if it never does — the panel
 * filters the labels it holds, so the field always does something.
 *
 * <p>A Minecraft screen has no layout: there is no flexbox and no grid, so
 * {@link #layout} works every position out in pixels and the scrolling is counted by
 * hand. The arithmetic is gathered there rather than spread through the drawing,
 * because the panel has to hold together at every window size and all four GUI
 * scales, and that is easier to see when the sizes are in one place.
 */
public class LibraryPanel extends Panel {
    private final Runnable relayout;
    private final Function<CatalogText, Component> naming;
    private final Function<String, CategorySprites> sprites;
    private final Supplier<Boolean> slim;
    private final Predicate<CatalogItem> used;
    private final Consumer<ItemTile> onPick;
    private final Consumer<CatalogModel> onOutfit;
    private final Consumer<ItemTile> onInfo;
    private final Consumer<ItemTile> onHover;
    private final Consumer<String> onFooterLink;
    private final Runnable onCategoriesChanged;

    private final ScrollPane scroll = new ScrollPane();
    private final List<PixelButton> regionTabs = new ArrayList<>();
    private final List<CategoryTab> categoryTabs = new ArrayList<>();
    private final List<PlacedTile> tiles = new ArrayList<>();
    private final List<GroupHeader> headers = new ArrayList<>();
    private final List<PixelButton> footerLinks = new ArrayList<>();

    private Catalog catalog = Catalog.EMPTY;
    private TextInput search;
    private PixelButton foldButton;
    /** The query the results below answer, which is not always the one being typed. */
    private String answeredQuery = "";
    private List<SearchResults.Hit> hits = List.of();

    private String region;
    private Dropdown<String> regionChooser;
    private CatalogCategory category;
    /** Where the rule under the region tabs goes: they are a tab bar, so they have one. */
    private int regionsBottom;
    private int bodyTop;
    private int bodyHeight;
    /** What to say while the body has nothing in it, which is not always the same thing. */
    private Supplier<Component> emptyMessage = Component::empty;

    /** A batch title inside the body: the icon, the name, and how many there are. */
    private record GroupHeader(CatalogCategory category, Component label, int count, int y) {
    }

    /**
     * A tile and where it sits in the body, measured from the top of the content
     * rather than the top of the screen.
     */
    private record PlacedTile(ItemTile tile, int contentY) {
    }

    public LibraryPanel(Runnable relayout, Function<CatalogText, Component> naming,
                        Function<String, CategorySprites> sprites, Supplier<Boolean> slim,
                        Predicate<CatalogItem> used, Consumer<ItemTile> onPick,
                        Consumer<CatalogModel> onOutfit, Consumer<ItemTile> onInfo,
                        Consumer<ItemTile> onHover, Consumer<String> onFooterLink,
                        Runnable onCategoriesChanged) {
        super("panel.mcskincreator.library", true);
        this.relayout = relayout;
        this.naming = naming;
        this.sprites = sprites;
        this.slim = slim;
        this.used = used;
        this.onPick = onPick;
        this.onOutfit = onOutfit;
        this.onInfo = onInfo;
        this.onHover = onHover;
        this.onFooterLink = onFooterLink;
        this.onCategoriesChanged = onCategoriesChanged;
    }

    /**
     * The region the outfits browse in.
     *
     * <p>The site gives them a shelf of their own between the body and the head, and
     * they are picked exactly the way an element is — so they are one more region here
     * too, rather than a button somewhere else. The catalogue does not carry the region
     * itself: outfits are a list beside the categories, not a category, so the tab is
     * added to the ones it does carry.
     */
    public static final String OUTFIT_REGION = "outfit";

    /**
     * The key the outfits' sheet of thumbnails is kept under, beside the categories'.
     *
     * <p>Not {@code "outfit"}: that is a region, and a catalogue is free to name a real
     * category anything: two sheets under one key would draw each other's pixels.
     */
    public static final String OUTFIT_SHEET = "ready-made-outfits";

    /**
     * The shelf the outfit tiles stand on.
     *
     * <p>A category in shape only, and it never leaves this panel: it lets an outfit be
     * drawn and picked by the same tile as everything else in the library instead of a
     * second widget that would have to be kept looking the same. What a pick means is
     * decided here, and the project only ever sees a real {@link CatalogModel}.
     */
    private CatalogCategory outfitShelf() {
        List<CatalogItem> items = new ArrayList<>(this.catalog.outfits().size());
        List<CatalogModel> outfits = this.catalog.outfits();
        for (int index = 0; index < outfits.size(); index++) {
            // The rank is the outfit's place in the sheet built from the same list.
            // No credit: the catalogue credits the elements an outfit is made of, one
            // work each, and has nothing to say about the set.
            items.add(new CatalogItem(outfits.get(index).id(), outfits.get(index).name(),
                    index, CatalogItem.NONE, ThumbCrop.ALL, ""));
        }
        return new CatalogCategory(OUTFIT_SHEET, OUTFIT_REGION,
                new CatalogText("", "", ""), false, ThumbCrop.ALL, "", items);
    }

    /** Whether the outfits' shelf is the one on screen, so its sheet is worth drawing. */
    public boolean showingOutfits() {
        return OUTFIT_REGION.equals(this.region) && !searching();
    }

    /** The regions to offer: the catalogue's, with the outfits' shelf after the body. */
    private List<String> regions() {
        return regionsWith(this.catalog.regions(), !this.catalog.outfits().isEmpty());
    }

    /**
     * Where the outfits' tab goes: straight after {@code base}, which is the site's
     * order, and first when the catalogue has no body region to put it after.
     *
     * <p>A catalogue with no outfit gets no tab at all, by the rule the rest of the
     * interface follows: a control whose target is empty is absent, not dead.
     */
    static List<String> regionsWith(List<String> catalogRegions, boolean hasOutfits) {
        List<String> regions = new ArrayList<>(catalogRegions);
        if (!hasOutfits) {
            return List.copyOf(regions);
        }
        int at = regions.indexOf("base");
        regions.add(at < 0 ? 0 : at + 1, OUTFIT_REGION);
        return List.copyOf(regions);
    }

    public boolean hasCatalog() {
        return !this.catalog.isEmpty();
    }

    /** Hands the panel the catalogue. Done once: doing it again resets the browsing. */
    public void setCatalog(Catalog catalog) {
        this.catalog = catalog;
        this.region = regions().isEmpty() ? null : regions().get(0);
        this.category = firstCategory(this.region);
        this.scroll.reset();
        this.relayout.run();
        this.onCategoriesChanged.run();
    }

    /** What to say when there is nothing to show — loading, empty, or a failure. */
    public void setEmptyMessage(Supplier<Component> message) {
        this.emptyMessage = message;
    }

    /**
     * The categories on screen, which are the ones whose pixels are worth fetching.
     *
     * <p>The outfits' shelf is the one that reaches outside itself: an outfit is made
     * of elements from all over the library, and none of them can be drawn without its
     * category. The body it is stood on adds the skin category to that.
     */
    public List<CatalogCategory> visibleCategories() {
        if (this.region == null) {
            return List.of();
        }
        if (!OUTFIT_REGION.equals(this.region)) {
            return this.catalog.categoriesIn(this.region);
        }
        LinkedHashSet<CatalogCategory> needed = new LinkedHashSet<>();
        for (CatalogCategory category : this.catalog.categories()) {
            if (category.single()) {
                needed.add(category);   // the body under the clothes
                break;
            }
        }
        for (CatalogModel outfit : this.catalog.outfits()) {
            needed.addAll(this.catalog.categoriesOf(outfit));
        }
        return List.copyOf(needed);
    }

    /** @param onQuery told what is being searched for, so the server can be asked */
    public void createSearch(Consumer<String> onQuery) {
        this.search = new TextInput(Component.translatable("gui.mcskincreator.search"),
                Metrics.MAX_NAME_CHARS, value -> {
                    this.scroll.reset();
                    onQuery.accept(value);
                    this.relayout.run();
                });
    }

    /**
     * Hands the panel what the server matched.
     *
     * <p>Kept with the query it answers: a result set is shown only while it is the
     * answer to what is in the field, so a slow reply cannot leave the panel showing
     * the matches of a word that has since been rewritten.
     */
    public void setSearchResults(String query, List<SearchResults.Hit> hits) {
        this.answeredQuery = query;
        this.hits = List.copyOf(hits);
    }

    private CatalogCategory firstCategory(String region) {
        if (OUTFIT_REGION.equals(region)) {
            return outfitShelf();
        }
        List<CatalogCategory> list = region == null ? List.of() : this.catalog.categoriesIn(region);
        return list.isEmpty() ? null : list.get(0);
    }

    private boolean searching() {
        return this.search != null && !this.search.isEmpty();
    }

    /**
     * The name of a region.
     *
     * <p>Regions are identifiers the catalogue chose, so the mod translates the ones it
     * knows and shows the identifier itself for one it has never heard of — a new
     * region appearing on the site reads as its own name rather than as a blank tab.
     */
    static Component regionLabel(String region) {
        String key = "library.mcskincreator.region." + (region.isEmpty() ? "all" : region);
        Component translated = Component.translatable(key);
        return translated.getString().equals(key) ? Component.literal(region) : translated;
    }

    @Override
    public void layout(Canvas canvas) {
        clearChildren();
        this.regionTabs.clear();
        this.regionChooser = null;
        this.categoryTabs.clear();
        this.tiles.clear();
        this.headers.clear();
        this.footerLinks.clear();

        int header = headerHeight(canvas);
        this.foldButton = new PixelButton(foldLabel(), PixelButton.Style.NORMAL, () -> {
            toggleFolded();
            this.relayout.run();
        });
        this.foldButton.withTooltip(Component.translatable(foldTooltipKey()));
        this.foldButton.fit(canvas);

        if (folded()) {
            // Folded, the header carries the unfold button and nothing else.
            this.foldButton.setBounds(this.x + (this.width - this.foldButton.width()) / 2,
                    this.y + (header - Metrics.HEADER_BUTTON) / 2,
                    this.foldButton.width(), Metrics.HEADER_BUTTON);
            addChild(this.foldButton);
            return;
        }

        this.foldButton.setBounds(
                contentRight() - this.foldButton.width(),
                this.y + (header - Metrics.HEADER_BUTTON) / 2,
                this.foldButton.width(), Metrics.HEADER_BUTTON);
        addChild(this.foldButton);

        int left = contentLeft();
        int right = contentRight();
        int cursorY = this.y + header + Metrics.PAD_TIGHT;

        cursorY = layoutRegions(canvas, left, right, cursorY);
        cursorY = layoutCategories(left, right, cursorY);

        this.search.setBounds(left, cursorY, right - left, Metrics.BUTTON_HEIGHT_COMPACT);
        addChild(this.search);
        cursorY += Metrics.BUTTON_HEIGHT_COMPACT + Metrics.PAD_TIGHT;

        int footerHeight = canvas.lineHeight() + Metrics.PAD_TIGHT * 2;
        int footerTop = this.y + this.height - Metrics.PANEL_INSET - footerHeight;
        this.bodyTop = cursorY;
        this.bodyHeight = Math.max(0, footerTop - cursorY - Metrics.PAD_TIGHT);

        layoutBody(canvas, left, right);
        layoutFooter(canvas, left, right, footerTop);
    }

    /**
     * The region chooser: a row of tabs while they fit on one row, a dropdown below
     * that.
     *
     * <p>Wrapping was the third option and it is the one that had to go. Five regions
     * in a column a hundred pixels wide wrapped onto three rows, which cost a third of
     * the panel and stopped reading as a tab bar at all — a tab bar is a row, and two
     * rows of tabs are a grid of boxes. The scene's view chooser already collapses the
     * same way when its strip runs out of room, so this is not a second idea.
     */
    private int layoutRegions(Canvas canvas, int left, int right, int top) {
        List<String> regions = regions();
        if (regions.isEmpty()) {
            this.regionsBottom = top;
            return top;
        }

        List<PixelButton> tabs = new ArrayList<>();
        int total = 0;
        for (String candidate : regions) {
            PixelButton tab = new PixelButton(regionLabel(candidate), PixelButton.Style.TAB,
                    () -> pickRegion(candidate));
            tab.fit(canvas);
            // The tab goes unselected while a search is showing: it would otherwise
            // claim a category the body is not displaying.
            tab.setActive(!searching() && candidate.equals(this.region));
            tabs.add(tab);
            total += tab.width() + Metrics.SEGMENT_GAP;
        }

        if (total - Metrics.SEGMENT_GAP <= right - left) {
            int cursorX = left;
            for (PixelButton tab : tabs) {
                tab.setBounds(cursorX, top, tab.width(), Metrics.TAB_HEIGHT);
                this.regionTabs.add(addChild(tab));
                cursorX += tab.width() + Metrics.SEGMENT_GAP;
            }
            // The game's tab sprite has no bottom edge: it is drawn to sit on the thing
            // it opens. Floating one in the middle of a column is what made these read
            // as brackets rather than tabs, so the row ends flush and a rule closes it.
            this.regionsBottom = top + Metrics.TAB_HEIGHT;
            return this.regionsBottom + Metrics.PAD_TIGHT;
        }

        Dropdown<String> chooser = new Dropdown<>(regions, LibraryPanel::regionLabel,
                () -> this.region, this::pickRegion, candidate -> true);
        chooser.setBounds(left, top, right - left, Metrics.TAB_HEIGHT);
        chooser.inScreen(this.y + this.height);
        this.regionChooser = addChild(chooser);
        this.regionsBottom = top + Metrics.TAB_HEIGHT;
        return this.regionsBottom + Metrics.PAD_TIGHT;
    }

    private int layoutCategories(int left, int right, int top) {
        if (showingOutfits()) {
            // One shelf, so no row to choose from: a single tab that cannot be
            // unchosen is a control that does nothing.
            return top;
        }
        int cursorX = left;
        int cursorY = top;
        for (CatalogCategory candidate : visibleCategories()) {
            CategoryTab tab = new CategoryTab(candidate, this.naming.apply(candidate.name()),
                    () -> searching() ? null : this.category,
                    () -> this.sprites.apply(candidate.id()),
                    this::pickCategory);
            if (cursorX + Metrics.CATEGORY_TAB > right && cursorX > left) {
                cursorX = left;
                cursorY += Metrics.CATEGORY_TAB + Metrics.SEGMENT_GAP;
            }
            tab.setBounds(cursorX, cursorY, Metrics.CATEGORY_TAB, Metrics.CATEGORY_TAB);
            this.categoryTabs.add(addChild(tab));
            cursorX += Metrics.CATEGORY_TAB + Metrics.SEGMENT_GAP;
        }
        return cursorY + Metrics.CATEGORY_TAB + Metrics.PAD_TIGHT;
    }

    /**
     * Lays the body out in content coordinates — the scroll offset is applied when the
     * tiles are placed, so a wheel notch moves an integer and nothing is measured
     * again.
     */
    private void layoutBody(Canvas canvas, int left, int right) {
        Map<CatalogCategory, List<CatalogItem>> batches = batches();

        int gutter = ScrollPane.BAR_WIDTH + Metrics.PAD_TIGHT;
        int usable = right - left - gutter;
        // Three columns, the way the site's own grid is three columns of
        // minmax(0, 1fr): a column always holds three whatever it is wide, and the
        // tiles share whatever that leaves. It drops to two only in the drawer, where
        // the panel is half a narrow screen and three would be thumbnails nobody can
        // tell apart.
        int columns = usable / Metrics.GRID_COLUMNS >= Metrics.MIN_TILE_WIDTH
                ? Metrics.GRID_COLUMNS
                : 2;
        int tileWidth = Math.max(Metrics.CATEGORY_TAB,
                (usable - Metrics.GRID_GAP * (columns - 1)) / columns);
        int headerHeight = canvas.lineHeight() + Metrics.PAD_TIGHT * 2;

        int cursorY = 0;
        for (Map.Entry<CatalogCategory, List<CatalogItem>> batch : batches.entrySet()) {
            CatalogCategory batchCategory = batch.getKey();
            Component label = this.naming.apply(batchCategory.name());
            this.headers.add(new GroupHeader(batchCategory, label, batch.getValue().size(), cursorY));
            cursorY += headerHeight;

            // One height for every tile, whatever the crop, because that is what makes
            // a grid a grid: the site letterboxes each thumbnail into a fixed box, and
            // sizing each tile to its own crop instead gave a column of ragged rows with
            // full-body tiles twice as tall as head ones.
            int tileHeight = ItemTile.heightFor(canvas, Metrics.THUMB_RENDER);

            boolean outfits = OUTFIT_SHEET.equals(batchCategory.id());
            int column = 0;
            for (CatalogItem item : batch.getValue()) {
                ItemTile tile = new ItemTile(batchCategory, item, this.naming.apply(item.name()),
                        () -> this.sprites.apply(batchCategory.id()), this.slim,
                        outfits ? this::outfitIsOn : this.used,
                        outfits ? this::pickOutfit : this.onPick,
                        // No provenance mark on an outfit: the catalogue credits the
                        // elements it is made of, one work each, not the set.
                        outfits ? null : this.onInfo,
                        this.onHover);
                tile.setBounds(left + column * (tileWidth + Metrics.GRID_GAP), cursorY,
                        tileWidth, tileHeight);
                this.tiles.add(new PlacedTile(addChild(tile), cursorY));
                if (++column == columns) {
                    column = 0;
                    cursorY += tileHeight + Metrics.GRID_GAP;
                }
            }
            if (column != 0) {
                cursorY += tileHeight + Metrics.GRID_GAP;
            }
        }

        this.scroll.setContent(cursorY, this.bodyHeight);
    }

    /** What the body shows: the chosen category, or every match of the search. */
    private Map<CatalogCategory, List<CatalogItem>> batches() {
        Map<CatalogCategory, List<CatalogItem>> batches = new LinkedHashMap<>();
        if (!searching()) {
            if (this.category != null) {
                batches.put(this.category, this.category.items());
            }
            return batches;
        }

        String needle = this.search.value().trim();
        return needle.equals(this.answeredQuery) && !this.hits.isEmpty()
                ? matched(batches)
                : filtered(batches, needle.toLowerCase(Locale.ROOT));
    }

    /**
     * What the server matched, grouped by category in the catalogue's order.
     *
     * <p>Outfits come back in the same answer and are left out: they are a list the mod
     * does not have, and a match one cannot pick is a row that does nothing.
     */
    private Map<CatalogCategory, List<CatalogItem>> matched(Map<CatalogCategory, List<CatalogItem>> batches) {
        for (SearchResults.Hit hit : this.hits) {
            if (!hit.isPreset()) {
                continue;
            }
            this.catalog.category(hit.category()).ifPresent(category -> category.items().stream()
                    .filter(item -> item.id().equals(hit.id()))
                    .findFirst()
                    .ifPresent(item -> batches.computeIfAbsent(category, key -> new ArrayList<>()).add(item)));
        }
        return batches;
    }

    /** What the panel can match on its own: the labels of the player's language. */
    private Map<CatalogCategory, List<CatalogItem>> filtered(
            Map<CatalogCategory, List<CatalogItem>> batches, String needle) {
        for (CatalogCategory candidate : this.catalog.categories()) {
            List<CatalogItem> matches = candidate.items().stream()
                    .filter(item -> this.naming.apply(item.name()).getString()
                            .toLowerCase(Locale.ROOT).contains(needle))
                    .toList();
            if (!matches.isEmpty()) {
                batches.put(candidate, new ArrayList<>(matches));
            }
        }
        return batches;
    }

    private void layoutFooter(Canvas canvas, int left, int right, int top) {
        int cursorX = left;
        for (String link : List.of("about", "credits", "beta", "legal")) {
            PixelButton button = new PixelButton(
                    Component.translatable("footer.mcskincreator." + link),
                    PixelButton.Style.GHOST, () -> this.onFooterLink.accept(link));
            button.fit(canvas);
            if (cursorX + button.width() > right) {
                break;
            }
            button.setBounds(cursorX, top, button.width(), canvas.lineHeight() + Metrics.PAD_TIGHT * 2);
            this.footerLinks.add(addChild(button));
            cursorX += button.width();
        }
    }

    private void pickRegion(String picked) {
        // A tab always shows its own category, whatever the panel was showing before.
        this.search.clearQuietly();
        this.region = picked;
        this.category = firstCategory(picked);
        this.scroll.reset();
        this.relayout.run();
        this.onCategoriesChanged.run();
    }

    /** The outfit a shelf tile stands for, by the id the two share. */
    private CatalogModel outfitFor(CatalogItem item) {
        for (CatalogModel outfit : this.catalog.outfits()) {
            if (outfit.id().equals(item.id())) {
                return outfit;
            }
        }
        return null;
    }

    private void pickOutfit(ItemTile tile) {
        CatalogModel outfit = outfitFor(tile.item());
        if (outfit != null) {
            this.onOutfit.accept(outfit);
        }
    }

    /** An outfit reads as worn once every piece of it is in the stack. */
    private boolean outfitIsOn(CatalogItem item) {
        CatalogModel outfit = outfitFor(item);
        if (outfit == null) {
            return false;
        }
        for (CatalogModel.Piece piece : outfit.pieces()) {
            CatalogCategory category = this.catalog.category(piece.categoryId()).orElse(null);
            CatalogItem piecedItem = category == null ? null : category.items().stream()
                    .filter(candidate -> candidate.id().equals(piece.itemId()))
                    .findFirst().orElse(null);
            if (piecedItem == null || !this.used.test(piecedItem)) {
                return false;
            }
        }
        return true;
    }

    private void pickCategory(CatalogCategory picked) {
        this.search.clearQuietly();
        this.category = picked;
        this.scroll.reset();
        this.relayout.run();
    }

    @Override
    public void draw(Paint paint) {
        Canvas canvas = paint.canvas();
        drawFrame(paint);
        this.foldButton.draw(paint);
        if (folded()) {
            return;
        }

        for (PixelButton tab : this.regionTabs) {
            tab.draw(paint);
        }
        if (this.regionChooser != null) {
            this.regionChooser.draw(paint);
        } else {
            // The rule the tabs stand on, which is what tells a row of tabs from a row
            // of dark boxes. Drawn after them so the selected one's open foot lands on
            // it.
            Surface.rule(canvas, contentLeft(), this.regionsBottom, contentWidth());
        }
        for (CategoryTab tab : this.categoryTabs) {
            tab.draw(paint);
        }
        this.search.draw(paint);

        int left = contentLeft();
        int right = contentRight();

        if (this.tiles.isEmpty()) {
            // Never a blank area: an empty state always says what to do about it, and
            // what it says depends on why there is nothing there.
            Component message = searching()
                    ? Component.translatable("empty.mcskincreator.search")
                    : this.emptyMessage.get();
            int room = Math.max(1, right - left);
            canvas.textWrapped(message, left,
                    this.bodyTop + (this.bodyHeight - canvas.wrappedHeight(message, room)) / 2,
                    room, Palette.INK_MUTED);
        } else {
            placeTiles();
            canvas.pushScissor(left, this.bodyTop, right - left, this.bodyHeight);
            int offset = this.bodyTop - this.scroll.offset();
            for (GroupHeader header : this.headers) {
                drawGroupHeader(canvas, header, left, offset + header.y());
            }
            for (PlacedTile placed : this.tiles) {
                if (onScreen(placed.tile())) {
                    placed.tile().draw(paint);
                }
            }
            canvas.popScissor();
            this.scroll.drawBar(canvas, right, this.bodyTop, this.bodyHeight);
        }

        for (PixelButton link : this.footerLinks) {
            link.draw(paint);
        }
    }

    /** Moves every tile to where this frame puts it. Called once, before drawing. */
    private void placeTiles() {
        int offset = this.bodyTop - this.scroll.offset();
        for (PlacedTile placed : this.tiles) {
            ItemTile tile = placed.tile();
            tile.setBounds(tile.x(), offset + placed.contentY(), tile.width(), tile.height());
            tile.clipTo(this.x, this.bodyTop, this.width, this.bodyHeight);
        }
    }

    /** A tile scrolled out of the band is not drawn, and is not clickable either. */
    private boolean onScreen(ItemTile tile) {
        return tile.y() + tile.height() > this.bodyTop && tile.y() < this.bodyTop + this.bodyHeight;
    }

    /**
     * A batch title: the category's name, how many it holds, and a rule across the
     * rest of the row — the shape the game gives its own section headings.
     */
    private void drawGroupHeader(Canvas canvas, GroupHeader header, int left, int y) {
        canvas.text(header.label(), left, y, Palette.INK);

        String count = Integer.toString(header.count());
        int countWidth = canvas.textWidth(count);
        int nameWidth = canvas.textWidth(header.label());
        int ruleX = left + nameWidth + Metrics.PAD_TIGHT;
        int right = contentRight() - ScrollPane.BAR_WIDTH;
        int ruleWidth = right - countWidth - Metrics.PAD_TIGHT - ruleX;
        if (ruleWidth > 0) {
            Surface.rule(canvas, ruleX, y + canvas.lineHeight() / 2, ruleWidth);
        }
        canvas.textFlat(Component.literal(count), right - countWidth, y, Palette.INK_MUTED);
    }

    /**
     * Everything that can be clicked or focused, in reading order.
     *
     * <p>A tile scrolled out of the band is left out entirely rather than merely not
     * drawn: invisible but still clickable is how people press buttons they never saw.
     */
    public List<Element> hitTargets() {
        List<Element> targets = new ArrayList<>();
        targets.add(this.foldButton);
        if (folded()) {
            return targets;
        }
        targets.addAll(this.regionTabs);
        if (this.regionChooser != null) {
            targets.add(this.regionChooser);
        }
        targets.addAll(this.categoryTabs);
        targets.add(this.search);
        targets.addAll(this.footerLinks);
        placeTiles();
        for (PlacedTile placed : this.tiles) {
            if (onScreen(placed.tile())) {
                targets.add(placed.tile());
            }
        }
        return targets;
    }

    @Override
    public boolean scroll(double mouseX, double mouseY, double amount) {
        if (!inBody(mouseX, mouseY, this.bodyTop, this.bodyHeight)) {
            return false;
        }
        return this.scroll.scroll(amount);
    }

    @Override
    public boolean mouseDown(double mouseX, double mouseY, int button) {
        if (folded() || button != 0) {
            return false;
        }
        return this.scroll.barMouseDown(mouseX, mouseY,
                contentRight(), this.bodyTop, this.bodyHeight);
    }

    @Override
    public void mouseDrag(double mouseX, double mouseY, double dragX, double dragY, int button) {
        this.scroll.barMouseDrag(mouseY, this.bodyTop, this.bodyHeight);
    }

    @Override
    public void mouseUp(double mouseX, double mouseY, int button) {
        this.scroll.barMouseUp();
    }
}
