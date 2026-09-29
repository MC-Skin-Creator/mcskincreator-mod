/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.remote;

import java.io.IOException;

/**
 * The server was reached and refused.
 *
 * <p>The status is kept because one of them is not really an error: an atlas answers
 * 404 once its category's content hash has moved on, and the answer to that is a
 * fresh catalogue rather than a message to the player.
 */
public class ApiException extends IOException {
    private static final long serialVersionUID = 1L;

    private final int status;

    public ApiException(int status, String path) {
        super("HTTP " + status + " on " + path);
        this.status = status;
    }

    public int status() {
        return this.status;
    }

    public boolean isNotFound() {
        return this.status == 404;
    }
}
