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
import fr.clixmods.mcsc.mod.scene.GameCamera;
import fr.clixmods.mcsc.mod.skin.PreviewSkin;
import fr.clixmods.mcsc.mod.style.Tiles;
import fr.clixmods.mcsc.mod.ui.Canvas;
import fr.clixmods.mcsc.mod.ui.EditorChrome;
import fr.clixmods.mcsc.mod.ui.Element;
import fr.clixmods.mcsc.mod.ui.Paint;
import fr.clixmods.mcsc.mod.ui.panel.LayersPanel;
import fr.clixmods.mcsc.mod.ui.panel.LibraryPanel;
import fr.clixmods.mcsc.mod.ui.panel.ScenePanel;
import fr.clixmods.mcsc.mod.ui.panel.TopBar;
import fr.clixmods.mcsc.mod.ui.window.ModalWindow;
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
    private ModalWindow window;
    private int pointerX = -1;
    private int pointerY = -1;

    /** The language the mod speaks longest, which is the one worth laying out against. */
    public static final String LONGEST_LANGUAGE = "fr_fr";

    public EditorPreview() {
        this(LONGEST_LANGUAGE);
    }

    public EditorPreview(String language) {
        Translations.install(language);

        this.topBar = new TopBar(this.history, EditorPreview::mark, () -> { }, () -> { },
                () -> !this.catalog.models().isEmpty(), () -> { }, () -> { }, () -> { });
        this.library = new LibraryPanel(this::invalidate, text -> Component.literal(text.base()),
                id -> null, this.project::isSlim, item -> false, tile -> { }, outfit -> { },
                tile -> { }, tile -> { }, link -> { }, () -> { });
        this.library.createSearch(query -> { });
        this.library.setEmptyMessage(
                () -> Component.translatable("library.mcskincreator.empty"));
        this.scene = new ScenePanel(this.preview, new PreviewFigure(), new GameCamera(null),
                this::invalidate, () -> null);
        this.layers = new LayersPanel(this.project, () -> this.catalog, id -> null, this.history,
                this::invalidate, () -> { }, () -> { }, layer -> { }, (layer, key) -> { });
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

    /** Opens a window over the editor, the way the screen does. */
    public EditorPreview withWindow(ModalWindow window) {
        this.window = window;
        return this;
    }

    /** Puts the pointer somewhere, so a hovered state can be looked at too. */
    public EditorPreview pointingAt(int x, int y) {
        this.pointerX = x;
        this.pointerY = y;
        return this;
    }

    /**
     * The mod's own icon, read straight out of the resources.
     *
     * <p>In the game it is a texture {@code Logo} uploads; here it is the same file
     * handed to the canvas under the same kind of name.
     */
    public static TopBar.Mark mark() {
        return MARK_IMAGE == null ? null : new TopBar.Mark(MARK_TEXTURE, MARK_IMAGE.getWidth());
    }

    private static final net.minecraft.resources.Identifier MARK_TEXTURE =
            net.minecraft.resources.Identifier.fromNamespaceAndPath("mcskincreator", "preview/icon");
    private static final BufferedImage MARK_IMAGE = readMark();

    private static BufferedImage readMark() {
        try (java.io.InputStream stream =
                     EditorPreview.class.getResourceAsStream("/assets/mcskincreator/icon.png")) {
            return stream == null ? null : javax.imageio.ImageIO.read(stream);
        } catch (java.io.IOException failure) {
            return null;
        }
    }

    private void invalidate() {
        this.laidOut = false;
    }

    /**
     * Lays the editor out at this size and paints it, at the screen's own resolution.
     *
     * <p>The scale is not decoration and it is no longer a magnification either. The
     * game draws this interface into a 960x540 buffer and blows it up to a 1920x1080
     * window — that is what a GUI scale of two is — but the half-size text lives in the
     * window's pixels and not in the buffer's, so blowing the buffer up afterwards
     * could not show it. The buffer is the window's from the start.
     */
    public BufferedImage render(int width, int height, int scale) {
        return paint(width, height, scale);
    }

    /** A rectangle of a picture, for looking at one control rather than a whole screen. */
    public BufferedImage crop(BufferedImage source, int x, int y, int width, int height) {
        int left = Math.max(0, Math.min(x, source.getWidth() - 1));
        int top = Math.max(0, Math.min(y, source.getHeight() - 1));
        return source.getSubimage(left, top,
                Math.min(width, source.getWidth() - left),
                Math.min(height, source.getHeight() - top));
    }

    /** Blows a picture up by a whole factor, sampled the way the game samples it. */
    public BufferedImage magnify(BufferedImage small, int scale) {
        if (scale <= 1) {
            return small;
        }
        BufferedImage large = new BufferedImage(small.getWidth() * scale,
                small.getHeight() * scale, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < large.getHeight(); y++) {
            for (int x = 0; x < large.getWidth(); x++) {
                large.setRGB(x, y, small.getRGB(x / scale, y / scale));
            }
        }
        return large;
    }

    /** Lays the editor out at this size and paints it, one screen pixel to one. */
    public BufferedImage render(int width, int height) {
        return paint(width, height, 1);
    }

    private BufferedImage paint(int width, int height, int scale) {
        ImageCanvas canvas = new ImageCanvas(width, height, scale);
        canvas.supply(Tiles.texture(), grain());
        if (MARK_IMAGE != null) {
            canvas.supply(MARK_TEXTURE, MARK_IMAGE);
        }
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

        Paint paint = new Paint(canvas, this.window == null ? this.pointerX : -1,
                this.window == null ? this.pointerY : -1, 0L, null);
        this.chrome.draw(paint, 0.0F);
        if (this.window != null) {
            this.window.layout(canvas, width, height, () -> { });
            this.window.draw(new Paint(canvas, this.pointerX, this.pointerY, 0L, null), width, height);
        }
        return canvas.image();
    }

    /**
     * The grain the materials are speckled with.
     *
     * <p>In the game it is a texture the client uploads on first use; here it is the
     * same function evaluated into an image, so the preview shows the same speckle
     * rather than four flat rectangles.
     */
    private static BufferedImage grain() {
        BufferedImage image = new BufferedImage(Tiles.SIZE, Tiles.SIZE,
                BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < Tiles.SIZE; y++) {
            for (int x = 0; x < Tiles.SIZE; x++) {
                image.setRGB(x, y, Tiles.grey(Tiles.shade(x, y)));
            }
        }
        return image;
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
        return new Catalog(categories, java.util.Map.of(), List.of(), List.of());
    }

    private static CatalogCategory category(String id, String region, String name,
                                            ThumbCrop crop, String... itemNames) {
        List<CatalogItem> items = new ArrayList<>();
        for (String itemName : itemNames) {
            items.add(new CatalogItem(id + "/" + itemName.toLowerCase(java.util.Locale.ROOT),
                    new CatalogText(itemName, itemName, itemName), items.size(),
                    CatalogItem.NONE, null, "", sampleColors(id)));
        }
        return new CatalogCategory(id, region, new CatalogText(name, name, name),
                "skin".equals(id), crop, "", items);
    }

    /**
     * Colour keys the way the catalogue gives them: most elements have one or two, and
     * the selected shirt has enough to show how the swatch row wraps — or does not.
     */
    private static java.util.Map<String, Integer> sampleColors(String categoryId) {
        java.util.Map<String, Integer> colors = new java.util.LinkedHashMap<>();
        switch (categoryId) {
            case "skin" -> colors.put("tone", 0xC89878);
            case "shirt" -> {
                colors.put("main", 0x3C6E9E);
                colors.put("accent", 0xE0C060);
                colors.put("collar", 0xF2F2F2);
                colors.put("button", 0x5A3A22);
                colors.put("stripe", 0xA03B31);
            }
            default -> colors.put("main", 0x6A8A4A);
        }
        return colors;
    }

    /** The one call a caller needs when all it wants is a picture. */
    public static BufferedImage of(int width, int height, int scale) {
        return of(LONGEST_LANGUAGE, width, height, scale);
    }

    public static BufferedImage of(String language, int width, int height, int scale) {
        return new EditorPreview(language).withCatalog(sampleCatalog()).withLayers(5)
                .render(width, height, scale);
    }

    /** Measuring alone, for a caller that wants the geometry rather than the picture. */
    public Canvas measuring(int width, int height) {
        return new ImageCanvas(width, height, 1);
    }
}
