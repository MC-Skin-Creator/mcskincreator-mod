/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.catalog;

import java.io.IOException;

/** The server answered, but not with a catalogue. */
public class CatalogFormatException extends IOException {
    private static final long serialVersionUID = 1L;

    public CatalogFormatException(String message) {
        super(message);
    }

    public CatalogFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
