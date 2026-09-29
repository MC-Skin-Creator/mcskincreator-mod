/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.catalog;

import java.util.Locale;

/**
 * The labels a catalogue entry carries: {@code name}, which the catalogue writes in
 * French, plus the {@code en} and {@code es} translations.
 *
 * <p>These are data, not translation keys: they name catalogue content, which the
 * language files cannot know about. Everything the mod says on its own behalf still
 * goes through {@code assets/mcskincreator/lang/}.
 */
public record CatalogText(String base, String en, String es) {
    public static final CatalogText EMPTY = new CatalogText("", "", "");

    /**
     * The label to display for a Minecraft language code such as {@code fr_fr}.
     *
     * <p>A language the catalogue does not carry falls back to English, then to
     * whatever the entry does have, so an entry always shows a name rather than a
     * blank slot.
     */
    public String forLanguage(String languageCode) {
        String language = languageCode == null ? "" : languageCode.toLowerCase(Locale.ROOT);
        if (language.length() > 2) {
            language = language.substring(0, 2);
        }

        return switch (language) {
            case "fr" -> firstFilled(base, en, es);
            case "es" -> firstFilled(es, en, base);
            default -> firstFilled(en, base, es);
        };
    }

    private static String firstFilled(String first, String second, String third) {
        if (!first.isBlank()) {
            return first;
        }
        return second.isBlank() ? third : second;
    }
}
