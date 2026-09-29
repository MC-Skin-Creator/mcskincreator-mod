/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.skin;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

/**
 * Turning a raw RGBA buffer into PNG bytes.
 *
 * <p>The API answers a composed sheet in either of two shapes — a PNG, or the raw
 * 64x64x4 buffer the atlases carry — and Mojang's skin endpoint takes only the first.
 * Something has to bridge that, and on the versions this mod supports nothing already
 * does: {@code NativeImage} reads a PNG and writes one to a <em>file</em>, and there is
 * no call that hands the bytes back. Writing a temporary file only to read it in again
 * would put a disk between two buffers already in memory, and fail for reasons that
 * have nothing to do with skins.
 *
 * <p>So the encoder is here, and it is small because it has exactly one job: 8-bit
 * RGBA, no interlacing, one filter byte of zero per row. That is a handful of chunks
 * and a {@link Deflater}, it needs no native library and no graphics context, and it
 * can therefore be read back byte for byte by a test.
 *
 * <p>A payload that is already a PNG is not re-encoded — see {@link #isPng}.
 */
public final class Png {
    private static final byte[] SIGNATURE = {
        (byte) 0x89, 'P', 'N', 'G', '\r', '\n', (byte) 0x1A, '\n'
    };

    private static final int BIT_DEPTH = 8;
    /** PNG colour type 6: each pixel is red, green, blue and alpha. */
    private static final int COLOR_TYPE_RGBA = 6;
    private static final int CHANNELS = 4;

    private Png() {
    }

    /** Whether a payload is already a PNG, and so needs no encoding at all. */
    public static boolean isPng(byte[] payload) {
        if (payload.length < SIGNATURE.length) {
            return false;
        }
        for (int index = 0; index < SIGNATURE.length; index++) {
            if (payload[index] != SIGNATURE[index]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Encodes {@code rgba} — {@code width * height * 4} bytes, red first — as a PNG.
     *
     * @throws IllegalArgumentException when the buffer is not that many bytes, which
     *                                  means the caller is holding something other than
     *                                  the image it thinks it is
     */
    public static byte[] encode(byte[] rgba, int width, int height) {
        int expected = width * height * CHANNELS;
        if (width <= 0 || height <= 0 || rgba.length != expected) {
            throw new IllegalArgumentException("a " + width + "x" + height
                    + " RGBA image is " + expected + " bytes, not " + rgba.length);
        }

        ByteArrayOutputStream png = new ByteArrayOutputStream(rgba.length / 2);
        png.writeBytes(SIGNATURE);
        chunk(png, "IHDR", header(width, height));
        chunk(png, "IDAT", deflate(scanlines(rgba, width, height)));
        chunk(png, "IEND", new byte[0]);
        return png.toByteArray();
    }

    private static byte[] header(int width, int height) {
        ByteArrayOutputStream header = new ByteArrayOutputStream(13);
        writeInt(header, width);
        writeInt(header, height);
        header.write(BIT_DEPTH);
        header.write(COLOR_TYPE_RGBA);
        // Compression, filtering and interlacing each have exactly one value a PNG
        // reader is required to understand, and these are they.
        header.write(0);
        header.write(0);
        header.write(0);
        return header.toByteArray();
    }

    /**
     * The image as PNG expects it before compression: every row preceded by the filter
     * it was encoded with. Filter 0 is "none" — the row is its own pixels. Anything
     * cleverer would shrink a 16 KiB image that is sent once, by hand, at the cost of
     * code that has to be right.
     */
    private static byte[] scanlines(byte[] rgba, int width, int height) {
        int stride = width * CHANNELS;
        byte[] rows = new byte[height * (stride + 1)];
        for (int y = 0; y < height; y++) {
            rows[y * (stride + 1)] = 0;
            System.arraycopy(rgba, y * stride, rows, y * (stride + 1) + 1, stride);
        }
        return rows;
    }

    private static byte[] deflate(byte[] raw) {
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        try {
            deflater.setInput(raw);
            deflater.finish();
            ByteArrayOutputStream compressed = new ByteArrayOutputStream(raw.length / 2);
            byte[] window = new byte[8192];
            while (!deflater.finished()) {
                compressed.write(window, 0, deflater.deflate(window));
            }
            return compressed.toByteArray();
        } finally {
            // A Deflater holds memory outside the heap, which the garbage collector
            // cannot reclaim on its own.
            deflater.end();
        }
    }

    /** Length, type, data, and a CRC over the type and the data — never over the length. */
    private static void chunk(ByteArrayOutputStream png, String type, byte[] data) {
        byte[] name = type.getBytes(StandardCharsets.US_ASCII);
        writeInt(png, data.length);
        png.writeBytes(name);
        png.writeBytes(data);

        CRC32 crc = new CRC32();
        crc.update(name);
        crc.update(data);
        writeInt(png, (int) crc.getValue());
    }

    private static void writeInt(ByteArrayOutputStream out, int value) {
        out.write(value >>> 24 & 0xFF);
        out.write(value >>> 16 & 0xFF);
        out.write(value >>> 8 & 0xFF);
        out.write(value & 0xFF);
    }
}
