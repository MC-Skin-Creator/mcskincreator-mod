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

import fr.clixmods.mcsc.mod.style.Metrics;
import fr.clixmods.mcsc.mod.style.Surface;
import fr.clixmods.mcsc.mod.ui.panel.LayersPanel;
import fr.clixmods.mcsc.mod.ui.panel.LibraryPanel;
import fr.clixmods.mcsc.mod.ui.panel.ScenePanel;
import fr.clixmods.mcsc.mod.ui.panel.TopBar;
import fr.clixmods.mcsc.mod.ui.widget.PixelButton;
import net.minecraft.network.chat.Component;

/**
 * The four zones of the editor, and where they go.
 *
 * <p>Three columns — library, scene, layers — under a top bar, while the width allows
 * it; below that the side columns become drawers driven by a tab bar at the bottom,
 * and the drawer lands under the scene in portrait and beside it in landscape. The
 * scene never goes away whatever the width: choosing an element while the model is
 * hidden is choosing blind, and the site tried two arrangements that did exactly that
 * before settling on this one.
 *
 * <p>Separate from {@link SkinCreatorScreen} because the screen is also the editor's
 * network, its history, its windows and its export, and none of that has anything to
 * say about where a panel goes. What is here is geometry and paint order and nothing
 * else — it holds no Minecraft screen, so the same arrangement can be laid out and
 * painted onto any {@link Canvas}, which is what the preview tool in the tests does.
 */
public final class EditorChrome {
    /** Which side column is open, when the width is too small to show both. */
    public enum Drawer {
        NONE, LIBRARY, LAYERS
    }

    private final TopBar topBar;
    private final LibraryPanel library;
    private final ScenePanel scene;
    private final LayersPanel layers;
    private final Runnable relayout;

    private int width;
    private int height;
    private Drawer drawer = Drawer.NONE;
    private PixelButton libraryTab;
    private PixelButton layersTab;
    private boolean overlaid;
    private boolean foldedLibrary;
    private boolean foldedLayers;

    public EditorChrome(TopBar topBar, LibraryPanel library, ScenePanel scene, LayersPanel layers,
                        Runnable relayout) {
        this.topBar = topBar;
        this.library = library;
        this.scene = scene;
        this.layers = layers;
        this.relayout = relayout;
    }

    public TopBar topBar() {
        return this.topBar;
    }

    public LibraryPanel library() {
        return this.library;
    }

    public ScenePanel scene() {
        return this.scene;
    }

    public LayersPanel layers() {
        return this.layers;
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }

    /**
     * Works out where everything goes. Redone on resize, on a fold, and whenever the
     * contents change.
     *
     * <p>The canvas it measures with need not be able to draw: laying out only ever
     * asks the surface how wide a label is.
     */
    public void layout(Canvas canvas, int width, int height) {
        this.width = width;
        this.height = height;
        foldForTheWorld();

        this.topBar.setBounds(0, 0, width, Metrics.TOP_BAR_HEIGHT);
        this.topBar.layout(canvas);

        int top = Metrics.TOP_BAR_HEIGHT;
        boolean wide = width >= Metrics.WIDE_LAYOUT;
        int libraryColumn = wide ? Metrics.LIBRARY_WIDTH : Metrics.LIBRARY_WIDTH_NARROW;
        int layersColumn = wide ? Metrics.LAYERS_WIDTH : Metrics.LAYERS_WIDTH_NARROW;
        if (width >= columnsMinimum()) {
            this.drawer = Drawer.NONE;
            this.libraryTab = null;
            this.layersTab = null;
            this.library.setVisible(true);
            this.layers.setVisible(true);

            int libraryWidth = this.library.folded() ? Metrics.COLLAPSED_WIDTH : libraryColumn;
            int layersWidth = this.layers.folded() ? Metrics.COLLAPSED_WIDTH : layersColumn;
            this.library.setBounds(0, top, libraryWidth, height - top);
            this.layers.setBounds(width - layersWidth, top, layersWidth, height - top);
            this.scene.setBounds(libraryWidth, top,
                    width - libraryWidth - layersWidth, height - top);
        } else {
            layoutDrawers(canvas, top);
        }

        // The figure may paint over everything below the top bar, not only over the
        // column it stands in: zoomed in, a head that stopped at the library's edge was
        // simply cut off in mid-air. The columns are painted after it, so what spills
        // under them is covered rather than seen.
        this.scene.setStage(0, top, width, height - top);

        this.library.layout(canvas);
        this.layers.layout(canvas);
        this.scene.layout(canvas);
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
                PixelButton.Style.TAB, () -> toggleDrawer(Drawer.LIBRARY));
        this.layersTab = new PixelButton(Component.translatable("panel.mcskincreator.layers"),
                PixelButton.Style.TAB, () -> toggleDrawer(Drawer.LAYERS));
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
            int drawerWidth = Math.min(Metrics.LIBRARY_WIDTH_NARROW, this.width / 2);
            open.setBounds(0, top, drawerWidth, usableBottom - top);
            this.scene.setBounds(drawerWidth, top, this.width - drawerWidth, usableBottom - top);
        }
    }

    private void toggleDrawer(Drawer requested) {
        this.drawer = this.drawer == requested ? Drawer.NONE : requested;
        this.relayout.run();
    }

    /** Below this width the two columns and the scene no longer fit side by side. */
    private int columnsMinimum() {
        return Metrics.LIBRARY_WIDTH_NARROW + Metrics.LAYERS_WIDTH_NARROW
                + Metrics.MIN_SCENE_WIDTH;
    }

    /** True while the side columns are drawers rather than columns. */
    public boolean narrow() {
        return this.width < columnsMinimum();
    }

    /** The "+" of the layers panel: bring the library forward. */
    public void revealLibrary() {
        this.library.setFolded(false);
        if (narrow()) {
            this.drawer = Drawer.LIBRARY;
        }
        this.relayout.run();
    }

    /**
     * Paints the four zones, in the order they have to be painted in.
     *
     * <p>The ground is the game's own: the panorama behind a menu, the world behind a
     * pause screen. Painting over it would be replacing something the player's resource
     * pack may well have chosen. The scene paints its own over its own rectangle, but
     * only when the backdrop chooser says to — that is the player asking.
     */
    public void draw(Paint paint, float delta) {
        this.topBar.draw(paint);
        this.scene.draw(paint, delta);
        if (this.library.visible()) {
            this.library.draw(paint);
        }
        if (this.layers.visible()) {
            this.layers.draw(paint);
        }
        drawTabs(paint);
    }

    /**
     * Gets the columns out of the way when the game is drawing the character.
     *
     * <p>A camera looking at the world needs the window, not a third of it: the game
     * draws the first-person arm low and to the right, which is exactly where the layers
     * column was. The fold is this screen's, not the player's, so their own choice is
     * remembered on the way in and given back on the way out.
     */
    private void foldForTheWorld() {
        boolean overlay = this.scene.overlaysGame();
        if (overlay == this.overlaid) {
            return;
        }
        this.overlaid = overlay;
        if (overlay) {
            this.foldedLibrary = this.library.folded();
            this.foldedLayers = this.layers.folded();
            this.drawer = Drawer.NONE;
        }
        this.library.setFolded(overlay || this.foldedLibrary);
        this.layers.setFolded(overlay || this.foldedLayers);
    }

    private void drawTabs(Paint paint) {
        if (this.libraryTab == null) {
            return;
        }
        Canvas canvas = paint.canvas();
        int barTop = this.libraryTab.y() - Metrics.PAD_TIGHT;
        Surface.panel(canvas, 0, barTop, this.width, this.height - barTop);
        this.libraryTab.draw(paint);
        this.layersTab.draw(paint);
    }

    /**
     * Everything in the chrome that can be clicked or focused, front to back.
     *
     * <p>A band comes after its own contents: it is in the list for the rail down its
     * edge, and a tile sitting on top of the rail would otherwise never be hit.
     */
    public List<Element> targets() {
        List<Element> targets = new ArrayList<>();
        targets.addAll(this.topBar.children());
        targets.addAll(this.scene.controls());
        if (this.library.visible()) {
            targets.addAll(this.library.hitTargets());
            targets.add(this.library);
        }
        if (this.layers.visible()) {
            targets.addAll(this.layers.hitTargets());
            targets.add(this.layers);
        }
        if (this.libraryTab != null) {
            targets.add(this.libraryTab);
            targets.add(this.layersTab);
        }
        targets.add(this.scene);
        return targets;
    }
}
