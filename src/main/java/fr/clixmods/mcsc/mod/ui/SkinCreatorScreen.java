/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.JsonParser;

import fr.clixmods.mcsc.mod.MCSkinCreatorClient;
import fr.clixmods.mcsc.mod.account.AccountSkin;
import fr.clixmods.mcsc.mod.account.SkinUploadException;
import fr.clixmods.mcsc.mod.catalog.Catalog;
import fr.clixmods.mcsc.mod.catalog.CatalogCategory;
import fr.clixmods.mcsc.mod.catalog.CatalogFormatException;
import fr.clixmods.mcsc.mod.catalog.CatalogItem;
import fr.clixmods.mcsc.mod.catalog.CatalogText;
import fr.clixmods.mcsc.mod.catalog.CatalogWork;
import fr.clixmods.mcsc.mod.project.History;
import fr.clixmods.mcsc.mod.project.Layer;
import fr.clixmods.mcsc.mod.project.SkinProject;
import fr.clixmods.mcsc.mod.remote.ApiException;
import fr.clixmods.mcsc.mod.remote.ItemCredit;
import fr.clixmods.mcsc.mod.remote.McscApi;
import fr.clixmods.mcsc.mod.remote.SavedSkin;
import fr.clixmods.mcsc.mod.remote.SearchResults;
import fr.clixmods.mcsc.mod.skin.AppliedSkin;
import fr.clixmods.mcsc.mod.skin.CategorySprites;
import fr.clixmods.mcsc.mod.skin.PreviewSkin;
import fr.clixmods.mcsc.mod.skin.ProjectJson;
import fr.clixmods.mcsc.mod.skin.SkinThumbnails;
import fr.clixmods.mcsc.mod.style.Metrics;
import fr.clixmods.mcsc.mod.style.Palette;
import fr.clixmods.mcsc.mod.style.Surface;
import fr.clixmods.mcsc.mod.style.Tiles;
import fr.clixmods.mcsc.mod.ui.panel.LayersPanel;
import fr.clixmods.mcsc.mod.ui.panel.LibraryPanel;
import fr.clixmods.mcsc.mod.ui.panel.ScenePanel;
import fr.clixmods.mcsc.mod.ui.panel.TopBar;
import fr.clixmods.mcsc.mod.ui.widget.Dropdown;
import fr.clixmods.mcsc.mod.ui.widget.ItemTile;
import fr.clixmods.mcsc.mod.ui.widget.PixelButton;
import fr.clixmods.mcsc.mod.ui.window.CardWindow;
import fr.clixmods.mcsc.mod.ui.window.ConfirmWindow;
import fr.clixmods.mcsc.mod.ui.window.ModalWindow;
import fr.clixmods.mcsc.mod.ui.window.NameWindow;
import fr.clixmods.mcsc.mod.ui.window.SkinsWindow;
import fr.clixmods.mcsc.mod.ui.window.TextWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
//? if >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?} else {
import net.minecraft.client.gui.GuiGraphics;
//?}
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerModelType;
import org.lwjgl.glfw.GLFW;

/**
 * The editor.
 *
 * <p>Three zones — library, scene, layers — laid out as three columns while the width
 * allows it and as drawers below that, with the scene always on screen. That last part
 * is the rule that does not bend: choosing an element while the model is hidden is
 * choosing blind, and the site tried two arrangements that did exactly that before
 * settling on this one.
 *
 * <p>A Minecraft screen has no layout engine, so everything here is arithmetic done in
 * {@link #relayout()} and redone whenever the window, the fold state or the contents
 * change. The widgets are the mod's own rather than the game's, because the site's
 * look is a material and a bevel rather than a skin over a button — but the mechanics
 * the game is right about are the game's: its font, its GUI scale, the player model,
 * and a focus ring a keyboard can walk, which is also how everything the site reveals
 * on hover stays reachable without a mouse.
 *
 * <p>Nothing on screen waits on the network. Stacking an element shows it straight
 * away from the atlas buffer already in memory, and the server's composition replaces
 * it when it lands; a server that cannot compose costs a notification, not the
 * preview. Every texture this screen builds is released in {@link #removed()}, and a
 * reply arriving after it has gone is dropped rather than uploaded.
 */
public class SkinCreatorScreen extends Screen {
    /** Below this width the side columns become drawers. */
    private static final int COLUMNS_MINIMUM =
            Metrics.LIBRARY_WIDTH + Metrics.LAYERS_WIDTH + Metrics.MIN_SCENE_WIDTH;

    /**
     * How long to wait before asking the server to compose.
     *
     * <p>One composition is one round trip, so a burst of clicks would be a burst of
     * requests. Waiting for the picking to settle collapses them into one.
     */
    private static final long COMPOSE_DEBOUNCE_MS = 300;

    /**
     * How long to wait before asking the server what a search matches.
     *
     * <p>Longer than the composition's: a query is typed letter by letter, and every
     * letter would otherwise be a request whose answer is thrown away by the next one.
     * The panel filters what it already holds in the meantime, so the field never
     * feels stalled.
     */
    private static final long SEARCH_DEBOUNCE_MS = 400;

    /** How many matches to ask for: the whole library rather than a first page. */
    private static final int SEARCH_PAGE_SIZE = 500;

    /** The magnification of an exported front view: 16 by 32 texels, eight times. */
    private static final int FRONT_VIEW_SCALE = 8;

    /**
     * The parsed catalogue, kept for the session: it is the same library on every
     * opening, and re-reading it would be a request for nothing. Only the JSON is kept
     * here — the atlases are pixels, and pixels belong to the screen that uploaded them.
     */
    private static Catalog catalog = Catalog.EMPTY;

    private final Screen parent;
    private final SkinProject project = new SkinProject();
    private final History history = new History(this.project);
    private final Toasts toasts = new Toasts();
    private final PreviewSkin preview = new PreviewSkin();
    /** One sheet of thumbnails per category, as its atlas arrives. */
    private final Map<String, CategorySprites> sprites = new HashMap<>();
    private final Set<String> requestedAtlases = new HashSet<>();
    /** The front views of the saved skins, which the server composed as it stored them. */
    private final SkinThumbnails skinThumbnails = new SkinThumbnails();
    /** Provenance sheets already fetched, by {@code category/element}. */
    private final Map<String, ItemCredit> credits = new HashMap<>();

    private TopBar topBar;
    private LibraryPanel library;
    private ScenePanel scene;
    private LayersPanel layers;

    private ModalWindow window;
    private Element focused;
    private Element pressed;
    private Drawer drawer = Drawer.NONE;
    private PixelButton libraryTab;
    private PixelButton layersTab;

    private Component hoveredLabel;
    private Component failure = Component.empty();
    private boolean loadingCatalog;
    private boolean closed;

    /** Which element the provenance sheet is about, and what is known of it so far. */
    private ItemTile creditTile;
    private boolean loadingCredit;

    /** The library of saved skins, as the window showing it reads it. */
    private SkinsWindow.Library skins = SkinsWindow.Library.LOADING;

    /** The query waiting to go to the server, and since when. */
    private String query = "";
    private boolean searchPending;
    private long searchSince;
    /** Rises with every search, so a slow answer cannot replace a newer one. */
    private int searchGeneration;

    /** The stack waiting to be composed, and since when. */
    private boolean composePending;
    private long pendingSince;
    /** Rises with every request, so a slow reply cannot overwrite a newer one. */
    private int composeGeneration;
    /** The last sheet the server sent back, which is what an export writes out. */
    private byte[] composed;
    /**
     * The project revision the preview is showing.
     *
     * <p>The display is never written to directly: the project changes, its revision
     * moves, and {@link #tick()} notices. A mutation that forgot to bump it would leave
     * the model a step behind with no error anywhere — this is the one place that can
     * happen, instead of every caller having to remember to say so.
     */
    private int shownRevision;
    /**
     * The cooldown the apply button is currently showing.
     *
     * <p>That button's label is built when the window is laid out, so a wait counting
     * down is a window that has to be laid out again on every second it loses.
     */
    private long shownLockSeconds;

    /** Which side column is open, when the width is too small to show both. */
    private enum Drawer {
        NONE, LIBRARY, LAYERS
    }

    public SkinCreatorScreen(Screen parent) {
        super(Component.translatable("screen.mcskincreator.title"));
        this.parent = parent;
        this.preview.model(this.project.model());
    }

    @Override
    protected void init() {
        Tiles.ensureRegistered(this.minecraft);
        Icons.ensureRegistered(this.minecraft);

        if (this.library == null) {
            this.topBar = new TopBar(this.history, this::startOver, this::openExport,
                    this::openSkins, this::openAbout);
            this.library = new LibraryPanel(this::relayout, this::name, this.sprites::get,
                    this.project::isSlim, this::isUsed, this::stack, this::openProvenance,
                    this::previewItem, this::openFooterLink, this::onCategoriesChanged);
            this.library.createSearch(this::queueSearch);
            this.library.setEmptyMessage(this::libraryMessage);
            this.scene = new ScenePanel(this.preview, this::relayout, () -> this.hoveredLabel);
            this.layers = new LayersPanel(this.project, () -> catalog, this.sprites::get,
                    this.history, this::relayout, this::revealLibrary, this::openImport,
                    this::peekLayer);
        }
        relayout();

        // init() runs again on every resize, so the catalogue is handed over once:
        // handing it over again would put the player back on the first region.
        if (this.library.hasCatalog()) {
            return;
        }
        if (!catalog.isEmpty()) {
            this.library.setCatalog(catalog);
        } else {
            loadCatalog();
        }
    }

    /** An entry's label in the player's language, which the catalogue carries itself. */
    private Component name(CatalogText text) {
        return Component.literal(text.forLanguage(this.minecraft.options.languageCode));
    }

    // ------------------------------------------------------------------ layout

    /**
     * Works out where everything goes. Redone on resize, on a fold, and whenever the
     * contents change.
     *
     * <p>The canvas it measures with carries no graphics object: laying out only ever
     * asks the font how wide a label is. Anything that tried to draw through it would
     * fail loudly rather than quietly, which is the right way round.
     */
    private void relayout() {
        Canvas canvas = new Canvas(null, this.font);

        this.topBar.setBounds(0, 0, this.width, Metrics.TOP_BAR_HEIGHT);
        this.topBar.layout(canvas);

        int top = Metrics.TOP_BAR_HEIGHT;
        if (this.width >= COLUMNS_MINIMUM) {
            this.drawer = Drawer.NONE;
            this.libraryTab = null;
            this.layersTab = null;
            this.library.setVisible(true);
            this.layers.setVisible(true);

            int libraryWidth = this.library.folded() ? Metrics.COLLAPSED_WIDTH : Metrics.LIBRARY_WIDTH;
            int layersWidth = this.layers.folded() ? Metrics.COLLAPSED_WIDTH : Metrics.LAYERS_WIDTH;
            this.library.setBounds(0, top, libraryWidth, this.height - top);
            this.layers.setBounds(this.width - layersWidth, top, layersWidth, this.height - top);
            this.scene.setBounds(libraryWidth, top,
                    this.width - libraryWidth - layersWidth, this.height - top);
        } else {
            layoutDrawers(canvas, top);
        }

        this.library.layout(canvas);
        this.layers.layout(canvas);
        this.scene.layout(canvas);

        // The window is laid out here too, and re-laid out after a scroll, because its
        // body is positioned in screen coordinates: its children have to move with it.
        if (this.window != null) {
            this.window.layout(canvas, this.width, this.height, this::closeWindow);
        }
    }

    /**
     * The narrow arrangement: a tab bar at the bottom, and the open column as a drawer.
     *
     * <p>Portrait puts the drawer under the scene, landscape beside it, and in both the
     * scene only shrinks. Two arrangements that hid it were tried on the site and
     * dropped: stacking all three left the preview 21 pixels, and swapping them through
     * one slot made the model disappear the moment the library opened.
     */
    private void layoutDrawers(Canvas canvas, int top) {
        int tabHeight = Metrics.BUTTON_HEIGHT_COMPACT + Metrics.PAD_TIGHT * 2;
        int usableBottom = this.height - tabHeight;

        this.libraryTab = new PixelButton(Component.translatable("panel.mcskincreator.library"),
                PixelButton.Style.NORMAL, () -> toggleDrawer(Drawer.LIBRARY));
        this.layersTab = new PixelButton(Component.translatable("panel.mcskincreator.layers"),
                PixelButton.Style.NORMAL, () -> toggleDrawer(Drawer.LAYERS));
        this.libraryTab.fit(canvas).setActive(this.drawer == Drawer.LIBRARY);
        this.layersTab.fit(canvas).setActive(this.drawer == Drawer.LAYERS);

        int tabsWidth = this.libraryTab.width() + this.layersTab.width() + Metrics.SEGMENT_GAP;
        int tabsX = (this.width - tabsWidth) / 2;
        int tabsY = usableBottom + Metrics.PAD_TIGHT;
        this.libraryTab.setBounds(tabsX, tabsY, this.libraryTab.width(), Metrics.BUTTON_HEIGHT_COMPACT);
        this.layersTab.setBounds(tabsX + this.libraryTab.width() + Metrics.SEGMENT_GAP, tabsY,
                this.layersTab.width(), Metrics.BUTTON_HEIGHT_COMPACT);

        this.library.setFolded(false);
        this.layers.setFolded(false);
        this.library.setVisible(this.drawer == Drawer.LIBRARY);
        this.layers.setVisible(this.drawer == Drawer.LAYERS);

        if (this.drawer == Drawer.NONE) {
            this.scene.setBounds(0, top, this.width, usableBottom - top);
            this.library.setBounds(0, usableBottom, 0, 0);
            this.layers.setBounds(0, usableBottom, 0, 0);
            return;
        }

        Element open = this.drawer == Drawer.LIBRARY ? this.library : this.layers;
        if (this.height >= this.width) {
            int drawerHeight = (usableBottom - top) / 2;
            this.scene.setBounds(0, top, this.width, usableBottom - top - drawerHeight);
            open.setBounds(0, usableBottom - drawerHeight, this.width, drawerHeight);
        } else {
            int drawerWidth = Math.min(Metrics.LIBRARY_WIDTH, this.width / 2);
            open.setBounds(0, top, drawerWidth, usableBottom - top);
            this.scene.setBounds(drawerWidth, top, this.width - drawerWidth, usableBottom - top);
        }
    }

    private void toggleDrawer(Drawer requested) {
        this.drawer = this.drawer == requested ? Drawer.NONE : requested;
        relayout();
    }

    /** The "+" of the layers panel: bring the library forward. */
    private void revealLibrary() {
        this.library.setFolded(false);
        if (this.width < COLUMNS_MINIMUM) {
            this.drawer = Drawer.LIBRARY;
        }
        relayout();
    }

    // ------------------------------------------------------------------ the stack

    private boolean isUsed(CatalogItem item) {
        return this.project.layers().stream().anyMatch(layer -> layer.itemId().equals(item.id()));
    }

    private void stack(ItemTile tile) {
        this.history.record();
        if (this.project.add(tile.category(), tile.item(), this.minecraft.options.languageCode) == null) {
            this.toasts.failed("stack", Component.translatable("toast.mcskincreator.full"));
            return;
        }
        this.toasts.succeeded("stack");
        relayout();
    }

    /**
     * Shows an element on the model without stacking it, and takes it straight back off
     * on the way out.
     *
     * <p>Nothing is written to the project, so cancelling costs nothing and can never
     * leave a stray layer behind.
     */
    private void previewItem(ItemTile tile) {
        if (tile == null) {
            this.hoveredLabel = null;
            showTopLocally();
            return;
        }
        this.hoveredLabel = tile.label();
        show(this.sprites.get(tile.category().id()), tile.item().atlasIndex(this.project.isSlim()));
    }

    /** Pointing at a layer shows it on the model, so you can tell which one it is. */
    private void peekLayer(Layer layer) {
        if (layer == null) {
            this.hoveredLabel = null;
            showTopLocally();
            return;
        }
        this.hoveredLabel = layer.name();
        show(this.sprites.get(layer.categoryId()), layer.atlasIndex(this.project.isSlim()));
    }

    /**
     * Puts the topmost visible layer on the model from the atlas buffer already in
     * memory — it is a 64x64 skin in its own right — until the server's composition of
     * the whole stack lands.
     *
     * <p>Picking therefore never waits on the network.
     */
    private void showTopLocally() {
        if (this.composed != null) {
            show(this.composed);
            return;
        }
        Layer top = this.project.topVisible();
        if (top != null) {
            show(this.sprites.get(top.categoryId()), top.atlasIndex(this.project.isSlim()));
        }
    }

    private void show(CategorySprites sprites, int index) {
        byte[] buffer = sprites == null ? null : sprites.buffer(index);
        if (buffer != null) {
            show(buffer);
        }
    }

    /** Uploads previewed pixels, reporting a failure the player can act on. */
    private void show(byte[] payload) {
        try {
            this.preview.show(payload);
        } catch (IOException | RuntimeException cause) {
            MCSkinCreatorClient.LOGGER.warn("Unreadable skin texture of {} bytes",
                    payload.length, cause);
            this.toasts.failed("preview",
                    Component.translatable("library.mcskincreator.thumbnails_failed"));
        }
    }

    /** Puts the stack back in the queue, restarting the wait. */
    private void queueCompose() {
        this.composePending = true;
        this.pendingSince = System.currentTimeMillis();
    }

    @Override
    public void tick() {
        super.tick();

        if (this.shownRevision != this.project.revision()) {
            this.shownRevision = this.project.revision();
            this.preview.model(this.project.model());
            // The composition on hand belongs to the stack as it was, so it goes, and
            // the top layer stands in until the server answers for the new one.
            this.composed = null;
            if (this.project.isEmpty()) {
                this.preview.clear();
            } else {
                showTopLocally();
            }
            queueCompose();
        }

        long now = System.currentTimeMillis();
        if (this.composePending && now - this.pendingSince >= COMPOSE_DEBOUNCE_MS) {
            this.composePending = false;
            compose();
        }
        if (this.searchPending && now - this.searchSince >= SEARCH_DEBOUNCE_MS) {
            this.searchPending = false;
            search();
        }

        long lockSeconds = AccountSkin.secondsLeft();
        if (lockSeconds != this.shownLockSeconds) {
            this.shownLockSeconds = lockSeconds;
            if (this.window != null) {
                relayout();
            }
        }
    }

    private void compose() {
        if (this.project.isEmpty()) {
            this.composed = null;
            return;
        }
        int generation = ++this.composeGeneration;
        String body = ProjectJson.project(this.project);

        McscApi.shared().compose(body).whenComplete((texture, failure) ->
                Minecraft.getInstance().execute(() -> {
                    if (this.closed || generation != this.composeGeneration) {
                        return;
                    }
                    if (failure != null) {
                        // The stack is already on the model, so this is a note rather
                        // than a dead end: the preview is the top layer on its own.
                        MCSkinCreatorClient.LOGGER.warn("Composing the project failed", failure);
                        this.toasts.failed("compose",
                                Component.translatable("preview.mcskincreator.compose_failed"));
                        return;
                    }
                    this.toasts.succeeded("compose");
                    this.composed = texture;
                    show(texture);
                }));
    }

    // ------------------------------------------------------------------ the catalogue

    private void loadCatalog() {
        if (this.loadingCatalog) {
            return;
        }
        this.loadingCatalog = true;

        McscApi.shared().catalog().whenComplete((loaded, failure) ->
                Minecraft.getInstance().execute(() -> {
                    this.loadingCatalog = false;
                    if (this.closed) {
                        return;
                    }
                    if (failure != null) {
                        // A player gets a sentence, the log gets the cause. A stack
                        // trace on screen tells them nothing they can act on.
                        MCSkinCreatorClient.LOGGER.warn("Reading the catalogue from {} failed",
                                McscApi.shared().baseUrl(), failure);
                        this.failure = catalogFailure(failure);
                        return;
                    }
                    catalog = loaded;
                    this.library.setCatalog(loaded);
                }));
    }

    /**
     * Asks for the atlases of the categories on screen, and only those.
     *
     * <p>The library runs to far more pixels than one region shows, so the whole of it
     * is never fetched: opening a region fetches that region, which is what the site
     * does too.
     */
    private void onCategoriesChanged() {
        for (CatalogCategory category : this.library.visibleCategories()) {
            if (this.requestedAtlases.add(category.id())) {
                requestAtlas(category);
            }
        }
    }

    private void requestAtlas(CatalogCategory category) {
        McscApi.shared().atlas(category).whenComplete((buffers, failure) ->
                Minecraft.getInstance().execute(() -> {
                    if (this.closed) {
                        return;
                    }
                    if (failure != null) {
                        // A stale content hash answers 404, and the answer to that is a
                        // fresh catalogue rather than a message.
                        MCSkinCreatorClient.LOGGER.warn("Reading the atlas of {} failed",
                                category.id(), failure);
                        this.toasts.failed("atlas:" + category.id(), Component.translatable(
                                notFound(failure)
                                        ? "library.mcskincreator.thumbnails_stale"
                                        : "library.mcskincreator.thumbnails_failed"));
                        return;
                    }
                    this.toasts.succeeded("atlas:" + category.id());
                    this.sprites.put(category.id(), CategorySprites.of(category.id(), buffers));
                }));
    }

    // ------------------------------------------------------------------ searching

    /** Puts a query in the queue, restarting the wait. */
    private void queueSearch(String query) {
        this.query = query.trim();
        this.searchPending = !this.query.isEmpty();
        this.searchSince = System.currentTimeMillis();
    }

    /**
     * Asks the server what the query matches.
     *
     * <p>The panel can filter the catalogue it holds, and does while this is in flight,
     * but only on the labels the player's language shows. The server looks in the three
     * the catalogue carries at once, which is the whole reason this is a request: a
     * French name is unreachable from an English game otherwise.
     */
    private void search() {
        String asked = this.query;
        if (asked.isEmpty()) {
            return;
        }
        int generation = ++this.searchGeneration;

        McscApi.shared().search(asked, 0, SEARCH_PAGE_SIZE).whenComplete((results, failure) ->
                Minecraft.getInstance().execute(() -> {
                    if (this.closed || generation != this.searchGeneration) {
                        return;
                    }
                    if (failure != null) {
                        // No message: the panel is already showing what it could match on
                        // its own, and a red banner over a list that is not empty says
                        // the wrong thing.
                        MCSkinCreatorClient.LOGGER.warn("Searching for \"{}\" failed", asked, failure);
                        return;
                    }
                    this.library.setSearchResults(asked, results.hits());
                    relayout();
                }));
    }

    // ------------------------------------------------------------------ provenance

    /** The provenance sheet of an element, opened on what is known and filled in after. */
    private void openProvenance(ItemTile tile) {
        this.creditTile = tile;
        this.loadingCredit = !this.credits.containsKey(creditKey(tile));
        if (this.loadingCredit) {
            requestCredit(tile);
        }
        open(new TextWindow("window.mcskincreator.provenance", this::provenanceLines, this.window));
    }

    private static String creditKey(ItemTile tile) {
        return tile.category().id() + "/" + tile.item().id();
    }

    /**
     * Fetches what the catalogue cannot say: which starter models this element is a
     * piece of.
     *
     * <p>The work itself is already in the catalogue, which is why the sheet is readable
     * before this lands and why a failure here costs a line rather than the window.
     */
    private void requestCredit(ItemTile tile) {
        String key = creditKey(tile);
        McscApi.shared().credit(tile.category().id(), tile.item().id()).whenComplete((credit, failure) ->
                Minecraft.getInstance().execute(() -> {
                    if (this.closed) {
                        return;
                    }
                    if (failure != null) {
                        MCSkinCreatorClient.LOGGER.warn("Reading the provenance of {} failed", key, failure);
                    } else {
                        this.credits.put(key, credit);
                    }
                    if (this.creditTile != null && creditKey(this.creditTile).equals(key)) {
                        this.loadingCredit = false;
                        relayout();
                    }
                }));
    }

    /**
     * The provenance sheet.
     *
     * <p>Read again on every frame, so the models arrive in a window that is already
     * open. An element that names no work is not a fault and does not read as one: the
     * sheet says the provenance is not documented, which is an open question rather
     * than a silence.
     */
    private List<TextWindow.Line> provenanceLines() {
        ItemTile tile = this.creditTile;
        if (tile == null) {
            return List.of();
        }

        List<TextWindow.Line> lines = new ArrayList<>();
        lines.add(new TextWindow.Line(tile.label(), false));
        lines.add(new TextWindow.Line(Component.translatable("provenance.mcskincreator.category",
                name(tile.category().name())), false));

        ItemCredit credit = this.credits.get(creditKey(tile));
        CatalogWork work = credit != null && credit.hasWork()
                ? credit.work()
                : catalog.workOf(tile.item());
        if (work.isEmpty()) {
            lines.add(TextWindow.Line.warning("provenance.mcskincreator.undocumented"));
        } else {
            lines.add(new TextWindow.Line(Component.translatable("provenance.mcskincreator.work",
                    work.title().isBlank() ? tile.label().getString() : work.title(),
                    work.author()), false));
            lines.add(new TextWindow.Line(Component.translatable("provenance.mcskincreator.licence",
                    licenceName(work)), false));
            if (work.hasUrl()) {
                lines.add(new TextWindow.Line(Component.literal(work.url()), false));
            }
        }

        if (this.loadingCredit) {
            lines.add(TextWindow.Line.of("provenance.mcskincreator.loading"));
        } else if (credit != null) {
            lines.add(modelsLine(credit));
        }
        return lines;
    }

    /** Which starter models this element is a piece of, which says what to expect of it. */
    private TextWindow.Line modelsLine(ItemCredit credit) {
        if (credit.total() == 0) {
            return TextWindow.Line.of("provenance.mcskincreator.no_models");
        }
        String named = credit.models().stream()
                .map(model -> model.name().forLanguage(this.minecraft.options.languageCode))
                .collect(Collectors.joining(", "));
        int unnamed = credit.unnamedModels();
        Component text = unnamed == 0
                ? Component.translatable("provenance.mcskincreator.models", credit.total(), named)
                : Component.translatable("provenance.mcskincreator.models_more", credit.total(), named, unnamed);
        return new TextWindow.Line(text, false);
    }

    /**
     * A licence, named rather than spelled out.
     *
     * <p>The catalogue publishes a key ({@code cc-by}, {@code maison}…), which the site
     * translates. The mod translates the ones it knows and shows the key itself for one
     * it has never heard of, the same way an unknown region reads as its own name — a
     * licence added to the catalogue appears as something rather than as nothing.
     */
    private static Component licenceName(CatalogWork work) {
        if (work.licence().isBlank()) {
            return Component.translatable("licence.mcskincreator.unknown");
        }
        String key = "licence.mcskincreator." + work.licence();
        Component translated = Component.translatable(key);
        return translated.getString().equals(key) ? Component.literal(work.licence()) : translated;
    }

    /** What the library says when it has nothing to show, which is not always the same. */
    private Component libraryMessage() {
        if (!this.failure.getString().isEmpty()) {
            return this.failure;
        }
        return Component.translatable(this.loadingCatalog
                ? "library.mcskincreator.loading"
                : "library.mcskincreator.empty");
    }

    private static boolean notFound(Throwable failure) {
        ApiException refusal = refusal(failure);
        return refusal != null && refusal.isNotFound();
    }

    /** The server's own refusal inside a failure, or null if it never answered. */
    private static ApiException refusal(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof ApiException api) {
                return api;
            }
        }
        return null;
    }

    /**
     * What to tell the player when the catalogue does not arrive.
     *
     * <p>The three cases call for different things from whoever reads them: a host that
     * never answered is a network or an address problem, a status is the server
     * declining, and a body that is not a catalogue means the address reached something
     * else entirely. Reporting all three as "unreachable" sends the reader looking at
     * their connection when the address is what is wrong.
     */
    private static Component catalogFailure(Throwable failure) {
        String address = McscApi.shared().baseUrl();
        ApiException refusal = refusal(failure);
        if (refusal != null) {
            return Component.translatable("library.mcskincreator.http_error", refusal.status(), address);
        }
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof CatalogFormatException) {
                return Component.translatable("library.mcskincreator.not_a_catalog", address);
            }
        }
        return Component.translatable("library.mcskincreator.unreachable", address);
    }

    // ------------------------------------------------------------------ windows

    private void open(ModalWindow opened) {
        this.window = opened;
        this.focused = null;
        if (opened != null) {
            // A window reopened starts at the top of its scroll, never where it was left.
            opened.reset();
        }
        relayout();
        if (opened != null) {
            // After the layout, because the control it wants is one of the children the
            // layout just made.
            this.focused = opened.initialFocus();
        }
    }

    private void closeWindow() {
        ModalWindow previous = this.window;
        open(previous == null ? null : previous.returnsTo());
    }

    private void startOver() {
        this.history.record();
        this.project.clear();
        this.project.setModel(PlayerModelType.WIDE);
        relayout();
    }

    private void openAbout() {
        open(new TextWindow("window.mcskincreator.about", List.of(
                TextWindow.Line.of("about.mcskincreator.what"),
                TextWindow.Line.of("about.mcskincreator.catalog"),
                TextWindow.Line.warning("about.mcskincreator.local_only")), null));
    }

    private void openFooterLink(String link) {
        switch (link) {
            case "about" -> openAbout();
            case "legal" -> open(new TextWindow("window.mcskincreator.legal",
                    List.of(TextWindow.Line.of("legal.mcskincreator.body")), null));
            case "credits" -> open(new TextWindow("window.mcskincreator.credits",
                    this::creditsLines, null));
            default -> open(new TextWindow("window.mcskincreator.beta",
                    List.of(TextWindow.Line.of("beta.mcskincreator.body")), null));
        }
    }

    /**
     * The credits of the skin on the model: one entry per work, in the order the works
     * first appear, each naming the elements that come from it.
     *
     * <p>Built from the catalogue rather than asked for, because the catalogue already
     * carries the table of works and which one every element points at — and because
     * the route that would group them for a whole stack is not part of the {@code v1}
     * contract, so a mod must not lean on it.
     *
     * <p>Elements the repository drew itself credit nobody outside it and are left out,
     * which is what the site does with them too.
     */
    private List<TextWindow.Line> creditsLines() {
        List<TextWindow.Line> lines = new ArrayList<>();
        lines.add(TextWindow.Line.of("credits.mcskincreator.body"));

        Map<CatalogWork, List<String>> used = new LinkedHashMap<>();
        for (Layer layer : this.project.layers()) {
            CatalogItem item = itemOf(layer).orElse(null);
            if (item == null || CatalogWork.IN_HOUSE.equals(item.credit())) {
                continue;
            }
            CatalogWork work = catalog.workOf(item);
            if (work.isEmpty()) {
                continue;
            }
            List<String> elements = used.computeIfAbsent(work, key -> new ArrayList<>());
            String element = layer.name().getString();
            if (!elements.contains(element)) {
                elements.add(element);
            }
        }

        if (used.isEmpty()) {
            lines.add(TextWindow.Line.of(this.project.isEmpty()
                    ? "credits.mcskincreator.nothing_stacked"
                    : "credits.mcskincreator.nothing_to_credit"));
            return lines;
        }
        for (Map.Entry<CatalogWork, List<String>> entry : used.entrySet()) {
            CatalogWork work = entry.getKey();
            lines.add(new TextWindow.Line(Component.translatable("credits.mcskincreator.work",
                    work.title(), work.author(), licenceName(work),
                    String.join(", ", entry.getValue())), false));
        }
        return lines;
    }

    /** The catalogue entry a layer was stacked from, which it may have outlived. */
    private Optional<CatalogItem> itemOf(Layer layer) {
        return catalog.category(layer.categoryId()).flatMap(category -> category.items().stream()
                .filter(item -> item.id().equals(layer.itemId()))
                .findFirst());
    }

    // ------------------------------------------------------------------ the library

    /** Opens the saved skins, and asks the server for them each time it is opened. */
    private void openSkins() {
        open(new SkinsWindow(() -> this.skins, this.skinThumbnails,
                this::openSavedSkin, this::deleteSavedSkin, this::openSaveName));
        loadSkins();
    }

    private void loadSkins() {
        this.skins = new SkinsWindow.Library(this.skins.skins(), true, Component.empty());
        McscApi.shared().skins().whenComplete((loaded, failure) ->
                Minecraft.getInstance().execute(() -> {
                    if (this.closed) {
                        return;
                    }
                    if (failure != null) {
                        MCSkinCreatorClient.LOGGER.warn("Reading the saved skins from {} failed",
                                McscApi.shared().baseUrl(), failure);
                        this.skins = new SkinsWindow.Library(List.of(), false, skinsFailure(failure));
                        relayout();
                        return;
                    }
                    this.skins = new SkinsWindow.Library(loaded, false, Component.empty());
                    loaded.forEach(this::requestSkinThumbnail);
                    relayout();
                }));
    }

    /**
     * The front view of one saved skin.
     *
     * <p>Asked for once per entry and per screen: the server composed that picture when
     * it stored the skin, so this costs a request and no composition — which is what
     * lets a dozen saved skins be shown without rebuilding a dozen stacks.
     */
    private void requestSkinThumbnail(SavedSkin skin) {
        if (this.skinThumbnails.has(skin.id())) {
            return;
        }
        McscApi.shared().skinThumbnail(skin.id(), SkinThumbnails.SCALE).whenComplete((png, failure) ->
                Minecraft.getInstance().execute(() -> {
                    if (this.closed || failure != null) {
                        if (failure != null) {
                            // A row without its picture is still a row one can open.
                            MCSkinCreatorClient.LOGGER.warn("Reading the picture of {} failed",
                                    skin.id(), failure);
                        }
                        return;
                    }
                    try {
                        this.skinThumbnails.put(skin.id(), png);
                    } catch (IOException | RuntimeException cause) {
                        MCSkinCreatorClient.LOGGER.warn("Unreadable picture for {}", skin.id(), cause);
                    }
                }));
    }

    /**
     * Opens a saved skin: its layers become the stack, and its texture goes on the
     * model at once.
     *
     * <p>The texture is the server's own, composed when the skin was stored, so the
     * model is right before the composition of the reopened stack comes back — the same
     * bargain the library makes when an element is picked.
     */
    private void openSavedSkin(SavedSkin skin) {
        this.history.record();
        int dropped = ProjectJson.read(skin.data(), this.project, catalog,
                this.minecraft.options.languageCode);
        closeWindow();
        relayout();

        if (dropped > 0) {
            // Said rather than hidden: the stack is short of something the catalogue no
            // longer carries, and a skin that opens with a layer missing and no word
            // about it reads as the mod losing it.
            this.toasts.failed("skin_open",
                    Component.translatable("toast.mcskincreator.layers_dropped", dropped));
        } else {
            this.toasts.succeeded("skin_open");
        }

        McscApi.shared().skinTexture(skin.id()).whenComplete((texture, failure) ->
                Minecraft.getInstance().execute(() -> {
                    if (this.closed || failure != null || this.project.isEmpty()) {
                        return;
                    }
                    this.composed = texture;
                    show(texture);
                }));
    }

    private void deleteSavedSkin(SavedSkin skin) {
        McscApi.shared().delete(skin.id()).whenComplete((ignored, failure) ->
                Minecraft.getInstance().execute(() -> {
                    if (this.closed) {
                        return;
                    }
                    if (failure != null) {
                        MCSkinCreatorClient.LOGGER.warn("Deleting {} failed", skin.id(), failure);
                        this.toasts.failed("skins", Component.translatable("toast.mcskincreator.skin_delete_failed"));
                        return;
                    }
                    this.toasts.succeeded("skins");
                    this.skinThumbnails.forget(skin.id());
                    loadSkins();
                }));
    }

    private void openSaveName() {
        // Nothing to undo on cancel: the window's own close already gives the library
        // back, and closing twice would take it away with it.
        open(new NameWindow("window.mcskincreator.save", "skin",
                this::saveSkin, () -> { }, this.window));
    }

    /**
     * Stores the skin on the server under a name.
     *
     * <p>A new entry every time, deliberately: the mod has no notion of an open skin
     * that follows the editing the way the site's does, and silently replacing one
     * would be the one behaviour nobody could undo.
     */
    private void saveSkin(String name) {
        SavedSkin skin = new SavedSkin(SavedSkin.newId(), name, System.currentTimeMillis(),
                JsonParser.parseString(ProjectJson.project(this.project)).getAsJsonObject());

        McscApi.shared().save(skin).whenComplete((written, failure) ->
                Minecraft.getInstance().execute(() -> {
                    if (this.closed) {
                        return;
                    }
                    if (failure != null) {
                        MCSkinCreatorClient.LOGGER.warn("Saving \"{}\" failed", name, failure);
                        this.toasts.failed("skins", Component.translatable("toast.mcskincreator.skin_save_failed"));
                        return;
                    }
                    this.toasts.succeeded("skins");
                    this.toasts.ok(Component.translatable("toast.mcskincreator.skin_saved", written.name()));
                    loadSkins();
                }));
    }

    /** What to tell the player when the library does not arrive, for the same three cases. */
    private static Component skinsFailure(Throwable failure) {
        ApiException refusal = refusal(failure);
        if (refusal != null) {
            return Component.translatable("skins.mcskincreator.http_error", refusal.status());
        }
        return Component.translatable("skins.mcskincreator.unreachable", McscApi.shared().baseUrl());
    }

    private void openImport() {
        open(new TextWindow("window.mcskincreator.import", List.of(
                TextWindow.Line.of("import.mcskincreator.body"),
                TextWindow.Line.warning("import.mcskincreator.not_yet")), null));
    }

    private void openExport() {
        List<CardWindow.Card> cards = new ArrayList<>();
        cards.add(new CardWindow.Card("save", "export.mcskincreator.file",
                "export.mcskincreator.file_detail", this::openExportName));
        cards.add(new CardWindow.Card("skin", "export.mcskincreator.front",
                "export.mcskincreator.front_detail", this::openFrontViewName));

        // Last of the three, and the only one that leaves this machine. Without a
        // signed-in session there is no token, so the card is left out rather than shown
        // refusing — and the note under the cards says why, because a choice that
        // silently disappears is one nobody can ask about.
        boolean canApply = AccountSkin.available(this.minecraft);
        if (canApply) {
            cards.add(new CardWindow.Card("outfit", "export.mcskincreator.account",
                    "export.mcskincreator.account_detail", this::openApply));
        }
        open(new CardWindow("window.mcskincreator.export", cards,
                canApply ? null : Component.translatable("export.mcskincreator.no_session"), null));
    }

    /**
     * What applying to the account costs, said before it is done rather than after.
     *
     * <p>The propagation notice is the one that earns its place: the profile CDN takes
     * its time, so a successful upload looks exactly like a failed one for a minute or
     * more. Someone who has not been told that presses the button again, and again,
     * until Mojang rate-limits them for it.
     */
    private void openApply() {
        open(new ConfirmWindow("window.mcskincreator.apply", List.of(
                TextWindow.Line.of("apply.mcskincreator.what"),
                TextWindow.Line.of("apply.mcskincreator.model"),
                TextWindow.Line.warning("apply.mcskincreator.propagation")),
                this::applyLabel, AccountSkin::ready, this::applyToAccount, null));
    }

    /** The apply button says what it is doing, and what it is waiting for. */
    private Component applyLabel() {
        if (AccountSkin.uploading()) {
            return Component.translatable("apply.mcskincreator.sending");
        }
        long left = AccountSkin.secondsLeft();
        return left > 0
                ? Component.translatable("apply.mcskincreator.wait", left)
                : Component.translatable("apply.mcskincreator.confirm");
    }

    /**
     * Sends the composed sheet to the account. One press, one upload.
     *
     * <p>What goes up is the sheet the server composed — the same bytes an export writes
     * out — so what lands on the account is what was on the model, and never the single
     * layer the preview stands in with while a composition is on its way.
     */
    private void applyToAccount() {
        if (this.composed == null) {
            this.toasts.error(Component.translatable("toast.mcskincreator.nothing_to_apply"));
            return;
        }
        // The button is disabled in both these cases, so getting here means a click beat
        // the layout that would have disabled it. Answering is still cheaper than
        // sending a second upload, and far cheaper than saying nothing.
        if (AccountSkin.uploading()) {
            this.toasts.ok(Component.translatable("toast.mcskincreator.applying"));
            return;
        }
        if (!AccountSkin.ready()) {
            this.toasts.error(Component.translatable("toast.mcskincreator.apply_rate_limited",
                    AccountSkin.secondsLeft()));
            return;
        }

        byte[] sheet = this.composed;
        PlayerModelType model = this.project.model();
        this.toasts.ok(Component.translatable("toast.mcskincreator.applying"));
        AccountSkin.apply(this.minecraft, sheet, model)
                .whenComplete((nothing, failure) -> Minecraft.getInstance().execute(() -> {
                    // Put on before the screen is consulted: the editor may well have
                    // been closed while the upload was in flight, and a skin that
                    // reached the account should go on the player either way.
                    if (failure == null) {
                        wearLocally(sheet, model);
                    }
                    if (this.closed) {
                        return;
                    }
                    if (failure != null) {
                        // The status and what threw, never the request: the log is the
                        // one place the token must not reach.
                        MCSkinCreatorClient.LOGGER.warn("Applying the skin to the account failed",
                                failure);
                        this.toasts.error(applyFailure(failure));
                        return;
                    }
                    this.toasts.ok(Component.translatable("toast.mcskincreator.applied"));
                }));
    }

    /**
     * Wears what was just uploaded, so the player sees it now rather than on the next
     * start of the game.
     *
     * <p>Only ever after Mojang accepted it, so this cannot show a skin that failed to
     * send. A failure here costs the immediacy and nothing else: the account has the
     * skin, and the game will draw it on its own next time it starts.
     */
    private void wearLocally(byte[] sheet, PlayerModelType model) {
        User user = Minecraft.getInstance().getUser();
        if (user == null) {
            return;
        }
        try {
            AppliedSkin.wear(user.getProfileId(), sheet, model);
        } catch (IOException | RuntimeException cause) {
            MCSkinCreatorClient.LOGGER.warn("Wearing the applied skin locally failed", cause);
        }
    }

    /**
     * What to tell the player when the skin did not reach the account.
     *
     * <p>Mojang's three refusals ask different things of whoever reads them: an expired
     * session is fixed by signing in again, a rate limit is fixed by waiting and by
     * nothing else, and a status is worth reporting. A network that never answered is a
     * fourth thing again, and everything left over is the mod's own fault rather than
     * anyone's connection — saying "unreachable" for that would send the reader to look
     * at a router that is working perfectly.
     */
    private static Component applyFailure(Throwable failure) {
        SkinUploadException refusal = AccountSkin.refusal(failure);
        if (refusal != null) {
            return switch (refusal.reason()) {
                case SESSION_EXPIRED -> Component.translatable("toast.mcskincreator.apply_session");
                case RATE_LIMITED -> Component.translatable("toast.mcskincreator.apply_rate_limited",
                        AccountSkin.secondsLeft());
                case REFUSED -> Component.translatable("toast.mcskincreator.apply_refused",
                        refusal.status());
            };
        }
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof IOException) {
                return Component.translatable("toast.mcskincreator.apply_unreachable");
            }
        }
        return Component.translatable("toast.mcskincreator.apply_failed");
    }

    private void openExportName() {
        open(new NameWindow("window.mcskincreator.name", "skin",
                this::exportTo, this::closeWindow, null));
    }

    private void openFrontViewName() {
        open(new NameWindow("window.mcskincreator.name", "skin-front",
                this::exportFrontView, this::closeWindow, null));
    }

    private void exportTo(String name) {
        if (this.composed == null) {
            this.toasts.failed(Export.KIND,
                    Component.translatable("toast.mcskincreator.nothing_to_export"));
            return;
        }
        write(this.composed, name);
    }

    /**
     * Writes out the character seen from the front rather than the sheet.
     *
     * <p>A skin file is for installing and this is for looking at, which is why it is
     * worth its own round trip: {@code POST /thumbnails} draws the whole figure
     * standing, at whole texels, and the mod has nothing that renders one.
     */
    private void exportFrontView(String name) {
        if (this.project.isEmpty()) {
            this.toasts.failed(Export.KIND,
                    Component.translatable("toast.mcskincreator.nothing_to_export"));
            return;
        }

        McscApi.shared().frontView(ProjectJson.project(this.project), FRONT_VIEW_SCALE)
                .whenComplete((png, failure) -> Minecraft.getInstance().execute(() -> {
                    if (this.closed) {
                        return;
                    }
                    if (failure != null) {
                        MCSkinCreatorClient.LOGGER.warn("Drawing the front view failed", failure);
                        this.toasts.failed(Export.KIND,
                                Component.translatable("toast.mcskincreator.export_failed"));
                        return;
                    }
                    write(png, name);
                }));
    }

    private void write(byte[] png, String name) {
        Path written = Export.write(this.minecraft, png, name);
        if (written == null) {
            this.toasts.failed(Export.KIND, Component.translatable("toast.mcskincreator.export_failed"));
            return;
        }
        this.toasts.succeeded(Export.KIND);
        this.toasts.ok(Component.translatable("toast.mcskincreator.exported",
                written.getFileName().toString()));
    }

    // ------------------------------------------------------------------ drawing

    //? if >=26.1 {
    /*@Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        paint(new Canvas(graphics, this.font), mouseX, mouseY, delta);
    }
    *///?} else {
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        paint(new Canvas(graphics, this.font), mouseX, mouseY, delta);
    }
    //?}

    private void paint(Canvas canvas, int mouseX, int mouseY, float delta) {
        Paint paint = new Paint(canvas, mouseX, mouseY, System.currentTimeMillis(), this.focused);

        canvas.fill(0, 0, this.width, this.height, Palette.DARKER);
        this.topBar.draw(paint);
        this.scene.draw(paint, delta);
        if (this.library.visible()) {
            this.library.draw(paint);
        }
        if (this.layers.visible()) {
            this.layers.draw(paint);
        }
        drawTabs(paint);

        // Overlays go over their own panel and take clicks before it, which is what
        // keeps an open menu from being painted over by the strip it belongs to.
        for (Element element : targets()) {
            if (element.overlayActive()) {
                element.drawOverlay(paint);
            }
        }

        if (this.window != null) {
            this.window.draw(paint, this.width, this.height);
        }

        this.toasts.draw(canvas, this.width, this.height, paint.time());
        drawTooltip(paint);
    }

    private void drawTabs(Paint paint) {
        if (this.libraryTab == null) {
            return;
        }
        Canvas canvas = paint.canvas();
        int barTop = this.libraryTab.y() - Metrics.PAD_TIGHT;
        Surface.dark(canvas, 0, barTop, this.width, this.height - barTop, Palette.DARK);
        this.libraryTab.draw(paint);
        this.layersTab.draw(paint);
    }

    private void drawTooltip(Paint paint) {
        for (Element element : targets()) {
            if (element.contains(paint.mouseX(), paint.mouseY()) && !element.tooltip().isEmpty()) {
                paint.canvas().tooltip(element.tooltip(), paint.mouseX(), paint.mouseY());
                return;
            }
        }
    }

    // ------------------------------------------------------------------ input

    /**
     * Everything that can be clicked or focused right now, front to back.
     *
     * <p>A window takes the whole screen while it is open, so nothing under it is in
     * the list at all — not merely covered, but genuinely out of reach.
     */
    private List<Element> targets() {
        List<Element> targets = new ArrayList<>();
        if (this.window != null) {
            targets.addAll(this.window.children());
            return targets;
        }
        targets.addAll(this.topBar.children());
        targets.addAll(this.scene.controls());
        if (this.library.visible()) {
            targets.addAll(this.library.hitTargets());
        }
        if (this.layers.visible()) {
            targets.addAll(this.layers.hitTargets());
        }
        if (this.libraryTab != null) {
            targets.add(this.libraryTab);
            targets.add(this.layersTab);
        }
        targets.add(this.scene);
        return targets;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        double mouseX = event.x();
        double mouseY = event.y();
        List<Element> targets = targets();

        for (Element element : targets) {
            if (!element.overlayActive()) {
                continue;
            }
            if (element instanceof Dropdown<?> dropdown
                    && dropdown.overlayMouseDown(mouseX, mouseY, new Canvas(null, this.font))) {
                relayout();
                return true;
            }
            if (element.overlayContains(mouseX, mouseY)) {
                return true;
            }
        }

        if (this.window != null && this.window.pressOutside(mouseX, mouseY)) {
            return true;
        }

        for (Element element : targets) {
            if (element.mouseDown(mouseX, mouseY, event.button())) {
                this.pressed = element;
                this.focused = element.focusable() ? element : null;
                blurEverythingBut(targets, element);
                return true;
            }
        }

        // A press that landed on nothing still takes the focus off whatever had it.
        this.focused = null;
        blurEverythingBut(targets, null);
        return true;
    }

    private static void blurEverythingBut(List<Element> targets, Element keep) {
        for (Element element : targets) {
            if (element != keep) {
                element.blur();
            }
        }
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.pressed != null) {
            this.pressed.mouseDrag(event.x(), event.y(), dragX, dragY, event.button());
            return true;
        }
        for (Element element : targets()) {
            element.mouseDrag(event.x(), event.y(), dragX, dragY, event.button());
        }
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.window != null && this.window.releaseClosesWindow(event.x(), event.y())) {
            // Only when the gesture started on the backdrop: otherwise selecting text
            // in a field and letting go outside would close the window and lose it.
            closeWindow();
            return true;
        }
        if (this.pressed != null) {
            this.pressed.mouseUp(event.x(), event.y(), event.button());
            this.pressed = null;
            return true;
        }
        for (Element element : targets()) {
            element.mouseUp(event.x(), event.y(), event.button());
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.window != null) {
            if (!this.window.scroll(scrollY)) {
                return false;
            }
            relayout();
            return true;
        }
        if (this.library.visible() && this.library.scroll(mouseX, mouseY, scrollY)) {
            return true;
        }
        return this.layers.visible() && this.layers.scroll(mouseX, mouseY, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        boolean control = (event.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean shift = (event.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0;

        if (typingTarget() != null && typingTarget().keyDown(key, event.modifiers())) {
            return true;
        }
        if (this.focused != null && this.focused.keyDown(key, event.modifiers())) {
            return true;
        }

        if (key == GLFW.GLFW_KEY_ESCAPE) {
            // Escape unwinds one layer at a time: an open menu, then the window, then
            // the screen. Closing the window under an open menu loses whatever was in it.
            for (Element element : targets()) {
                if (element.overlayActive()) {
                    element.closeOverlay();
                    return true;
                }
            }
            if (this.window != null) {
                closeWindow();
                return true;
            }
            onClose();
            return true;
        }

        if (key == GLFW.GLFW_KEY_TAB) {
            moveFocus(shift ? -1 : 1);
            return true;
        }

        if (this.window instanceof NameWindow named
                && (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER)) {
            if (named.accept()) {
                closeWindow();
            }
            return true;
        }

        if (this.focused != null && (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_SPACE)
                && !this.focused.capturesTyping()) {
            return this.focused.activate();
        }

        // No shortcut fires while someone is typing: searching for "band" is not a
        // request for the bucket tool.
        if (typing()) {
            return false;
        }

        if (control && key == GLFW.GLFW_KEY_Z) {
            if (shift ? this.history.redo() : this.history.undo()) {
                relayout();
            }
            return true;
        }
        if (control && key == GLFW.GLFW_KEY_Y) {
            if (this.history.redo()) {
                relayout();
            }
            return true;
        }
        if (control && key == GLFW.GLFW_KEY_S) {
            openExport();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        Element typing = typingTarget();
        if (typing != null && typing.charTyped(event.codepoint())) {
            relayout();
            return true;
        }
        return super.charTyped(event);
    }

    /**
     * Whatever is being typed into, focused or not.
     *
     * <p>A field can hold the cursor without holding the focus ring — a window opens
     * with one ready to type into — so the keystrokes follow the field rather than the
     * ring.
     */
    private Element typingTarget() {
        for (Element element : targets()) {
            if (element.capturesTyping()) {
                return element;
            }
        }
        return null;
    }

    private boolean typing() {
        return typingTarget() != null;
    }

    /**
     * Moves the focus ring.
     *
     * <p>Hovering exists with a mouse and not with a controller, so everything the site
     * reveals on hover has to be reachable this way: what has the focus draws as what
     * is hovered.
     */
    private void moveFocus(int direction) {
        List<Element> ring = targets().stream().filter(Element::focusable).toList();
        if (ring.isEmpty()) {
            this.focused = null;
            return;
        }
        int index = ring.indexOf(this.focused);
        this.focused = ring.get(Math.floorMod(index + direction, ring.size()));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        super.removed();
        this.closed = true;
        this.preview.close();
        for (CategorySprites loaded : this.sprites.values()) {
            loaded.close();
        }
        this.sprites.clear();
        this.skinThumbnails.close();
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            ScreenCompat.setScreen(this.minecraft, this.parent);
        }
    }
}
