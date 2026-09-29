/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.preview;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

import fr.clixmods.mcsc.mod.catalog.CatalogModel;
import fr.clixmods.mcsc.mod.catalog.CatalogText;
import fr.clixmods.mcsc.mod.remote.SavedSkin;
import fr.clixmods.mcsc.mod.skin.SkinThumbnails;
import fr.clixmods.mcsc.mod.ui.EditorScale;
import fr.clixmods.mcsc.mod.ui.window.AboutWindow;
import fr.clixmods.mcsc.mod.ui.window.CardWindow;
import fr.clixmods.mcsc.mod.ui.window.ConfirmWindow;
import fr.clixmods.mcsc.mod.ui.window.ModalWindow;
import fr.clixmods.mcsc.mod.ui.window.ModelsWindow;
import fr.clixmods.mcsc.mod.ui.window.NameWindow;
import fr.clixmods.mcsc.mod.ui.window.SkinsWindow;
import fr.clixmods.mcsc.mod.ui.window.TextWindow;
import net.minecraft.network.chat.Component;

/**
 * Renders every window over the editor and writes the pictures out.
 *
 * <p>The same deal as {@link EditorPreviewTest}: nothing is compared, the pictures are
 * there to be looked at. {@code build/ui-preview/windows/} holds one per window, as a
 * 1080p screen shows it.
 *
 * <p>The windows are built here the way the screen builds them. The screen itself needs
 * a running client, so the few lines that assemble each one are repeated rather than
 * reached.
 */
class WindowPreviewTest {
    private static final Path OUTPUT = Paths.get("build", "ui-preview", "windows");

    @Test
    void everyWindowRenders() throws IOException {
        Files.createDirectories(OUTPUT);
        write("export", exportWindow(true), -1, -1);
        write("export-no-session", exportWindow(false), -1, -1);
        write("apply", new ConfirmWindow("window.mcskincreator.apply", List.of(
                TextWindow.Line.of("apply.mcskincreator.what"),
                TextWindow.Line.of("apply.mcskincreator.model"),
                TextWindow.Line.warning("apply.mcskincreator.propagation")),
                () -> Component.translatable("apply.mcskincreator.confirm"), () -> true, () -> { }, null),
                -1, -1);
        write("name", new NameWindow("window.mcskincreator.name", "mon-skin",
                Component.translatable("export.mcskincreator.name_hint"),
                name -> { }, () -> { }, null), -1, -1);
        write("about", new AboutWindow("0.3.0", "clixmods", uri -> { }), -1, -1);
        write("models", modelsWindow(), -1, -1);
        write("skins", skinsWindow(), -1, -1);
    }

    /** The export window with the pointer on its first card, to see the lit state. */
    @Test
    void theExportWindowLightsUp() throws IOException {
        Files.createDirectories(OUTPUT);
        write("export-hover", exportWindow(true), 480, 230);
    }

    static ModalWindow exportWindow(boolean canApply) {
        List<CardWindow.Card> cards = new ArrayList<>();
        cards.add(new CardWindow.Card("export.mcskincreator.file",
                "export.mcskincreator.file_detail", () -> { }));
        cards.add(new CardWindow.Card("export.mcskincreator.front",
                "export.mcskincreator.front_detail", () -> { }));
        cards.add(new CardWindow.Card("export.mcskincreator.folder",
                "export.mcskincreator.folder_detail", () -> { }));
        CardWindow.Lead lead = canApply
                ? new CardWindow.Lead(new CardWindow.Card("export.mcskincreator.account",
                        "export.mcskincreator.account_detail", () -> { }), EditorPreview::mark)
                : null;
        return new CardWindow("window.mcskincreator.export", lead, cards,
                canApply ? null : Component.translatable("export.mcskincreator.no_session"),
                Component.translatable("export.mcskincreator.or_file"), null);
    }

    private static ModalWindow modelsWindow() {
        String[] names = {"Adventurer", "Knight in bronze", "Farmer", "Miner", "Wizard",
            "Pirate captain", "Explorer", "Ranger", "Chef", "Builder"};
        List<CatalogModel> models = new ArrayList<>();
        for (String name : names) {
            models.add(new CatalogModel(name.toLowerCase(java.util.Locale.ROOT),
                    new CatalogText(name, name, name), CatalogModel.Kind.MODEL, false, List.of()));
        }
        return new ModelsWindow(() -> models, text -> Component.literal(text.base()),
                () -> null, model -> { }, null);
    }

    private static ModalWindow skinsWindow() {
        List<SavedSkin> skins = new ArrayList<>();
        String[] names = {"Mon skin du dimanche", "Chevalier", "Test couleurs", "Hiver"};
        long now = 1_790_000_000_000L;
        for (int index = 0; index < names.length; index++) {
            skins.add(new SavedSkin("skin" + index, names[index], now - index * 86_400_000L,
                    new JsonObject()));
        }
        // The first entry is the project in progress, so the preview shows both kinds of row.
        return new SkinsWindow(() -> new SkinsWindow.Library(skins, false, Component.empty()),
                () -> "skin0", new SkinThumbnails(), skin -> { }, skin -> { }, () -> { });
    }

    private static void write(String name, ModalWindow window, int pointerX, int pointerY)
            throws IOException {
        EditorPreview preview = new EditorPreview().withCatalog(EditorPreview.sampleCatalog())
                .withLayers(5).withWindow(window).pointingAt(pointerX, pointerY);
        // A 1080p window, at the scale the editor takes in one: what a player sees.
        int scale = EditorScale.scaleFor(1080, 4);
        ImageIO.write(preview.render(1920 / scale, 1080 / scale, scale), "png",
                OUTPUT.resolve(name + ".png").toFile());
    }
}
