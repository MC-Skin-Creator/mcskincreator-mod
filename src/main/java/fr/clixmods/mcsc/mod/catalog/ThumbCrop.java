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
 * Which part of the body an element's thumbnail shows.
 *
 * <p>The catalogue says so per category and, for a few elements, per element. It
 * matters: a hairstyle drawn on a whole body is eight rows of hair over an empty
 * figure, and a shelf of those is unreadable. Cropping to the part the element
 * actually covers is what the site does.
 *
 * <p>The rectangles are in the coordinates of the 16x32 front view that
 * {@code FrontSprite} produces. A crop has to be one rectangle, so the ones that
 * cover the arms take the whole width rather than the two arms alone.
 */
public enum ThumbCrop {
    ALL("all", 0, 0, 16, 32),
    HEAD("head", 4, 0, 8, 8),
    HEAD_TORSO("headtorso", 0, 0, 16, 20),
    TORSO("torso", 0, 8, 16, 12),
    ARMS("arms", 0, 8, 16, 12),
    LEGS("legs", 4, 20, 8, 12);

    private final String key;
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    ThumbCrop(String key, int x, int y, int width, int height) {
        this.key = key;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /** The crop the catalogue names, or {@link #ALL} for one the mod does not know. */
    public static ThumbCrop of(String key) {
        if (key != null && !key.isBlank()) {
            String wanted = key.trim().toLowerCase(Locale.ROOT);
            for (ThumbCrop crop : values()) {
                if (crop.key.equals(wanted)) {
                    return crop;
                }
            }
        }
        return ALL;
    }

    public int x() {
        return this.x;
    }

    public int y() {
        return this.y;
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }
}
