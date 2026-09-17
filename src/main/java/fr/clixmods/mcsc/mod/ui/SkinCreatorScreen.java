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
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.clixmods.mcsc.mod.MCSkinCreatorClient;
import fr.clixmods.mcsc.mod.catalog.Catalog;
import fr.clixmods.mcsc.mod.catalog.CatalogCategory;
import fr.clixmods.mcsc.mod.catalog.CatalogFormatException;
import fr.clixmods.mcsc.mod.catalog.CatalogItem;
import fr.clixmods.mcsc.mod.catalog.CatalogText;
import fr.clixmods.mcsc.mod.project.History;
import fr.clixmods.mcsc.mod.project.Layer;
import fr.clixmods.mcsc.mod.project.SkinProject;
import fr.clixmods.mcsc.mod.remote.ApiException;
import fr.clixmods.mcsc.mod.remote.McscApi;
import fr.clixmods.mcsc.mod.skin.CategorySprites;
import fr.clixmods.mcsc.mod.skin.PreviewSkin;
import fr.clixmods.mcsc.mod.skin.ProjectJson;
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
import fr.clixmods.mcsc.mod.ui.window.ModalWindow;
import fr.clixmods.mcsc.mod.ui.window.NameWindow;
import fr.clixmods.mcsc.mod.ui.window.TextWindow;
import net.minecraft.client.Minecraft;
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
            this.topBar = new TopBar(this.history, this::startOver, this::openExport, this::openAbout);
            this.library = new LibraryPanel(this::relayout, this::name, this.sprites::get,
                    this.project::isSlim, this::isUsed, this::stack, this::openProvenance,
                    this::previewItem, this::openFooterLink, this::onCategoriesChanged);
            this.library.createSearch();
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

        if (this.composePending && System.currentTimeMillis() - this.pendingSince >= COMPOSE_DEBOUNCE_MS) {
            this.composePending = false;
            compose();
        }
    }

    private void compose() {
        if (this.project.isEmpty()) {
            this.composed = null;
            return;
        }
        int generation = ++this.composeGeneration;
        String body = ProjectJson.stack(this.project);

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
                    List.of(TextWindow.Line.of("credits.mcskincreator.body")), null));
            default -> open(new TextWindow("window.mcskincreator.beta",
                    List.of(TextWindow.Line.of("beta.mcskincreator.body")), null));
        }
    }

    private void openProvenance(ItemTile tile) {
        open(new TextWindow("window.mcskincreator.provenance", List.of(
                new TextWindow.Line(tile.label(), false),
                new TextWindow.Line(Component.translatable("provenance.mcskincreator.category",
                        name(tile.category().name())), false),
                TextWindow.Line.of("provenance.mcskincreator.catalog")), this.window));
    }

    private void openImport() {
        open(new TextWindow("window.mcskincreator.import", List.of(
                TextWindow.Line.of("import.mcskincreator.body"),
                TextWindow.Line.warning("import.mcskincreator.not_yet")), null));
    }

    private void openExport() {
        open(new CardWindow("window.mcskincreator.export", List.of(
                new CardWindow.Card("save", "export.mcskincreator.file",
                        "export.mcskincreator.file_detail", this::openExportName)), null));
    }

    private void openExportName() {
        open(new NameWindow("window.mcskincreator.name", "skin",
                this::exportTo, this::closeWindow, null));
    }

    private void exportTo(String name) {
        if (this.composed == null) {
            this.toasts.failed(Export.KIND,
                    Component.translatable("toast.mcskincreator.nothing_to_export"));
            return;
        }
        Path written = Export.write(this.minecraft, this.composed, name);
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
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            ScreenCompat.setScreen(this.minecraft, this.parent);
        }
    }
}
