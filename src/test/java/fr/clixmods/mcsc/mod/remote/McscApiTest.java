/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.remote;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.Test;

/** What the client does to an answer before anything else sees it. */
class McscApiTest {
    @Test
    void anAtlasIsCutIntoOneBufferPerElement() throws IOException {
        byte[] atlas = new byte[McscApi.ATLAS_BUFFER_BYTES * 2];
        atlas[0] = 7;
        atlas[McscApi.ATLAS_BUFFER_BYTES] = 9;

        List<byte[]> buffers = McscApi.slice(atlas, "/atlas/hair/abc");

        assertEquals(2, buffers.size());
        assertEquals(7, buffers.get(0)[0]);
        assertEquals(9, buffers.get(1)[0]);
    }

    @Test
    void anAtlasThatIsNotAWholeNumberOfBuffersIsRefused() {
        assertThrows(IOException.class,
                () -> McscApi.slice(new byte[McscApi.ATLAS_BUFFER_BYTES + 1], "/atlas/hair/abc"));
    }

    @Test
    void anAtlasArrivesCompressedOrNot() throws IOException {
        byte[] plain = "not compressed".getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(compressed)) {
            gzip.write(plain);
        }

        assertArrayEquals(plain, McscApi.gunzip(compressed.toByteArray()));
        assertArrayEquals(plain, McscApi.gunzip(plain),
                "something between the mod and the server may have unwrapped it already");
    }

    @Test
    void aMagnificationStaysInsideWhatTheServerAccepts() {
        assertEquals(McscApi.MIN_SCALE, McscApi.clampScale(0));
        assertEquals(McscApi.MAX_SCALE, McscApi.clampScale(99));
        assertEquals(6, McscApi.clampScale(6));
    }

    @Test
    void aValueGoingIntoAPathIsEscaped() {
        assertEquals("eye-verts-a", McscApi.segment("eye-verts-a"));
        assertEquals("a%2Fb", McscApi.segment("a/b"), "a slash would make a different address");
        assertEquals("a%20b", McscApi.segment("a b"));
    }
}
