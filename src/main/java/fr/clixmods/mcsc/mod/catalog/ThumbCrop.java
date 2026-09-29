/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.catalog;

import java.util.Locale;

import fr.clixmods.mcsc.engine.Composition;

/**
 * Which part of the body an element's thumbnail shows.
 *
 * <p>The catalogue says so per category and, for a few elements, per element. It
 * matters: a hairstyle drawn on a whole body is eight rows of hair over an empty
 * figure, and a shelf of those is unreadable. Cropping to the part the element
 * actually covers is what the site does.
 *
 * <p>The rectangles are in the coordinates of the 16x32 front view, and they are
 * <strong>the site's own</strong>: {@code Composition.SPRITE_CROP} of
 * {@code mcsc-engine}, which is what the browser frames its library with. They used
 * to be written out here, tighter — a head was the eight rows of the face where the
 * site gives it ten by eleven — so the same element was framed one way in the game
 * and another on the site. A crop has to be one rectangle, so the ones that cover the
 * arms take the whole width rather than the two arms alone.
 */
public enum ThumbCrop {
    ALL("all"),
    HEAD("head"),
    HEAD_TORSO("headtorso"),
    TORSO("torso"),
    ARMS("arms"),
    LEGS("legs");

    private final String key;
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    ThumbCrop(String key) {
        int[] rectangle = Composition.SPRITE_CROP.get(key);
        this.key = key;
        this.x = rectangle[0];
        this.y = rectangle[1];
        this.width = rectangle[2];
        this.height = rectangle[3];
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
