/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.account;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import fr.clixmods.mcsc.mod.MCSkinCreatorClient;

/**
 * The one place in the mod that sends the player's access token anywhere, and the only
 * address it ever sends it to is {@link #ENDPOINT}.
 *
 * <p>This is exactly the call the official launcher makes for the account it belongs
 * to. What the launcher does not do, and what this will not do either, is make it on
 * its own: {@link AccountSkin} only reaches this from a button the player pressed, one
 * upload per press.
 *
 * <p>The token is a parameter and never a field, it is read at the moment of the call
 * and dropped with the stack frame, and nothing here logs a header, a body or a
 * request. The failure this raises carries a status and nothing else, for the same
 * reason. A mod that touches the session token is guilty until it is read, so this file
 * and {@link GameSession} are deliberately the two short ones: they are what a
 * suspicious player has to be able to check, and they are the whole of it.
 */
final class MojangSkins {
    /** Mojang's own skin endpoint. Nothing else in the mod may be given the token. */
    static final URI ENDPOINT =
            URI.create("https://api.minecraftservices.com/minecraft/profile/skins");

    /** The two shapes Mojang accepts for the arm width. */
    static final String CLASSIC = "classic";
    static final String SLIM = "slim";

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private static HttpClient http;

    private MojangSkins() {
    }

    /**
     * Sends one skin to the account the running session belongs to.
     *
     * <p>Runs entirely off the caller's thread: {@link HttpClient} takes the request and
     * gives the thread straight back. The caller steps onto the client thread again
     * before it touches anything in the game.
     *
     * @param accessToken the session's token, used here and kept nowhere
     * @param png         a 64x64 PNG, already encoded
     * @param variant     {@link #CLASSIC} or {@link #SLIM}
     */
    static CompletableFuture<Void> upload(String accessToken, byte[] png, String variant) {
        String boundary = "mcsc" + UUID.randomUUID().toString().replace("-", "");
        HttpRequest request = HttpRequest.newBuilder(ENDPOINT)
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("Accept", "application/json")
                .header("User-Agent", userAgent())
                .POST(HttpRequest.BodyPublishers.ofByteArray(multipart(boundary, variant, png)))
                .build();

        // The answer is the whole profile, and none of it is needed: what this call
        // wants to know is whether it was accepted. Discarding it keeps the reply out of
        // memory while leaving the headers, which is where a 429 says how long to wait.
        return client().sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .thenAccept(response -> {
                    if (response.statusCode() / 100 != 2) {
                        throw new CompletionException(new SkinUploadException(
                                response.statusCode(), retryAfter(response)));
                    }
                });
    }

    /**
     * The multipart body Mojang expects: the variant as a plain field, the sheet as a
     * PNG file part.
     *
     * <p>Built by hand because the JDK's HTTP client has no multipart publisher and
     * because the alternative is a dependency embedded in the jar to write forty bytes
     * of boundary. Kept separate from the sending so it can be read back in a test
     * without a request.
     */
    static byte[] multipart(String boundary, String variant, byte[] png) {
        ByteArrayOutputStream body = new ByteArrayOutputStream(png.length + 256);
        ascii(body, "--" + boundary + "\r\n");
        ascii(body, "Content-Disposition: form-data; name=\"variant\"\r\n\r\n");
        ascii(body, variant + "\r\n");
        ascii(body, "--" + boundary + "\r\n");
        ascii(body, "Content-Disposition: form-data; name=\"file\"; filename=\"skin.png\"\r\n");
        ascii(body, "Content-Type: image/png\r\n\r\n");
        body.writeBytes(png);
        ascii(body, "\r\n--" + boundary + "--\r\n");
        return body.toByteArray();
    }

    private static void ascii(ByteArrayOutputStream body, String text) {
        body.writeBytes(text.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * How long a refusal asked to be left alone.
     *
     * <p>Only the delta-seconds form is read. {@code Retry-After} may also carry an HTTP
     * date, and reading one means trusting the machine's clock to agree with Mojang's —
     * an unread header falls back to a fixed wait, whereas a clock an hour out would
     * either lock the button for an hour or not at all.
     */
    private static Duration retryAfter(HttpResponse<Void> response) {
        return response.headers().firstValue("Retry-After")
                .map(String::trim)
                .filter(value -> value.chars().allMatch(Character::isDigit) && !value.isEmpty())
                .map(value -> {
                    try {
                        return Duration.ofSeconds(Long.parseLong(value));
                    } catch (NumberFormatException tooBig) {
                        return null;
                    }
                })
                .orElse(null);
    }

    /**
     * Built on first need and kept: an {@link HttpClient} owns a connection pool and
     * threads, and the mod would otherwise throw both away between two uploads.
     */
    private static synchronized HttpClient client() {
        if (http == null) {
            http = HttpClient.newBuilder()
                    .connectTimeout(CONNECT_TIMEOUT)
                    // Mojang's API does not redirect, and a token must not follow one
                    // somewhere else if it ever starts to.
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .build();
        }
        return http;
    }

    private static String userAgent() {
        return "mcskincreator-mod/" + MCSkinCreatorClient.version();
    }

    /** Mojang's name for the model the sheet was drawn for. */
    static String variant(boolean slim) {
        return slim ? SLIM : CLASSIC;
    }

    /** Only so a test can name the endpoint's host without repeating the literal. */
    static String host() {
        return ENDPOINT.getHost().toLowerCase(Locale.ROOT);
    }
}
