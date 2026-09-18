/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.preview;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import fr.clixmods.mcsc.mod.catalog.Catalog;
import fr.clixmods.mcsc.mod.catalog.CatalogCategory;
import fr.clixmods.mcsc.mod.catalog.CatalogItem;
import fr.clixmods.mcsc.mod.catalog.CatalogText;
import fr.clixmods.mcsc.mod.catalog.ThumbCrop;
import fr.clixmods.mcsc.mod.project.History;
import fr.clixmods.mcsc.mod.project.SkinProject;
import fr.clixmods.mcsc.mod.skin.PreviewSkin;
import fr.clixmods.mcsc.mod.ui.Canvas;
import fr.clixmods.mcsc.mod.ui.EditorChrome;
import fr.clixmods.mcsc.mod.ui.Element;
import fr.clixmods.mcsc.mod.ui.Paint;
import fr.clixmods.mcsc.mod.ui.panel.LayersPanel;
import fr.clixmods.mcsc.mod.ui.panel.LibraryPanel;
import fr.clixmods.mcsc.mod.ui.panel.ScenePanel;
import fr.clixmods.mcsc.mod.ui.panel.TopBar;
import net.minecraft.network.chat.Component;

/**
 * The editor, built out of stand-ins and painted into an image.
 *
 * <p>Everything real about a layout is here: the same panels, the same widgets, the
 * same arithmetic, the same font and the same sprites. What is stood in for is
 * everything that needs a running game — the player figure, the thumbnail sheets the
 * server composes, the mark — and none of those decide where anything goes.
 *
 * <p>The catalogue is made up but not arbitrary. Its names are the lengths real names
 * are, in the language the labels are longest in, because a column that holds
 * "Weathered bronze" is not the column that holds "Ash".
 */
public final class EditorPreview {
    private final SkinProject project = new SkinProject();
    private final History history = new History(this.project);
    private final PreviewSkin preview = new PreviewSkin();

    private final TopBar topBar;
    private final LibraryPanel library;
    private final ScenePanel scene;
    private final LayersPanel layers;
    private final EditorChrome chrome;

    private Catalog catalog = Catalog.EMPTY;
    private boolean laidOut;

    public EditorPreview() {
        Translations.install();

        this.topBar = new TopBar(this.history, () -> null,
                () -> { }, () -> { }, () -> { }, () -> { });
        this.library = new LibraryPanel(this::invalidate, text -> Component.literal(text.base()),
                id -> null, this.project::isSlim, item -> false, tile -> { }, tile -> { },
                tile -> { }, link -> { }, () -> { });
        this.library.createSearch(query -> { });
        this.library.setEmptyMessage(
                () -> Component.translatable("library.mcskincreator.empty"));
        this.scene = new ScenePanel(this.preview, new PreviewFigure(), this::invalidate,
                () -> null);
        this.layers = new LayersPanel(this.project, () -> this.catalog, id -> null, this.history,
                this::invalidate, () -> { }, () -> { }, layer -> { });
        this.chrome = new EditorChrome(this.topBar, this.library, this.scene, this.layers,
                this::invalidate);
    }

    public EditorChrome chrome() {
        return this.chrome;
    }

    public SkinProject project() {
        return this.project;
    }

    /** Hands the panels a catalogue, the way the screen does once it has loaded one. */
    public EditorPreview withCatalog(Catalog catalog) {
        this.catalog = catalog;
        this.library.setCatalog(catalog);
        return this;
    }

    /** Stacks a few elements, so the layers column has something to draw. */
    public EditorPreview withLayers(int count) {
        List<CatalogCategory> categories = this.catalog.categories();
        for (int index = 0; index < count && !categories.isEmpty(); index++) {
            CatalogCategory category = categories.get(index % categories.size());
            if (!category.items().isEmpty()) {
                this.project.add(category, category.items().get(index % category.items().size()), "en");
            }
        }
        return this;
    }

    private void invalidate() {
        this.laidOut = false;
    }

    /** Lays the editor out at this size and paints it. */
    public BufferedImage render(int width, int height) {
        ImageCanvas canvas = new ImageCanvas(width, height);
        // The game's own ground is behind this screen; the preview needs something
        // there to tell a panel's edge from the end of the picture.
        canvas.fill(0, 0, width, height, 0xFF202225);

        this.chrome.layout(canvas, width, height);
        this.laidOut = true;
        // A panel may ask for another pass while it is being laid out — a fold, a
        // dropdown that had to close. Settling that here is what the screen does too.
        for (int pass = 0; pass < 3 && !this.laidOut; pass++) {
            this.laidOut = true;
            this.chrome.layout(canvas, width, height);
        }

        Paint paint = new Paint(canvas, -1, -1, 0L, null);
        this.chrome.draw(paint, 0.0F);
        return canvas.image();
    }

    /** Everything the chrome laid out, so a test can check where it all landed. */
    public List<Element> elements() {
        return new ArrayList<>(this.chrome.targets());
    }

    /** A catalogue of the shape the real one has, with names of the lengths real ones are. */
    public static Catalog sampleCatalog() {
        List<CatalogCategory> categories = new ArrayList<>();
        categories.add(category("skin", "base", "Skin tone", ThumbCrop.ALL,
                "Ash", "Porcelain", "Weathered bronze", "Sand", "Umber", "Slate", "Ochre",
                "Blank template", "Midnight ash", "Sugared almond"));
        categories.add(category("eyes", "head", "Eyes", ThumbCrop.HEAD,
                "Round", "Narrow", "Wide awake", "Sleepy", "Star-struck"));
        categories.add(category("hair", "head", "Hair", ThumbCrop.HEAD,
                "Short crop", "Long and loose", "Braided crown", "Bun", "Undercut", "Curls"));
        categories.add(category("hat", "head", "Hats", ThumbCrop.HEAD,
                "Beanie", "Wide brim", "Crown", "Bandana"));
        categories.add(category("shirt", "torso", "Shirts", ThumbCrop.TORSO,
                "Linen shirt", "Striped tee", "Knitted jumper", "Tabard", "Apron", "Waistcoat"));
        categories.add(category("jacket", "torso", "Jackets", ThumbCrop.TORSO,
                "Rain shell", "Leather jacket", "Cloak"));
        categories.add(category("gloves", "arms", "Gloves", ThumbCrop.ARMS,
                "Fingerless", "Gauntlets", "Mittens"));
        categories.add(category("trousers", "legs", "Trousers", ThumbCrop.LEGS,
                "Work trousers", "Shorts", "Long skirt", "Leggings"));
        categories.add(category("shoes", "legs", "Shoes", ThumbCrop.LEGS,
                "Boots", "Sandals", "Trainers"));
        return new Catalog(categories, java.util.Map.of());
    }

    private static CatalogCategory category(String id, String region, String name,
                                            ThumbCrop crop, String... itemNames) {
        List<CatalogItem> items = new ArrayList<>();
        for (String itemName : itemNames) {
            items.add(new CatalogItem(id + "/" + itemName.toLowerCase(java.util.Locale.ROOT),
                    new CatalogText(itemName, itemName, itemName), items.size(),
                    CatalogItem.NONE, null, ""));
        }
        return new CatalogCategory(id, region, new CatalogText(name, name, name),
                "skin".equals(id), crop, "", items);
    }

    /** The one call a caller needs when all it wants is a picture. */
    public static BufferedImage of(int width, int height) {
        return new EditorPreview().withCatalog(sampleCatalog()).withLayers(5).render(width, height);
    }

    /** Measuring alone, for a caller that wants the geometry rather than the picture. */
    public Canvas measuring(int width, int height) {
        return new ImageCanvas(width, height);
    }
}
