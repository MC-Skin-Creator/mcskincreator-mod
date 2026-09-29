/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.remote;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The identifier the storage side of the contract asks for on every call, and which
 * this installation has to hand back unchanged for its library to still be its own.
 */
class ClientIdTest {
    @TempDir
    Path folder;

    @Test
    void anIdIsDrawnAndWrittenDownOnFirstUse() throws IOException {
        Path file = this.folder.resolve("nested").resolve(ClientId.FILE_NAME);

        String drawn = ClientId.read(file);

        assertTrue(ClientId.isUuid(drawn));
        assertTrue(Files.isRegularFile(file), "an id that is not written down is a library lost");
        assertEquals(drawn, Files.readString(file, StandardCharsets.UTF_8).trim());
    }

    @Test
    void anIdAlreadyWrittenDownIsTheOneUsed() throws IOException {
        Path file = this.folder.resolve(ClientId.FILE_NAME);
        String first = ClientId.read(file);

        assertEquals(first, ClientId.read(file));
    }

    @Test
    void surroundingWhitespaceIsNotPartOfTheId() throws IOException {
        Path file = this.folder.resolve(ClientId.FILE_NAME);
        Files.writeString(file, "  123e4567-e89b-12d3-a456-426614174000\n");

        assertEquals("123e4567-e89b-12d3-a456-426614174000", ClientId.read(file));
    }

    @Test
    void aDamagedFileIsReplacedRatherThanSent() throws IOException {
        Path file = this.folder.resolve(ClientId.FILE_NAME);
        Files.writeString(file, "not a uuid at all");

        String drawn = ClientId.read(file);

        assertTrue(ClientId.isUuid(drawn), "the server refuses every call made with a malformed id");
        assertNotEquals("not a uuid at all", Files.readString(file, StandardCharsets.UTF_8).trim());
    }

    @Test
    void whatIsNotAUuidIsNotTakenForOne() {
        assertFalse(ClientId.isUuid(null));
        assertFalse(ClientId.isUuid(""));
        assertFalse(ClientId.isUuid("123e4567e89b12d3a456426614174000"));
        assertTrue(ClientId.isUuid("123e4567-e89b-12d3-a456-426614174000"));
    }
}
