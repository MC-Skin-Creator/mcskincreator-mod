/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

//? if >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?} else {
import net.minecraft.client.gui.GuiGraphics;
//?}
import fr.clixmods.mcsc.mod.MCSkinCreatorClient;
import fr.clixmods.mcsc.mod.catalog.Catalog;
import fr.clixmods.mcsc.mod.catalog.CatalogCategory;
import fr.clixmods.mcsc.mod.catalog.CatalogFormatException;
import fr.clixmods.mcsc.mod.catalog.CatalogItem;
import fr.clixmods.mcsc.mod.remote.ApiException;
import fr.clixmods.mcsc.mod.remote.McscApi;
import fr.clixmods.mcsc.mod.skin.CategorySprites;
import fr.clixmods.mcsc.mod.skin.PreviewSkin;
import fr.clixmods.mcsc.mod.skin.ProjectJson;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlayerSkinWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerModelType;

/**
 * The skin editor: the element library on the left, the player model on the right.
 *
 * <p>The model is vanilla's own {@link PlayerSkinWidget}, which already renders a
 * player, already turns under the mouse and already follows the classic/slim model of
 * the {@code PlayerSkin} it is given. All the mod adds is the skin: a texture it
 * builds at runtime, handed over as a {@code PlayerSkin}. There is no renderer to
 * port - no WebGL, no poses, no matrices - because the game has one.
 *
 * <p>Every texture this screen builds is owned by it and released in
 * {@link #removed()}, and a reply that arrives after the screen has gone is dropped
 * rather than uploaded, so closing the editor leaves nothing behind.
 */
public class SkinCreatorScreen extends Screen implements ElementLibrary.Listener {
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 20;
    private static final int MARGIN = 8;
    private static final int TITLE_Y = 12;
    private static final int TITLE_COLOR = 0xFFFFFF;
    private static final int STATUS_COLOR = 0xFFFF7F7F;

    /**
     * How long to wait before asking the server to compose.
     *
     * <p>One composition is one round trip, so a burst of clicks would be a burst of
     * requests. Waiting for the picking to settle collapses them into one. Issue #8
     * removes the wait along with the round trip, by composing locally.
     */
    private static final long COMPOSE_DEBOUNCE_MS = 300;

    /** Vanilla's proportions for a player portrait, from the skin customisation screen. */
    private static final int PREVIEW_WIDTH = 85;
    private static final int PREVIEW_HEIGHT = 120;

    /**
     * The parsed catalogue, kept for the session: it is the same library on every
     * opening, and re-reading it would be a request for nothing. Only the JSON is kept
     * here - the atlases are pixels, and pixels belong to the screen that uploaded
     * them.
     */
    private static Catalog catalog;

    private final Screen parent;
    private final PreviewSkin preview = new PreviewSkin();
    /** One sheet of thumbnails per category, as its atlas arrives. */
    private final Map<String, CategorySprites> sprites = new HashMap<>();
    private final Set<String> requestedAtlases = new HashSet<>();
    private final ElementLibrary library = new ElementLibrary(this, this.sprites::get);

    private Button modelButton;
    /** The column a status line wraps in: over the model, clear of the library panel. */
    private int statusX;
    private int statusWidth;
    private int statusBottom;
    private Component status = Component.empty();
    private boolean loadingCatalog;
    private boolean closed;

    /** What the model is showing, kept so a model toggle can compose it again. */
    private CatalogCategory pickedCategory;
    private CatalogItem pickedItem;
    /** The pick waiting to be composed, and since when. */
    private CatalogCategory pendingCategory;
    private CatalogItem pendingItem;
    private long pendingSince;
    /** Rises with every request, so a slow reply cannot overwrite a newer one. */
    private int composeGeneration;

    public SkinCreatorScreen(Screen parent) {
        super(Component.translatable("screen.mcskincreator.title"));
        this.parent = parent;
        this.preview.model(playerModelType());
    }

    @Override
    protected void init() {
        // A third of the window for the library, the rest for the model. The panel
        // rounds that down to whole slot columns and holds itself within bounds, so a
        // wide window gets more thumbnails across rather than a strip of background.
        int panelWidth = Math.min(ElementLibrary.preferredWidth(this.width / 3),
                Math.max(MARGIN, this.width - MARGIN * 2));
        int contentTop = TITLE_Y + this.font.lineHeight + MARGIN;
        int buttonRowTop = this.height - MARGIN - BUTTON_HEIGHT;
        int contentHeight = Math.max(0, buttonRowTop - MARGIN - contentTop);

        this.library.layout(MARGIN, contentTop, panelWidth, contentHeight);

        int previewLeft = MARGIN + panelWidth + MARGIN;
        int previewSpace = Math.max(0, this.width - previewLeft - MARGIN);
        // Keep the portrait's proportions and let whichever of the two runs out first
        // decide its size, so the model stays whole at any window size or GUI scale.
        int previewHeight = Math.min(contentHeight, Math.min(PREVIEW_HEIGHT,
                previewSpace * PREVIEW_HEIGHT / Math.max(1, PREVIEW_WIDTH)));
        int previewWidth = previewHeight * PREVIEW_WIDTH / PREVIEW_HEIGHT;
        if (previewWidth > 0 && previewHeight > 0) {
            PlayerSkinWidget model = new PlayerSkinWidget(previewWidth, previewHeight,
                    this.minecraft.getEntityModels(), this.preview::playerSkin);
            model.setX(previewLeft + (previewSpace - previewWidth) / 2);
            model.setY(contentTop + (contentHeight - previewHeight) / 2);
            this.addRenderableWidget(model);
        }

        // The status line lives over the model, and wraps within that column: these
        // messages are sentences, and one of them is longer than a narrow column.
        this.statusWidth = Math.max(1, previewSpace);
        this.statusX = previewLeft;
        this.statusBottom = buttonRowTop - MARGIN;

        int buttonsWidth = Math.min(BUTTON_WIDTH * 2 + MARGIN, Math.max(120, previewSpace));
        int buttonWidth = (buttonsWidth - MARGIN) / 2;
        int buttonsLeft = previewLeft + (previewSpace - buttonsWidth) / 2;

        this.modelButton = this.addRenderableWidget(Button
                .builder(this.modelLabel(), ignored -> this.toggleModel())
                .bounds(buttonsLeft, buttonRowTop, buttonWidth, BUTTON_HEIGHT)
                .build());
        this.addRenderableWidget(Button
                .builder(Component.translatable("gui.mcskincreator.close"), ignored -> this.onClose())
                .bounds(buttonsLeft + buttonWidth + MARGIN, buttonRowTop, buttonWidth, BUTTON_HEIGHT)
                .build());

        // init() runs again on every resize, so the catalogue is handed over once:
        // handing it over again would put the player back on the first region.
        if (this.library.hasCatalog()) {
            return;
        }
        if (catalog != null) {
            this.library.catalog(catalog);
        } else {
            this.loadCatalog();
        }
    }

    // 26.x replaced immediate-mode screen drawing with a render-state extraction
    // pass: the hook and its parameter type changed. Both variants do the same two
    // things - hand the drawing calls to a Painter, and let the shared code below
    // draw with it.
    //? if >=26.1 {
    /*@Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        this.paint(new Painter(graphics, this.font), mouseX, mouseY);
    }
    *///?} else {
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        this.paint(new Painter(graphics, this.font), mouseX, mouseY);
    }
    //?}

    private void paint(Painter painter, int mouseX, int mouseY) {
        painter.centeredText(this.title, this.width / 2, TITLE_Y, TITLE_COLOR);
        this.library.render(painter, mouseX, mouseY,
                this.loadingCatalog
                        ? Component.translatable("library.mcskincreator.loading")
                        : Component.translatable("library.mcskincreator.empty"),
                Component.translatable("library.mcskincreator.thumbnails"));

        if (!this.status.getString().isEmpty()) {
            // Grown upwards from the button row, so a message that takes two lines does
            // not end up underneath the buttons.
            painter.wrappedText(this.status, this.statusX,
                    this.statusBottom - ElementLibrary.wrappedHeight(this.status, this.statusWidth),
                    this.statusWidth, STATUS_COLOR);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.pendingItem != null
                && System.currentTimeMillis() - this.pendingSince >= COMPOSE_DEBOUNCE_MS) {
            CatalogCategory category = this.pendingCategory;
            CatalogItem item = this.pendingItem;
            this.pendingCategory = null;
            this.pendingItem = null;
            this.compose(category, item);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        // Widgets first: the buttons and the model are on top of the panel, and the
        // panel must not swallow a click that belongs to one of them.
        if (super.mouseClicked(event, doubled)) {
            return true;
        }
        return this.library.mouseClicked(event.x(), event.y());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return this.library.mouseScrolled(mouseX, mouseY, scrollY)
                || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onElementPicked(CatalogCategory category, CatalogItem item) {
        // Show the element straight away from the atlas buffer already in memory - it
        // is a 64x64 skin in its own right - and let the composed texture replace it
        // when it lands. Picking therefore never waits on the network, and a server
        // that cannot be reached costs the message below rather than the preview.
        CategorySprites loaded = this.sprites.get(category.id());
        byte[] buffer = loaded == null ? null : loaded.buffer(item.atlasIndex());
        if (buffer != null) {
            this.show(buffer, Component.translatable("library.mcskincreator.thumbnails_failed"));
        }

        this.pickedCategory = category;
        this.pickedItem = item;
        this.queueCompose();
    }

    /** Puts the current pick back in the queue, restarting the wait. */
    private void queueCompose() {
        this.pendingCategory = this.pickedCategory;
        this.pendingItem = this.pickedItem;
        this.pendingSince = System.currentTimeMillis();
    }

    private void compose(CatalogCategory category, CatalogItem item) {
        int generation = ++this.composeGeneration;
        String project = ProjectJson.singlePreset(category.id(), item.id(), this.preview.isSlim());

        McscApi.shared().compose(project).whenComplete((texture, failure) ->
                Minecraft.getInstance().execute(() -> {
                    if (this.closed || generation != this.composeGeneration) {
                        return;
                    }
                    if (failure != null) {
                        // The element is already on the model, so this is a note, not a
                        // dead end: the preview is the element on its own rather than
                        // the server's composition of it.
                        MCSkinCreatorClient.LOGGER.warn("Composing {}/{} failed",
                                category.id(), item.id(), failure);
                        this.status = Component.translatable("preview.mcskincreator.compose_failed");
                        return;
                    }
                    this.show(texture, Component.translatable("preview.mcskincreator.compose_failed"));
                }));
    }

    /** Uploads previewed pixels, reporting {@code onFailure} if they cannot be read. */
    private void show(byte[] payload, Component onFailure) {
        try {
            this.preview.show(payload);
            this.status = Component.empty();
        } catch (IOException | RuntimeException cause) {
            MCSkinCreatorClient.LOGGER.warn("Unreadable skin texture of {} bytes", payload.length, cause);
            this.status = onFailure;
        }
    }

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
                        this.status = catalogFailure(failure);
                        return;
                    }
                    catalog = loaded;
                    // Handing the catalogue over is what asks for the first region's
                    // thumbnails, through onCategoriesChanged below.
                    this.library.catalog(loaded);
                }));
    }

    /**
     * Asks for the atlases of the categories on screen, and only those.
     *
     * <p>The library runs to far more pixels than one region shows, so the whole of it
     * is never fetched: opening a region fetches that region, which is what the site
     * does too. Issue #3 keeps them on disk, at which point a second opening fetches
     * nothing at all.
     */
    @Override
    public void onCategoriesChanged() {
        for (CatalogCategory category : this.library.visibleCategories()) {
            if (this.requestedAtlases.add(category.id())) {
                this.requestAtlas(category);
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
                        // fresh catalogue rather than a message. Issue #3 owns that
                        // retry; until then the category stays empty and says so.
                        MCSkinCreatorClient.LOGGER.warn("Reading the atlas of {} failed",
                                category.id(), failure);
                        this.status = Component.translatable(
                                notFound(failure)
                                        ? "library.mcskincreator.thumbnails_stale"
                                        : "library.mcskincreator.thumbnails_failed");
                        return;
                    }
                    this.sprites.put(category.id(), CategorySprites.of(category.id(), buffers));
                }));
    }

    private static boolean notFound(Throwable failure) {
        ApiException refusal = refusal(failure);
        return refusal != null && refusal.isNotFound();
    }

    /** The server's own refusal inside a failure, or {@code null} if it never answered. */
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
     * <p>The three cases are worth separating because they call for different things
     * from whoever reads them: a host that never answered is a network or an address
     * problem, a status is the server declining, and a body that is not a catalogue
     * means the address reached something else entirely. Reporting all three as
     * "unreachable" sends the reader looking at their connection when the address is
     * what is wrong. The address is named for the same reason - the mod can be pointed
     * at another deployment, so which one it tried is half the answer.
     */
    private static Component catalogFailure(Throwable failure) {
        String address = McscApi.shared().baseUrl();
        ApiException refusal = refusal(failure);
        if (refusal != null) {
            return Component.translatable("library.mcskincreator.http_error",
                    refusal.status(), address);
        }
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof CatalogFormatException) {
                return Component.translatable("library.mcskincreator.not_a_catalog", address);
            }
        }
        return Component.translatable("library.mcskincreator.unreachable", address);
    }

    private void toggleModel() {
        this.preview.model(this.preview.isSlim() ? PlayerModelType.WIDE : PlayerModelType.SLIM);
        this.modelButton.setMessage(this.modelLabel());
        // The model decides how the server lays the arms out, so the pick is composed
        // again for the model now on screen. The widget picks the new model up on its
        // own: it reads it from the PlayerSkin on every frame.
        if (this.pickedItem != null) {
            this.queueCompose();
        }
    }

    private Component modelLabel() {
        return Component.translatable(this.preview.isSlim()
                ? "gui.mcskincreator.model.slim"
                : "gui.mcskincreator.model.wide");
    }

    /** The model the player's own skin uses, so the editor opens on the one they wear. */
    private static PlayerModelType playerModelType() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            return client.player.getSkin().model();
        }
        return PlayerModelType.WIDE;
    }

    @Override
    public void removed() {
        super.removed();
        // Releasing here rather than in onClose: this runs however the screen goes
        // away, including when something other than the close button replaces it.
        this.closed = true;
        this.preview.close();
        List<CategorySprites> loaded = new ArrayList<>(this.sprites.values());
        this.sprites.clear();
        for (CategorySprites category : loaded) {
            category.close();
        }
    }

    @Override
    public void onClose() {
        // Hand control back to whichever menu opened us, so Escape behaves the way
        // the player expects from a vanilla sub-screen.
        if (this.minecraft != null) {
            ScreenCompat.setScreen(this.minecraft, this.parent);
        }
    }
}
