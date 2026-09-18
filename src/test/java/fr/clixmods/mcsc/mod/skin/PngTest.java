/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.skin;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.zip.CRC32;
import java.util.zip.Inflater;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

/**
 * The PNG the mod hands Mojang.
 *
 * <p>Read two ways on purpose. The structural tests say the bytes are the ones the
 * format asks for, chunk by chunk, so a mistake is named where it happens; the decode
 * through {@link ImageIO} says the whole is a file something other than this encoder
 * agrees is an image, which is the part that actually matters at the far end.
 *
 * <p>{@code ImageIO} is a test's tool, not the mod's: it lives in {@code java.desktop},
 * and a Minecraft client has no business waking the AWT toolkit to save sixty lines.
 */
class PngTest {
    private static final int SIZE = 8;

    /** A deterministic image with alpha, so a channel swapped anywhere shows up. */
    private static byte[] rgba(int width, int height) {
        byte[] pixels = new byte[width * height * 4];
        Random random = new Random(20260918L);
        random.nextBytes(pixels);
        return pixels;
    }

    private record Chunk(String type, byte[] data) {
    }

    /** Walks the file the way a reader would, checking every CRC on the way past. */
    private static List<Chunk> chunks(byte[] png) {
        ByteBuffer buffer = ByteBuffer.wrap(png);
        byte[] signature = new byte[8];
        buffer.get(signature);
        assertArrayEquals(new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', (byte) 0x1A, '\n'},
                signature, "PNG signature");

        List<Chunk> chunks = new ArrayList<>();
        while (buffer.remaining() > 0) {
            int length = buffer.getInt();
            byte[] name = new byte[4];
            buffer.get(name);
            byte[] data = new byte[length];
            buffer.get(data);
            int declared = buffer.getInt();

            CRC32 crc = new CRC32();
            crc.update(name);
            crc.update(data);
            String type = new String(name, StandardCharsets.US_ASCII);
            assertEquals((int) crc.getValue(), declared, "CRC of " + type);
            chunks.add(new Chunk(type, data));
        }
        return chunks;
    }

    @Test
    void theChunksAreTheThreeARgbaImageNeeds() {
        List<Chunk> chunks = chunks(Png.encode(rgba(SIZE, SIZE), SIZE, SIZE));

        assertEquals(List.of("IHDR", "IDAT", "IEND"),
                chunks.stream().map(Chunk::type).toList());
        assertEquals(0, chunks.get(2).data().length, "IEND carries nothing");
    }

    @Test
    void theHeaderDescribesEightBitRgba() {
        Chunk header = chunks(Png.encode(rgba(16, 4), 16, 4)).get(0);
        ByteBuffer fields = ByteBuffer.wrap(header.data());

        assertEquals(13, header.data().length);
        assertEquals(16, fields.getInt(), "width");
        assertEquals(4, fields.getInt(), "height");
        assertEquals(8, fields.get(), "bit depth");
        assertEquals(6, fields.get(), "colour type: RGBA");
        assertEquals(0, fields.get(), "compression");
        assertEquals(0, fields.get(), "filter");
        assertEquals(0, fields.get(), "interlace");
    }

    @Test
    void everyRowIsItsOwnPixelsBehindAFilterByteOfZero() throws Exception {
        byte[] pixels = rgba(SIZE, SIZE);
        Chunk data = chunks(Png.encode(pixels, SIZE, SIZE)).get(1);

        byte[] rows = inflate(data.data(), SIZE * (SIZE * 4 + 1));
        for (int y = 0; y < SIZE; y++) {
            int row = y * (SIZE * 4 + 1);
            assertEquals(0, rows[row], "filter byte of row " + y);
            assertArrayEquals(
                    java.util.Arrays.copyOfRange(pixels, y * SIZE * 4, (y + 1) * SIZE * 4),
                    java.util.Arrays.copyOfRange(rows, row + 1, row + 1 + SIZE * 4),
                    "row " + y);
        }
    }

    private static byte[] inflate(byte[] compressed, int expected) throws Exception {
        Inflater inflater = new Inflater();
        try {
            inflater.setInput(compressed);
            byte[] raw = new byte[expected];
            assertEquals(expected, inflater.inflate(raw), "inflated length");
            assertTrue(inflater.finished(), "the whole stream was read");
            return raw;
        } finally {
            inflater.end();
        }
    }

    @Test
    void anotherDecoderReadsBackEveryPixel() throws IOException {
        byte[] pixels = rgba(SIZE, SIZE);

        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(Png.encode(pixels, SIZE, SIZE)));

        assertNotNull(decoded, "ImageIO did not recognise the file as an image");
        assertEquals(SIZE, decoded.getWidth());
        assertEquals(SIZE, decoded.getHeight());
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int offset = (y * SIZE + x) * 4;
                int argb = decoded.getRGB(x, y);
                assertEquals(pixels[offset] & 0xFF, argb >> 16 & 0xFF, "red at " + x + "," + y);
                assertEquals(pixels[offset + 1] & 0xFF, argb >> 8 & 0xFF, "green at " + x + "," + y);
                assertEquals(pixels[offset + 2] & 0xFF, argb & 0xFF, "blue at " + x + "," + y);
                assertEquals(pixels[offset + 3] & 0xFF, argb >>> 24, "alpha at " + x + "," + y);
            }
        }
    }

    @Test
    void aSkinSizedSheetEncodes() throws IOException {
        int size = FrontSprite.SKIN_SIZE;

        BufferedImage decoded = ImageIO.read(
                new ByteArrayInputStream(Png.encode(rgba(size, size), size, size)));

        assertEquals(size, decoded.getWidth());
        assertEquals(size, decoded.getHeight());
    }

    @Test
    void aPngIsRecognisedAndAnythingElseIsNot() {
        assertTrue(Png.isPng(Png.encode(rgba(SIZE, SIZE), SIZE, SIZE)));
        // A raw atlas buffer, which is the other shape the API answers with.
        assertFalse(Png.isPng(rgba(SIZE, SIZE)));
        assertFalse(Png.isPng(new byte[0]));
        assertFalse(Png.isPng(new byte[] {(byte) 0x89, 'P'}));
    }

    @Test
    void aBufferThatIsNotTheImageItClaimsIsRefused() {
        // Better here than as a body Mojang rejects for reasons nobody can read.
        assertThrows(IllegalArgumentException.class, () -> Png.encode(new byte[10], SIZE, SIZE));
        assertThrows(IllegalArgumentException.class, () -> Png.encode(new byte[0], 0, 0));
    }
}
