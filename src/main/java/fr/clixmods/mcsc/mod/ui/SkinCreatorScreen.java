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
import fr.clixmods.mcsc.mod.catalog.CatalogModel;
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
import fr.clixmods.mcsc.mod.scene.GameCamera;
import fr.clixmods.mcsc.mod.skin.AppliedSkin;
import fr.clixmods.mcsc.mod.skin.CategorySprites;
import fr.clixmods.mcsc.mod.skin.Highlight;
import fr.clixmods.mcsc.mod.skin.ReadyMadeSkins;
import fr.clixmods.mcsc.mod.skin.PreviewSkin;
import fr.clixmods.mcsc.mod.skin.ProjectJson;
import fr.clixmods.mcsc.mod.skin.SkinThumbnails;
import fr.clixmods.mcsc.mod.style.Tiles;
import fr.clixmods.mcsc.mod.ui.panel.LayersPanel;
import fr.clixmods.mcsc.mod.ui.panel.LibraryPanel;
import fr.clixmods.mcsc.mod.ui.panel.Panel;
import fr.clixmods.mcsc.mod.ui.panel.ScenePanel;
import fr.clixmods.mcsc.mod.ui.panel.TopBar;
import fr.clixmods.mcsc.mod.ui.widget.Dropdown;
import fr.clixmods.mcsc.mod.ui.widget.ItemTile;
import fr.clixmods.mcsc.mod.ui.window.CardWindow;
import fr.clixmods.mcsc.mod.ui.window.ConfirmWindow;
import fr.clixmods.mcsc.mod.ui.window.ModalWindow;
import fr.clixmods.mcsc.mod.ui.window.ModelsWindow;
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
import com.mojang.blaze3d.platform.NativeImage;
import fr.clixmods.mcsc.mod.skin.SkinBlend;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerModelType;
import org.lwjgl.glfw.GLFW;

/**
 * The editor.
 *
 * <p>What the editor <em>is</em>: the project being edited, its history, the catalogue
 * it draws from, the requests that compose and save it, the windows it opens, and the
 * keyboard and mouse that drive all of it. Where its four zones go is
 * {@link EditorChrome}'s, and nothing here works a pixel out.
 *
 * <p>The widgets are the mod's own rather than the game's, because half of them — a
 * category tab, an element thumbnail, a layer row — have no vanilla equivalent to
 * dress. The mechanics the game is right about stay the game's: its font, its GUI
 * scale, the player model, and a focus ring a keyboard can walk, which is also how
 * everything the site reveals on hover stays reachable without a mouse.
 *
 * <p>Nothing on screen waits on the network. Stacking an element shows it straight
 * away from the atlas buffer already in memory, and the server's composition replaces
 * it when it lands; a server that cannot compose costs a notification, not the
 * preview. Every texture this screen builds is released in {@link #removed()}, and a
 * reply arriving after it has gone is dropped rather than uploaded.
 */
public class SkinCreatorScreen extends Screen {

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

    /**
     * The pictures of the starter models, and of the outfits, drawn from the atlases
     * already here.
     *
     * <p>Rebuilt rather than patched: a ready-made stack spans several categories, so
     * one atlas arriving can complete a dozen of them at once, and working out which
     * would cost more than redrawing the sheet. The outfits' sheet is kept in
     * {@link #sprites} beside the categories' own, which is what lets the library draw
     * an outfit with the same tile as everything else.
     */
    private CategorySprites modelSprites;
    /** What the two sheets were drawn from, so they are only redrawn when that changed. */
    private int modelSpritesStamp = -1;
    private int outfitSpritesStamp = -1;
    /** Rises every time a category's pixels land. */
    private int atlasRevision;

    private TopBar topBar;
    private LibraryPanel library;
    private ScenePanel scene;
    private LayersPanel layers;
    /** Where the four zones go. The screen owns what is in them, not where they are. */
    private EditorChrome chrome;
    /** The scale this screen draws at, which is its own rather than the player's. */
    private final EditorScale scale = new EditorScale();

    /**
     * The pulse that says where the layer under the pointer sits on the figure.
     *
     * <p>It flashes over the composed sheet, so it needs one: while the server has not
     * answered for the current stack there is nothing to flash over, and pointing at a
     * row names the layer in the corner without lighting it up. That lasts as long as
     * the debounce and no longer.
     */
    private final Highlight highlight = new Highlight();

    /** Whether the sheet on the model is currently a flashed one, and so owes a reset. */
    private boolean highlighting;

    /**
     * Whether a library element is being shown on the model instead.
     *
     * <p>The two hovers are one pointer and cannot both be wanted, but they can overlap
     * in time: leaving a layer row starts a fade, and the pointer may be on a thumbnail
     * before it ends. Without this the dying flash would paint over the element for the
     * length of the fade and then put the stack back, losing the preview that was asked
     * for.
     */
    private boolean showingItem;

    /** The sheet the character in the world is wearing, so it is not re-uploaded. */
    private byte[] previewWorn;

    private ModalWindow window;
    private Element focused;
    private Element pressed;

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
    /** The composed stack decoded, so a blend does not decode a PNG twenty times a second. */
    private NativeImage baseImage;
    private byte[] baseOf;
    /** The element under the pointer in the library, laid over the stack while it is. */
    private byte[] hovered;
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

    public SkinCreatorScreen(Screen parent) {
        super(Component.translatable("screen.mcskincreator.title"));
        this.parent = parent;
        this.preview.model(this.project.model());
    }

    @Override
    protected void init() {
        // Before anything is measured: taking the scale changes what a pixel is, and
        // every size below is in pixels. A resize makes the game recompute the scale
        // from the options, so this runs on every layout rather than once.
        if (this.scale.apply(this.minecraft)) {
            this.width = this.minecraft.getWindow().getGuiScaledWidth();
            this.height = this.minecraft.getWindow().getGuiScaledHeight();
        }

        Tiles.ensureRegistered(this.minecraft);

        if (this.library == null) {
            this.topBar = new TopBar(this.history,
                    () -> new TopBar.Mark(Logo.texture(this.minecraft), Logo.size()),
                    this::startOver, this::openModels, () -> !catalog.models().isEmpty(),
                    this::openExport, this::openSkins, this::openAbout);
            this.library = new LibraryPanel(this::relayout, this::name, this.sprites::get,
                    this.project::isSlim, this::isUsed, this::stack, this::wear,
                    this::openProvenance, this::previewItem, this::openFooterLink,
                    this::onCategoriesChanged);
            this.library.createSearch(this::queueSearch);
            this.library.setEmptyMessage(this::libraryMessage);
            this.scene = new ScenePanel(this.preview, new PlayerFigure(this.preview),
                    new GameCamera(Minecraft.getInstance()),
                    this::relayout, () -> this.hoveredLabel);
            this.layers = new LayersPanel(this.project, () -> catalog, this.sprites::get,
                    this.history, this::relayout, this::revealLibrary, this::openImport,
                    this::peekLayer);
            this.chrome = new EditorChrome(this.topBar, this.library, this.scene, this.layers,
                    this::relayout);
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
        Canvas canvas = new GameCanvas(null, this.font);
        this.chrome.layout(canvas, this.width, this.height);

        // The window is laid out here too, and re-laid out after a scroll, because its
        // body is positioned in screen coordinates: its children have to move with it.
        if (this.window != null) {
            this.window.layout(canvas, this.width, this.height, this::closeWindow);
        }
    }

    /** The "+" of the layers panel: bring the library forward. */
    private void revealLibrary() {
        this.chrome.revealLibrary();
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
     * Shows an element <em>on</em> the model without stacking it, and takes it straight
     * back off on the way out.
     *
     * <p>It used to replace the previewed skin with the element alone, so pointing at a
     * pair of eyes emptied the scene and showed two eyes floating in it — which answers
     * "what would this look like" by taking away everything it would look like against.
     * The element is laid over the composed skin instead, which is exactly what picking
     * it would do.
     *
     * <p>Nothing is written to the project, so cancelling costs nothing and can never
     * leave a stray layer behind.
     */
    private void previewItem(ItemTile tile) {
        if (tile == null) {
            this.hovered = null;
            this.hoveredLabel = null;
            this.showingItem = false;
            showTopLocally();
            return;
        }
        this.hoveredLabel = tile.label();
        this.showingItem = true;
        // This hover owns the model now, so a layer pulse still fading out is dropped
        // rather than left to paint over the element and then reset the texture.
        this.highlight.drop();
        this.highlighting = false;
        this.hovered = buffer(this.sprites.get(tile.category().id()),
                tile.item().atlasIndex(this.project.isSlim()));
        showHovered();
    }

    /**
     * Pointing at a layer makes it pulse on the figure, so you can tell where it is.
     *
     * <p>It used to put that layer on the model <em>on its own</em>, which answers the
     * wrong question — it shows what the layer is and hides where it is, and a layer
     * something else covers showed up as the whole figure disappearing. The site flashes
     * the layer's texels over the stack instead, and so does this: see {@link Highlight}.
     */
    private void peekLayer(Layer layer) {
        this.hovered = null;
        this.showingItem = false;
        if (layer == null) {
            this.hoveredLabel = null;
            this.highlight.hide(System.currentTimeMillis());
            return;
        }
        this.hoveredLabel = layer.name();
        this.highlight.show(layer, System.currentTimeMillis());
    }

    /**
     * Puts this frame's flash on the preview texture, or takes the last one off.
     *
     * <p>Called once a frame while the pulse is up, because the pulse is a pulse: the
     * sheet is rebuilt and rewritten into the texture already there rather than a new
     * one being registered sixty times a second.
     */
    private void applyHighlight(long now) {
        if (this.showingItem) {
            return;
        }
        Layer layer = this.highlight.layer();
        if (layer != null && !this.project.layers().contains(layer)) {
            // A deleted layer's row will never report the pointer leaving it, so
            // nothing else would ever turn this off.
            this.highlight.drop();
            layer = null;
        }

        if (layer != null && this.composed != null && this.highlight.active(now)) {
            byte[] flashed = this.highlight.over(this.composed, layerBuffer(layer), now);
            if (flashed != this.composed) {
                this.preview.refresh(flashed);
                this.highlighting = true;
                return;
            }
        }
        if (this.highlighting) {
            this.highlighting = false;
            showTopLocally();
        }
    }

    /** The layer's own 64x64, straight out of the atlas its category arrived in. */
    private byte[] layerBuffer(Layer layer) {
        return buffer(this.sprites.get(layer.categoryId()),
                layer.atlasIndex(this.project.isSlim()));
    }

    /** The base every blend is laid on: the composed stack, decoded once and kept. */
    private NativeImage base() {
        if (this.composed == null) {
            this.baseImage = closed(this.baseImage);
            this.baseOf = null;
            return null;
        }
        if (this.baseImage != null && this.baseOf == this.composed) {
            return this.baseImage;
        }
        this.baseImage = closed(this.baseImage);
        try {
            this.baseImage = PreviewSkin.decode(this.composed);
            this.baseOf = this.composed;
        } catch (IOException | RuntimeException cause) {
            MCSkinCreatorClient.LOGGER.warn("Unreadable composed skin", cause);
            this.baseOf = null;
        }
        return this.baseImage;
    }

    private static NativeImage closed(NativeImage image) {
        if (image != null) {
            image.close();
        }
        return null;
    }

    /** The element under the pointer, laid over the stack. */
    private void showHovered() {
        NativeImage base = base();
        if (base == null || this.hovered == null) {
            // Nothing composed yet, so there is nothing to lay it over: the element on
            // its own is the best answer available, and it is the old one.
            if (this.hovered != null) {
                show(this.hovered);
            }
            return;
        }
        this.preview.show(SkinBlend.over(base, this.hovered));
    }

    private static byte[] buffer(CategorySprites sprites, int index) {
        return sprites == null ? null : sprites.buffer(index);
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
        this.scene.tick();
        syncWorldPreview();

        if (this.shownRevision != this.project.revision()) {
            this.shownRevision = this.project.revision();
            this.preview.model(this.project.model());
            // The composition on hand belongs to the stack as it was, so it goes, and
            // the top layer stands in until the server answers for the new one.
            this.composed = null;
            this.hovered = null;
            if (this.project.isEmpty()) {
                this.preview.clear();
            } else {
                showTopLocally();
            }
            queueCompose();
        }

        // Only while what needs them is on screen: a sheet is a few hundred stacks
        // composed, which is not work to do for something nobody is looking at.
        if (this.window instanceof ModelsWindow) {
            refreshModelSprites();
        }
        if (this.library.showingOutfits()) {
            refreshOutfitSprites();
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

    /**
     * Puts the edit on the real character while an in-game camera is looking at them.
     *
     * <p>Only then: the two cameras that show the world are the only reason to dress the
     * character in something that has not been applied. Under the workshop camera there
     * is nothing to see it on, so nothing is overridden and the player in the world —
     * behind the editor, where a passing mob can still see them — stays as they were.
     *
     * <p>Sends only what the server composed, and only when it changes, which is at most
     * once per stack rather than once per frame.
     */
    private void syncWorldPreview() {
        boolean wanted = this.scene.showsWorld() && this.composed != null
                && this.minecraft != null && this.minecraft.player != null;
        if (!wanted) {
            AppliedSkin.stopPreviewing();
            this.previewWorn = null;
            return;
        }
        if (this.previewWorn == this.composed) {
            return;
        }
        try {
            AppliedSkin.preview(this.minecraft.player.getUUID(), this.composed,
                    this.project.model());
            this.previewWorn = this.composed;
        } catch (IOException | RuntimeException cause) {
            // The scene still shows the skin; only the character in the world does not.
            MCSkinCreatorClient.LOGGER.warn("Wearing the edited skin in the world failed",
                    cause);
            this.previewWorn = this.composed;
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
                    this.atlasRevision++;
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

    /**
     * Opens the catalogue's starter models.
     *
     * <p>Their pieces come from all over the library, so this is one of the two
     * gestures that fetch every category rather than the region on screen — there is no
     * way to show a model without the pixels it is made of. Atlas addresses carry the
     * hash of their own pixels and are kept, so it is paid once.
     */
    private void openModels() {
        for (CatalogModel model : catalog.models()) {
            for (CatalogCategory category : catalog.categoriesOf(model)) {
                if (this.requestedAtlases.add(category.id())) {
                    requestAtlas(category);
                }
            }
        }
        refreshModelSprites();
        open(new ModelsWindow(catalog::models, this::name, () -> this.modelSprites,
                this::chooseModel, null));
    }

    /**
     * Starts again from a starter model: the stack is replaced, and the player model
     * with it.
     *
     * <p>One history entry for the whole thing, so a model that stacked eleven elements
     * comes back off with one undo rather than eleven.
     */
    private void chooseModel(CatalogModel model) {
        applyReadyMade(model);
        closeWindow();
    }

    /** Puts an outfit on over whatever is already worn. Also one history entry. */
    private void wear(CatalogModel outfit) {
        applyReadyMade(outfit);
    }

    private void applyReadyMade(CatalogModel entry) {
        this.history.record();
        int stacked = this.project.apply(entry, catalog, this.minecraft.options.languageCode);
        if (stacked == 0) {
            // Every piece named an element the catalogue has since dropped.
            this.toasts.failed("readymade", Component.translatable("toast.mcskincreator.model_empty"));
            return;
        }
        this.toasts.succeeded("readymade");
        this.toasts.ok(Component.translatable(
                entry.kind() == CatalogModel.Kind.MODEL
                        ? "toast.mcskincreator.model_applied"
                        : "toast.mcskincreator.outfit_applied",
                name(entry.name()), stacked));
        relayout();
    }

    /**
     * Redraws a sheet of ready-made pictures when what it was drawn from has moved: a
     * new atlas, or the other player model. Never called while drawing — it ends in a
     * texture upload, and a frame is for deciding what to draw.
     */
    private void refreshModelSprites() {
        if (catalog.models().isEmpty() || this.modelSpritesStamp == stamp()) {
            return;
        }
        if (this.modelSprites != null) {
            this.modelSprites.close();
        }
        // A model carries its own skin, so nothing is stood under it.
        this.modelSprites = CategorySprites.of("ready-made-models", ReadyMadeSkins.of(
                catalog.models(), catalog, this.sprites::get, this.project.isSlim(), null));
        this.modelSpritesStamp = stamp();
    }

    private void refreshOutfitSprites() {
        if (catalog.outfits().isEmpty() || this.outfitSpritesStamp == stamp()) {
            return;
        }
        CategorySprites previous = this.sprites.remove(LibraryPanel.OUTFIT_SHEET);
        if (previous != null) {
            previous.close();
        }
        // An outfit is clothes: without a body under them its picture is empty sleeves.
        byte[] body = ReadyMadeSkins.mannequin(catalog, this.sprites::get, this.project.isSlim());
        this.sprites.put(LibraryPanel.OUTFIT_SHEET, CategorySprites.of(LibraryPanel.OUTFIT_SHEET,
                ReadyMadeSkins.of(catalog.outfits(), catalog, this.sprites::get,
                        this.project.isSlim(), body)));
        this.outfitSpritesStamp = stamp();
    }

    /** What a sheet of ready-made pictures depends on: the atlases here, and the model. */
    private int stamp() {
        return this.atlasRevision * 2 + (this.project.isSlim() ? 1 : 0);
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
        cards.add(new CardWindow.Card("export.mcskincreator.file",
                "export.mcskincreator.file_detail", this::openExportName));
        cards.add(new CardWindow.Card("export.mcskincreator.front",
                "export.mcskincreator.front_detail", this::openFrontViewName));

        // Last of the three, and the only one that leaves this machine. Without a
        // signed-in session there is no token, so the card is left out rather than shown
        // refusing — and the note under the cards says why, because a choice that
        // silently disappears is one nobody can ask about.
        boolean canApply = AccountSkin.available(this.minecraft);
        if (canApply) {
            cards.add(new CardWindow.Card("export.mcskincreator.account",
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

    /**
     * Vanilla's backdrop, except when the point is to see through it.
     *
     * <p>Vanilla draws a blurred copy of what is behind a screen and then the tiled menu
     * background over it. That is the ground the editor stands on and it stays — a
     * resource pack chose it. It is fatal, though, to a camera that is looking <em>at</em>
     * what is behind: the in-game view came out blurred and then hidden altogether. So
     * the one case where it is skipped is the one where the world is the picture.
     *
     * <p>The name is the one 26.x gave it, so the override is versioned rather than
     * shared.
     */
    //? if >=26.1 {
    /*@Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (this.chrome == null || !this.chrome.scene().showsWorld()) {
            super.extractBackground(graphics, mouseX, mouseY, delta);
        }
    }
    *///?} else {
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (this.chrome == null || !this.chrome.scene().showsWorld()) {
            super.renderBackground(graphics, mouseX, mouseY, delta);
        }
    }
    //?}

    //? if >=26.1 {
    /*@Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        paint(new GameCanvas(graphics, this.font), mouseX, mouseY, delta);
    }
    *///?} else {
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        paint(new GameCanvas(graphics, this.font), mouseX, mouseY, delta);
    }
    //?}

    private void paint(Canvas canvas, int mouseX, int mouseY, float delta) {
        List<Element> targets = targets();
        // An open menu owns the pointer. Everything under it is drawn as though the
        // pointer were not there at all, or the layer row behind the menu lights up,
        // offers its tooltip and takes the model's gaze with it.
        boolean blocked = false;
        for (Element element : targets) {
            blocked |= element.overlayActive();
        }
        Paint paint = new Paint(canvas, mouseX, mouseY, System.currentTimeMillis(),
                this.focused, blocked);
        applyHighlight(paint.time());

        this.chrome.draw(paint, delta);

        // Overlays go over their own panel and take clicks before it, which is what
        // keeps an open menu from being painted over by the strip it belongs to.
        for (Element element : targets) {
            if (element.overlayActive()) {
                element.drawOverlay(paint.unblocked());
            }
        }

        if (this.window != null) {
            this.window.draw(paint, this.width, this.height);
        }

        this.toasts.draw(canvas, this.width, this.height, paint.time());
        drawTooltip(paint);
    }

    private void drawTooltip(Paint paint) {
        if (paint.blocked()) {
            // The pointer is on an open menu; whatever it happens to be over has
            // nothing to say about it.
            return;
        }
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
        targets.addAll(this.chrome.targets());
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
                    && dropdown.overlayMouseDown(mouseX, mouseY, new GameCanvas(null, this.font))) {
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

        // The rail of an open window is not one of its children: the window draws it
        // itself, so the press has to be offered to the window before it counts as
        // having landed on nothing.
        if (this.window != null && this.window.barMouseDown(mouseX, mouseY)) {
            this.focused = null;
            blurEverythingBut(targets, null);
            relayout();
            return true;
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
        if (this.window != null && this.window.draggingBar()) {
            if (this.window.barMouseDrag(event.y())) {
                // The body's controls are placed at the offset they were laid out at,
                // so moving the rail is what moves them.
                relayout();
            }
            return true;
        }
        if (this.pressed != null) {
            this.pressed.mouseDrag(event.x(), event.y(), dragX, dragY, event.button());
            for (Panel band : bands()) {
                if (band != this.pressed) {
                    band.mouseDrag(event.x(), event.y(), dragX, dragY, event.button());
                }
            }
            return true;
        }
        for (Element element : targets()) {
            element.mouseDrag(event.x(), event.y(), dragX, dragY, event.button());
        }
        return true;
    }

    /**
     * The bands that own gestures of their own.
     *
     * <p>A gesture can belong to a band rather than to what was pressed: a layer row is
     * taken hold of by its grip, and it is the band that reorders the stack around it.
     * So a band hears the drag and the release whatever they started on, and ignores
     * the ones that are not its own.
     */
    private List<Panel> bands() {
        List<Panel> bands = new ArrayList<>();
        if (this.library.visible()) {
            bands.add(this.library);
        }
        if (this.layers.visible()) {
            bands.add(this.layers);
        }
        return bands;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.window != null && this.window.releaseClosesWindow(event.x(), event.y())) {
            // Only when the gesture started on the backdrop: otherwise selecting text
            // in a field and letting go outside would close the window and lose it.
            closeWindow();
            return true;
        }
        if (this.window != null) {
            this.window.barMouseUp();
        }
        if (this.pressed != null) {
            this.pressed.mouseUp(event.x(), event.y(), event.button());
            for (Panel band : bands()) {
                if (band != this.pressed) {
                    band.mouseUp(event.x(), event.y(), event.button());
                }
            }
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
        for (Element element : targets()) {
            if (element.overlayActive()) {
                // The wheel belongs to whatever is open over the screen, not to the
                // column it happens to be floating above.
                return true;
            }
        }
        if (this.library.visible() && this.library.scroll(mouseX, mouseY, scrollY)) {
            return true;
        }
        if (this.layers.visible() && this.layers.scroll(mouseX, mouseY, scrollY)) {
            return true;
        }
        // Last, so a list under the pointer keeps its own wheel: the scene is the
        // whole middle column, and it would otherwise swallow every scroll over it.
        return this.scene.scroll(mouseX, mouseY, scrollY);
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

    /**
     * True, so a single-player game stops while a skin is being edited.
     *
     * <p>It used to be false, which nobody noticed while the scene was a figure on a
     * flat panel — and which became obvious the moment a camera looked at the real
     * world: mobs closing in behind the editor. Somebody choosing a hat is not playing,
     * and the pause is not theirs to lose.
     *
     * <p>Screens tick either way, so nothing in here stops with it: the composition
     * debounce, the search and the camera all keep running. What does stop is the
     * character's own animation, which is why the first-person swing plays out on a
     * server and stands still at home.
     */
    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    public void removed() {
        super.removed();
        this.scale.restore(this.minecraft);
        this.closed = true;
        // Before anything else: a mod that leaves a player in third person after its
        // window closes has broken their game, not their preview.
        if (this.scene != null) {
            this.scene.release();
        }
        AppliedSkin.stopPreviewing();
        this.preview.close();
        this.baseImage = closed(this.baseImage);
        this.baseOf = null;
        if (this.modelSprites != null) {
            this.modelSprites.close();
            this.modelSprites = null;
        }
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
