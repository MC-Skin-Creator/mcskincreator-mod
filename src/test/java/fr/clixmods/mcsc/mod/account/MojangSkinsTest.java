/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.account;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

/**
 * The request sent to Mojang: the body it expects, and the address it is sent to.
 *
 * <p>The last test here is not about correctness. It is the one that fails if the
 * endpoint is ever pointed somewhere else, which is the single change to this mod that
 * would turn it into the thing its category is known for.
 */
class MojangSkinsTest {
    private static final String BOUNDARY = "mcscboundary";
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 1, 2, 3, 4};

    private static String text(byte[] body) {
        // Latin-1 so every byte survives the trip: the PNG in the middle is not text.
        return new String(body, StandardCharsets.ISO_8859_1);
    }

    @Test
    void theBodyCarriesTheVariantAsAPlainField() {
        String body = text(MojangSkins.multipart(BOUNDARY, MojangSkins.SLIM, PNG));

        assertTrue(body.startsWith("--" + BOUNDARY + "\r\n"), body);
        assertTrue(body.contains("Content-Disposition: form-data; name=\"variant\"\r\n\r\nslim\r\n"),
                body);
    }

    @Test
    void theBodyCarriesTheSheetAsAPngFile() {
        String body = text(MojangSkins.multipart(BOUNDARY, MojangSkins.CLASSIC, PNG));

        assertTrue(body.contains("Content-Disposition: form-data; name=\"file\";"
                + " filename=\"skin.png\"\r\nContent-Type: image/png\r\n\r\n"), body);
    }

    @Test
    void theBodyEndsOnTheClosingBoundary() {
        String body = text(MojangSkins.multipart(BOUNDARY, MojangSkins.CLASSIC, PNG));

        assertTrue(body.endsWith("\r\n--" + BOUNDARY + "--\r\n"), body);
    }

    @Test
    void thePixelsGoThroughUntouched() {
        byte[] body = MojangSkins.multipart(BOUNDARY, MojangSkins.CLASSIC, PNG);

        int start = text(body).indexOf("image/png\r\n\r\n") + "image/png\r\n\r\n".length();
        assertArrayEquals(PNG, Arrays.copyOfRange(body, start, start + PNG.length));
    }

    @Test
    void theModelDecidesTheVariant() {
        assertEquals("slim", MojangSkins.variant(true));
        assertEquals("classic", MojangSkins.variant(false));
    }

    @Test
    void theTokenOnlyEverGoesToMojang() {
        // The one address the access token is allowed to reach. Changing it is the
        // change this test exists to make somebody justify.
        assertEquals("https", MojangSkins.ENDPOINT.getScheme());
        assertEquals("api.minecraftservices.com", MojangSkins.host());
        assertEquals("/minecraft/profile/skins", MojangSkins.ENDPOINT.getPath());
    }

    @Test
    void aRefusalCarriesItsStatusAndNothingElse() {
        SkinUploadException refusal = new SkinUploadException(429, Duration.ofSeconds(30));

        assertEquals(429, refusal.status());
        assertEquals(SkinUploadException.Reason.RATE_LIMITED, refusal.reason());
        assertEquals(Duration.ofSeconds(30), refusal.retryAfter().orElseThrow());
        // Whatever ends up in a log or a crash report must not describe the request.
        assertEquals("Mojang answered HTTP 429 on the skin upload", refusal.getMessage());
    }

    @Test
    void eachStatusMapsToWhatThePlayerCanDoAboutIt() {
        assertEquals(SkinUploadException.Reason.SESSION_EXPIRED,
                new SkinUploadException(401, null).reason());
        assertEquals(SkinUploadException.Reason.SESSION_EXPIRED,
                new SkinUploadException(403, null).reason());
        assertEquals(SkinUploadException.Reason.RATE_LIMITED,
                new SkinUploadException(429, null).reason());
        assertEquals(SkinUploadException.Reason.REFUSED,
                new SkinUploadException(400, null).reason());
        assertEquals(SkinUploadException.Reason.REFUSED,
                new SkinUploadException(500, null).reason());
        assertTrue(new SkinUploadException(500, null).retryAfter().isEmpty());
    }
}
