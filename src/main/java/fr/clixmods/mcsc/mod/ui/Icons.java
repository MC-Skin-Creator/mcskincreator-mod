/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.ui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.platform.NativeImage;

import fr.clixmods.mcsc.mod.MCSkinCreatorClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * The interface icons: 16x16 pixel drawings, packed into one atlas.
 *
 * <p>There are no emoji anywhere in this interface, not even as a stop-gap. An emoji
 * is drawn by whatever font the machine happens to have: its shape, its colour and
 * its size change from one device to the next, and it is round and smoothed in the
 * middle of an interface that is square and hard-edged.
 *
 * <p>Each icon is a 14x14 drawing inside a 16x16 cell, which leaves the one pixel of
 * margin the black outline needs. That outline is laid on automatically, around
 * whatever silhouette the drawing happens to have, exactly as the site generates it —
 * so an icon is described by its shape and its four colours and never has to carry
 * its own border.
 *
 * <p>The atlas is built at runtime rather than shipped as a PNG. A drawing written
 * out as rows of characters can be read, reviewed and corrected in a diff; a binary
 * of the same thing cannot, and would still have to be regenerated whenever the
 * palette moves.
 */
public final class Icons {
    /** One cell, and the size an icon is drawn at x1. */
    public static final int SIZE = 16;
    /** The drawing inside the cell, leaving a pixel of margin all round. */
    private static final int ART = 14;
    private static final int COLUMNS = 6;

    private static final Map<String, Icon> ICONS = new LinkedHashMap<>();
    private static final Identifier ATLAS =
            Identifier.fromNamespaceAndPath(MCSkinCreatorClient.MOD_ID, "generated/icons");

    private static boolean registered;

    private Icons() {
    }

    private record Icon(int index, int main, int light, int dark, int accent, List<String> rows) {
    }

    static {
        define("skin", 0xFFECB17C, 0xFFFFE0CC, 0xFFA86B3F, 0xFF573320,
                "....######....",
                "...########...",
                "..##########..",
                "..##########..",
                "..#o#####o##..",
                "..##########..",
                "..###oooo###..",
                "..##########..",
                "...########...",
                "....######....",
                "....######....",
                "..##########..",
                ".############.",
                ".############.");
        define("hair", 0xFF3A2A1C, 0xFF6B4423, 0xFF282828, 0xFFECB17C,
                "....######....",
                "...########...",
                "..##########..",
                ".############.",
                ".####oooo####.",
                ".###oooooo###.",
                "..#oooooooo#..",
                "..#oooooooo#..",
                "..#oooooooo#..",
                "..#oooooooo#..",
                "...oooooooo...",
                "....oooooo....",
                "..............",
                "..............");
        define("beard", 0xFF3A2A1C, 0xFF6B4423, 0xFF282828, 0xFFECB17C,
                "..............",
                "..............",
                "..oooooooooo..",
                "..oooooooooo..",
                "..o#oooooo#o..",
                "..##oooooo##..",
                "..##########..",
                "..##########..",
                "...########...",
                "....######....",
                ".....####.....",
                "..............",
                "..............",
                "..............");
        define("eyes", 0xFF4B8FD6, 0xFFFFFFFF, 0xFF282828, 0xFF282828,
                "..............",
                "..............",
                "..............",
                "..++++..++++..",
                ".++++++++++++.",
                ".++##++++##++.",
                ".++##++++##++.",
                ".++++++++++++.",
                "..++++..++++..",
                "..............",
                "..............",
                "..............",
                "..............",
                "..............");
        define("mouth", 0xFFC0392B, 0xFFE05A47, 0xFF8B1E1E, 0xFFFFFFFF,
                "..............",
                "..............",
                "..............",
                "..#........#..",
                "..#........#..",
                "..##......##..",
                "..###....###..",
                "...########...",
                "...+oooooo+...",
                "....######....",
                "..............",
                "..............",
                "..............",
                "..............");
        define("glasses", 0xFF282828, 0xFF7C7C84, 0xFF0C0C0C, 0xFF59E0FF,
                "..............",
                "..............",
                "..............",
                ".####....####.",
                ".#oo#....#oo#.",
                ".#oo######oo#.",
                ".#oo#....#oo#.",
                ".####....####.",
                "..............",
                "..............",
                "..............",
                "..............",
                "..............",
                "..............");
        define("hat", 0xFFC0392B, 0xFFE05A47, 0xFF8B1E1E, 0xFF282828,
                "..............",
                "..............",
                "....++++++....",
                "...########...",
                "...########...",
                "..##########..",
                ".############.",
                ".oooooooooooo.",
                "..............",
                "..............",
                "..............",
                "..............",
                "..............",
                "..............");
        define("shirt", 0xFF4B8FD6, 0xFF59E0FF, 0xFF2C4B8C, 0xFFFFFFFF,
                "..............",
                "..###....###..",
                ".#####..#####.",
                ".############.",
                ".############.",
                ".###++++++###.",
                "..#++++++++#..",
                "..#++++++++#..",
                "..#++++++++#..",
                "..#++++++++#..",
                "..#++++++++#..",
                "..##########..",
                "..............",
                "..............");
        define("jacket", 0xFF573320, 0xFF84512C, 0xFF3A2A1C, 0xFF282828,
                "..............",
                "..###....###..",
                ".#####..#####.",
                ".####oooo####.",
                ".####oooo####.",
                ".###+o..o+###.",
                "..#++o..o++#..",
                "..#++o..o++#..",
                "..#++o..o++#..",
                "..#++o..o++#..",
                "..#++o..o++#..",
                "..#########+..",
                "..............",
                "..............");
        define("pants", 0xFF2C4B8C, 0xFF4B8FD6, 0xFF1B2B50, 0xFF0C0C0C,
                "..............",
                "..............",
                "..##########..",
                "..##########..",
                "..####++####..",
                "..###+..+###..",
                "..###....###..",
                "..###....###..",
                "..###....###..",
                "..###....###..",
                "..ooo....ooo..",
                "..............",
                "..............",
                "..............");
        define("boot", 0xFF3A2A1C, 0xFF6B4423, 0xFF0C0C0C, 0xFF282828,
                "..............",
                "..............",
                "..............",
                "...####.......",
                "...####.......",
                "...####.......",
                "...####.......",
                "...#####......",
                "...########...",
                "...########...",
                "...oooooooo...",
                "..............",
                "..............",
                "..............");
        define("arm", 0xFFECB17C, 0xFFFFE0CC, 0xFFA86B3F, 0xFFC9A227,
                "..............",
                "....####......",
                "....####......",
                "....####......",
                "....oooo......",
                "....oooo......",
                "....####......",
                "....####......",
                "....#####.....",
                ".....#####....",
                "......####....",
                "......####....",
                "..............",
                "..............");
        define("legband", 0xFF84512C, 0xFFA86B3F, 0xFF573320, 0xFFC9A227,
                "..............",
                "..............",
                "...###..###...",
                "...###..###...",
                "...###..###...",
                "..oooooooooo..",
                "..oooooooooo..",
                "...###..###...",
                "...###..###...",
                "...###..###...",
                "...###..###...",
                "..............",
                "..............",
                "..............");
        define("backpack", 0xFF6B4423, 0xFF8A5A2B, 0xFF3A2A1C, 0xFFC9A227,
                "..............",
                "....######....",
                "...########...",
                "..#+######+#..",
                "..#+######+#..",
                "..##########..",
                "..#oooooooo#..",
                "..#oooooooo#..",
                "..##########..",
                "..##########..",
                "..##--------..",
                "...########...",
                "..............",
                "..............");
        define("outfit", 0xFF43C07C, 0xFF7EC850, 0xFF2F6F4F, 0xFFFFFFFF,
                "..............",
                "..###....###..",
                ".#####..#####.",
                ".############.",
                ".####oooo####.",
                ".###+oooo+###.",
                "..#++oooo++#..",
                "..#++++++++#..",
                "..#########...",
                "..###....###..",
                "..###....###..",
                "..###....###..",
                "..ooo....ooo..",
                "..............");
        define("folder", 0xFFC9A227, 0xFFE8B23A, 0xFF84512C, 0xFF573320,
                "..............",
                "..............",
                "..####........",
                ".######.......",
                ".############.",
                ".############.",
                ".#++++++++++#.",
                ".#++++++++++#.",
                ".#++++++++++#.",
                ".############.",
                ".############.",
                "..............",
                "..............",
                "..............");
        define("save", 0xFF4B8FD6, 0xFFFFFFFF, 0xFF2C4B8C, 0xFF282828,
                "..............",
                ".############.",
                ".#++++++++++#.",
                ".#+oooooooo+#.",
                ".#+oooooooo+#.",
                ".#++++++++++#.",
                ".############.",
                ".#----------#.",
                ".#-++++++++-#.",
                ".#-++++++++-#.",
                ".#-++++++++-#.",
                ".############.",
                "..............",
                "..............");
        define("logo", 0xFF3C8527, 0xFF7EC850, 0xFF23511A, 0xFFFCFC54,
                "..............",
                "...########...",
                "..##########..",
                ".####oooo####.",
                ".###oooooo###.",
                ".##oo####oo##.",
                ".##o######o##.",
                ".##oo####oo##.",
                ".###oooooo###.",
                ".####oooo####.",
                "..##########..",
                "...########...",
                "..............",
                "..............");
    }

    private static void define(String name, int main, int light, int dark, int accent, String... rows) {
        ICONS.put(name, new Icon(ICONS.size(), main, light, dark, accent, List.of(rows)));
    }

    /** True when the atlas actually carries this icon — a missing one is never drawn. */
    public static boolean has(String name) {
        return ICONS.containsKey(name);
    }

    /** Builds and registers the atlas on first use. Safe to call every frame. */
    public static void ensureRegistered(Minecraft client) {
        if (registered) {
            return;
        }
        registered = true;

        int rows = (ICONS.size() + COLUMNS - 1) / COLUMNS;
        NativeImage image = new NativeImage(COLUMNS * SIZE, rows * SIZE, false);
        // NativeImage does not promise a blank buffer, so the sheet is cleared first.
        image.fillRect(0, 0, image.getWidth(), image.getHeight(), 0);
        for (Icon icon : ICONS.values()) {
            paint(image, icon);
        }
        client.getTextureManager().register(ATLAS, new DynamicTexture(() -> "mcsc icons", image));
    }

    /**
     * Draws an icon.
     *
     * <p>{@code scale} is a whole number and nothing else: an icon shown at one and a
     * half times its size is an icon with half-pixels in it, and the whole interface
     * is built on the promise that there are none.
     */
    public static void draw(Canvas canvas, String name, int x, int y, int scale) {
        Icon icon = ICONS.get(name);
        if (icon == null) {
            return;
        }
        int column = icon.index() % COLUMNS;
        int row = icon.index() / COLUMNS;
        int sheetRows = (ICONS.size() + COLUMNS - 1) / COLUMNS;
        canvas.blit(ATLAS, x, y, SIZE * scale, SIZE * scale,
                column * SIZE, row * SIZE, SIZE, SIZE, COLUMNS * SIZE, sheetRows * SIZE);
    }

    /** The same, dimmed or tinted — which is how a disabled control keeps its icon. */
    public static void drawTinted(Canvas canvas, String name, int x, int y, int scale, int tint) {
        Icon icon = ICONS.get(name);
        if (icon == null) {
            return;
        }
        int column = icon.index() % COLUMNS;
        int row = icon.index() / COLUMNS;
        int sheetRows = (ICONS.size() + COLUMNS - 1) / COLUMNS;
        canvas.blitTinted(ATLAS, x, y, SIZE * scale, SIZE * scale,
                column * SIZE, row * SIZE, SIZE, SIZE, COLUMNS * SIZE, sheetRows * SIZE, tint);
    }

    private static void paint(NativeImage image, Icon icon) {
        int originX = (icon.index() % COLUMNS) * SIZE + 1;
        int originY = (icon.index() / COLUMNS) * SIZE + 1;

        int[][] cell = new int[ART][ART];
        for (int y = 0; y < ART; y++) {
            String line = icon.rows().get(y);
            for (int x = 0; x < ART; x++) {
                cell[y][x] = switch (line.charAt(x)) {
                    case '#' -> icon.main();
                    case '+' -> icon.light();
                    case '-' -> icon.dark();
                    case 'o' -> icon.accent();
                    default -> 0;
                };
            }
        }

        // The outline goes on after the drawing is complete, so that it rings the
        // silhouette as a whole rather than each colour inside it.
        List<int[]> outline = new ArrayList<>();
        for (int y = -1; y <= ART; y++) {
            for (int x = -1; x <= ART; x++) {
                if (opaque(cell, x, y)) {
                    continue;
                }
                if (opaque(cell, x - 1, y) || opaque(cell, x + 1, y)
                        || opaque(cell, x, y - 1) || opaque(cell, x, y + 1)) {
                    outline.add(new int[] {x, y});
                }
            }
        }

        for (int y = 0; y < ART; y++) {
            for (int x = 0; x < ART; x++) {
                if (cell[y][x] != 0) {
                    image.setPixel(originX + x, originY + y, cell[y][x]);
                }
            }
        }
        for (int[] pixel : outline) {
            int x = originX + pixel[0];
            int y = originY + pixel[1];
            if (x >= 0 && y >= 0 && x < image.getWidth() && y < image.getHeight()) {
                image.setPixel(x, y, 0xFF000000);
            }
        }
    }

    private static boolean opaque(int[][] cell, int x, int y) {
        return x >= 0 && y >= 0 && x < ART && y < ART && cell[y][x] != 0;
    }
}
