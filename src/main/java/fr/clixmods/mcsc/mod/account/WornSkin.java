/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.account;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import fr.clixmods.mcsc.mod.MCSkinCreatorClient;

/**
 * The skin an account is wearing right now, as anybody can read it.
 *
 * <p>This is the public profile every client reads to draw another player: no token, no
 * header, nothing of the session. It is asked of Mojang rather than of the running game
 * because the game's idea of the account is the profile it was handed when it started,
 * and the question here is precisely whether the account has moved since.
 *
 * <p>Two requests, and the second one is skipped when it can be: the profile names the
 * texture by an address that carries the hash of its pixels, so an address already seen
 * is a skin already known.
 */
public final class WornSkin {
    private static final String PROFILE = "https://sessionserver.mojang.com/session/minecraft/profile/";
    private static final String TEXTURE_HOST = "textures.minecraft.net";

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

    private static HttpClient http;

    private WornSkin() {
    }

    /**
     * The address of the skin {@code profileId} wears.
     *
     * @return empty for an account wearing one of the game's default skins, and for a
     *         profile Mojang does not know — an offline development account
     */
    public static CompletableFuture<Optional<String>> address(UUID profileId) {
        String path = PROFILE + profileId.toString().replace("-", "");
        return client().sendAsync(get(path), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(response -> {
                    if (response.statusCode() == 204 || response.statusCode() == 404) {
                        return Optional.<String>empty();
                    }
                    if (response.statusCode() / 100 != 2) {
                        throw new CompletionException(new IllegalStateException(
                                "the session server answered HTTP " + response.statusCode()));
                    }
                    return skinAddress(response.body());
                });
    }

    /** The skin's pixels, as the texture server keeps them. */
    public static CompletableFuture<byte[]> download(String address) {
        return client().sendAsync(get(address), HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(response -> {
                    if (response.statusCode() / 100 != 2) {
                        throw new CompletionException(new IllegalStateException(
                                "the texture server answered HTTP " + response.statusCode()));
                    }
                    return response.body();
                });
    }

    /**
     * Reads the skin's address out of a profile.
     *
     * <p>The {@code textures} property is itself base64-encoded JSON, and names the skin
     * over plain HTTP; it is read back over HTTPS, which the texture server answers
     * too. An address on any other host is refused: this reads pixels, and only from
     * where Mojang keeps them.
     */
    static Optional<String> skinAddress(String profileJson) {
        try {
            JsonObject profile = JsonParser.parseString(profileJson).getAsJsonObject();
            JsonElement properties = profile.get("properties");
            if (properties == null || !properties.isJsonArray()) {
                return Optional.empty();
            }
            for (JsonElement property : properties.getAsJsonArray()) {
                if (!property.isJsonObject()
                        || !"textures".equals(string(property.getAsJsonObject(), "name"))) {
                    continue;
                }
                String decoded = new String(Base64.getDecoder().decode(
                        string(property.getAsJsonObject(), "value")), StandardCharsets.UTF_8);
                JsonObject textures = JsonParser.parseString(decoded).getAsJsonObject()
                        .getAsJsonObject("textures");
                JsonObject skin = textures == null ? null : textures.getAsJsonObject("SKIN");
                return skin == null ? Optional.empty() : secure(string(skin, "url"));
            }
            return Optional.empty();
        } catch (JsonParseException | IllegalStateException | IllegalArgumentException | ClassCastException malformed) {
            MCSkinCreatorClient.LOGGER.warn("Unreadable profile from the session server", malformed);
            return Optional.empty();
        }
    }

    private static Optional<String> secure(String address) {
        try {
            URI uri = URI.create(address);
            if (!TEXTURE_HOST.equalsIgnoreCase(uri.getHost())) {
                return Optional.empty();
            }
            return Optional.of("https://" + TEXTURE_HOST + uri.getRawPath());
        } catch (IllegalArgumentException malformed) {
            return Optional.empty();
        }
    }

    private static String string(JsonObject json, String key) {
        JsonElement value = json.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : "";
    }

    private static HttpRequest get(String address) {
        return HttpRequest.newBuilder(URI.create(address))
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", "mcskincreator-mod/" + MCSkinCreatorClient.version())
                .GET()
                .build();
    }

    private static synchronized HttpClient client() {
        if (http == null) {
            http = HttpClient.newBuilder()
                    .connectTimeout(CONNECT_TIMEOUT)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
        }
        return http;
    }
}
