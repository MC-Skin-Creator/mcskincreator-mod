/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.preview;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

/**
 * The strings the preview draws.
 *
 * <p>Without this every label would be its own translation key, and a screen of keys
 * is a screen whose widths are wrong: a button is as wide as what is written on it, so
 * "gui.mcskincreator.export" and "Export" do not produce the same layout. The keys are
 * read from the two language files that are already on the test classpath — the game's
 * own and the mod's — which is the same pair the client merges.
 */
final class Translations {
    private static boolean installed;

    private Translations() {
    }

    static synchronized void install() {
        if (installed) {
            return;
        }
        installed = true;

        Map<String, String> strings = new HashMap<>();
        read("assets/minecraft/lang/en_us.json", strings);
        read("assets/mcskincreator/lang/en_us.json", strings);

        Language.inject(new Language() {
            @Override
            public String getOrDefault(String key, String fallback) {
                return strings.getOrDefault(key, fallback);
            }

            @Override
            public boolean has(String key) {
                return strings.containsKey(key);
            }

            @Override
            public boolean isDefaultRightToLeft() {
                return false;
            }

            @Override
            public FormattedCharSequence getVisualOrder(FormattedText text) {
                return sink -> text.visit(
                        (style, string) -> FormattedCharSequence.forward(string, style).accept(sink)
                                ? java.util.Optional.empty()
                                : FormattedText.STOP_ITERATION,
                        Style.EMPTY).isEmpty();
            }
        });
    }

    private static void read(String path, Map<String, String> into) {
        try (InputStream stream = GameAssets.open(path)) {
            if (stream != null) {
                Language.loadFromJson(stream, into::put);
            }
        } catch (Exception failure) {
            // A missing language file costs labels that read as keys, and nothing else.
        }
    }
}
