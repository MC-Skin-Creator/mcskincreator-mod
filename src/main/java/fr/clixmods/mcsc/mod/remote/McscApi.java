/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.remote;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.zip.GZIPInputStream;

import fr.clixmods.mcsc.mod.MCSkinCreatorClient;
import fr.clixmods.mcsc.mod.catalog.Catalog;
import fr.clixmods.mcsc.mod.catalog.CatalogCategory;
import fr.clixmods.mcsc.mod.catalog.CatalogParser;

/**
 * The mod's side of the MC Skin Creator HTTP API.
 *
 * <p>Every call is asynchronous and every one of them runs off the render thread:
 * {@link HttpClient} never touches the caller's thread once the request is handed
 * over. Callers are responsible for stepping back onto the client thread before they
 * touch the game, which they do through {@code Minecraft#execute}. A synchronous call
 * from the render thread would freeze the game for as long as the server takes, and
 * that is the first thing that goes wrong in a mod that talks to an API.
 *
 * <p>Only routes of the {@code /api/v1} contract are here, and only the ones that
 * read or render: nothing the player makes is stored on the server, the saved skins
 * live on this machine. Nothing that is not in the contract either: the editor's autosave, the PNG import, the share image and the random draws
 * answer under {@code /api} only, which follows the site's jar and promises nothing to
 * a mod installed months ago. What the contract holds is described in
 * {@code site/API.md} of the site's repository.
 *
 * <p>Nothing here caches. Keeping a downloaded atlas on disk is issue #3's job, and
 * this class is the seam it plugs into.
 */
public final class McscApi implements AutoCloseable {
    /**
     * Where the API lives.
     *
     * <p>Overridable so a contributor can point the mod at a local back-end without
     * rebuilding, and so the value is in one place when the deployment changes:
     *
     * <pre>-Dmcskincreator.api=http://localhost:3000/api/v1</pre>
     */
    public static final String BASE_URL_PROPERTY = "mcskincreator.api";
    public static final String BASE_URL_ENV = "MCSKINCREATOR_API";
    static final String DEFAULT_BASE_URL = "https://mcskincreator.app/api/v1";

    /** One element's buffer inside an atlas: 64 x 64 pixels, four bytes each. */
    public static final int ATLAS_BUFFER_BYTES = 64 * 64 * 4;

    /** What the server accepts as a thumbnail magnification, and clamps to anyway. */
    public static final int MIN_SCALE = 1;
    public static final int MAX_SCALE = 16;

    private static final String JSON = "application/json";
    private static final String IMAGE = "application/octet-stream, image/png";

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final byte[] GZIP_MAGIC = {(byte) 0x1F, (byte) 0x8B};

    private static McscApi shared;

    private final HttpClient http;
    private final String baseUrl;

    public McscApi(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        // HttpClient does not follow redirects unless told to, and a deployment may
        // well answer one - a bare host sent to www, or a path normalised. Left alone,
        // that arrives as a bare 301 that looks like the server refusing.
        this.http = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * The client the screens use, created on first need and kept for the rest of the
     * session: an {@link HttpClient} owns a connection pool and threads, and building
     * one per screen opening would throw both away every time.
     */
    public static synchronized McscApi shared() {
        if (shared == null) {
            shared = new McscApi(configuredBaseUrl());
            MCSkinCreatorClient.LOGGER.info("MC Skin Creator API at {}", shared.baseUrl);
        }
        return shared;
    }

    /** Where this client is pointed, so a failure can say which address it tried. */
    public String baseUrl() {
        return this.baseUrl;
    }

    static String configuredBaseUrl() {
        String property = System.getProperty(BASE_URL_PROPERTY);
        if (property != null && !property.isBlank()) {
            return property.trim();
        }
        String environment = System.getenv(BASE_URL_ENV);
        if (environment != null && !environment.isBlank()) {
            return environment.trim();
        }
        return DEFAULT_BASE_URL;
    }

    /** {@code GET /catalog} */
    public CompletableFuture<Catalog> catalog() {
        return getJson("/catalog").thenApply(json -> unchecked(() -> CatalogParser.parse(json)));
    }

    /**
     * {@code GET /search}: what matches, in the three languages the catalogue carries.
     *
     * <p>Worth a round trip rather than filtering what is already in memory, because
     * the loaded names are only the ones the player's language shows: a French label
     * matches here and cannot match there.
     */
    public CompletableFuture<SearchResults> search(String query, int page, int size) {
        String path = "/search?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
                + "&page=" + Math.max(0, page) + "&size=" + Math.max(1, size);
        return getJson(path).thenApply(json -> unchecked(() -> SearchResults.parse(json)));
    }

    /** {@code GET /presets/{cat}/{id}/credit}: where an element came from. */
    public CompletableFuture<ItemCredit> credit(String categoryId, String itemId) {
        String path = "/presets/" + segment(categoryId) + "/" + segment(itemId) + "/credit";
        return getJson(path).thenApply(json -> unchecked(() -> ItemCredit.parse(json)));
    }

    /**
     * The category's atlas, sliced into one buffer per element.
     *
     * <p>The answer is raw RGBA, which is already the layout {@code NativeImage}
     * wants, so there is no image to decode: cut it every
     * {@value #ATLAS_BUFFER_BYTES} bytes. It arrives gzipped or not depending on the
     * deployment, and {@link #gunzip} takes either.
     *
     * <p>The address is the one the catalogue gave for this category, not one built
     * here from a hash: where the atlases live is the server's to decide, and the
     * catalogue already says. It is relative to the site root, so it resolves against
     * the origin of the base URL rather than against the API path.
     */
    public CompletableFuture<List<byte[]>> atlas(CatalogCategory category) {
        String path = category.atlasPath();
        if (path.isBlank()) {
            return CompletableFuture.failedFuture(
                    new IOException("the catalogue gives no atlas address for " + category.id()));
        }

        URI target = this.origin().resolve(path);
        HttpRequest request = HttpRequest.newBuilder(target)
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", userAgent())
                .header("Accept", "application/octet-stream")
                .GET()
                .build();
        return send(request, target.toString())
                .thenApply(bytes -> unchecked(() -> slice(gunzip(bytes), target.toString())));
    }

    /** The scheme and host the API lives on, which the atlas addresses hang off. */
    private URI origin() {
        return URI.create(this.baseUrl).resolve("/");
    }

    /**
     * {@code POST /textures}: the server composes the project and answers the skin.
     *
     * <p>One round trip per change, which is why callers debounce. Issue #8 replaces
     * this with local composition through {@code mcsc-engine}.
     */
    public CompletableFuture<byte[]> compose(String projectJson) {
        return post("/textures", projectJson);
    }

    /**
     * {@code POST /thumbnails}: the same project seen from the front, standing, at
     * {@code scale} times its 16 by 32 pixels.
     *
     * <p>This is a picture of the character rather than of the sheet, which is what one
     * wants of an export meant to be looked at instead of installed.
     */
    public CompletableFuture<byte[]> frontView(String projectJson, int scale) {
        return post("/thumbnails?x=" + clampScale(scale), projectJson);
    }

    // ------------------------------------------------------------------ plumbing

    private CompletableFuture<byte[]> post(String path, String body) {
        HttpRequest request = request(path)
                .header("Content-Type", JSON)
                .header("Accept", IMAGE)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        return send(request, path);
    }

    private CompletableFuture<String> getJson(String path) {
        HttpRequest request = request(path).header("Accept", JSON).GET().build();
        return send(request, path).thenApply(McscApi::text);
    }

    private static String text(byte[] body) {
        return new String(body, StandardCharsets.UTF_8);
    }

    static int clampScale(int scale) {
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));
    }

    /**
     * A value going into a path.
     *
     * <p>Catalogue identifiers are lowercase words and dashes, so nothing here has ever
     * needed escaping - which is exactly why it is done anyway, before the first
     * identifier that does arrives as a broken address or as something worse.
     */
    static String segment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private CompletableFuture<byte[]> send(HttpRequest request, String path) {
        return this.http.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(response -> {
                    if (response.statusCode() / 100 != 2) {
                        throw new java.util.concurrent.CompletionException(
                                new ApiException(response.statusCode(), path));
                    }
                    return response.body();
                });
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create(this.baseUrl + path))
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", userAgent());
    }

    private static String userAgent() {
        return "mcskincreator-mod/" + MCSkinCreatorClient.version();
    }

    static List<byte[]> slice(byte[] atlas, String path) throws IOException {
        if (atlas.length == 0 || atlas.length % ATLAS_BUFFER_BYTES != 0) {
            throw new IOException(path + ": " + atlas.length + " bytes is not a whole number of "
                    + ATLAS_BUFFER_BYTES + "-byte buffers");
        }

        List<byte[]> buffers = new ArrayList<>(atlas.length / ATLAS_BUFFER_BYTES);
        for (int offset = 0; offset < atlas.length; offset += ATLAS_BUFFER_BYTES) {
            buffers.add(Arrays.copyOfRange(atlas, offset, offset + ATLAS_BUFFER_BYTES));
        }
        return List.copyOf(buffers);
    }

    /**
     * Decompresses an atlas, and passes it through untouched when it arrives already
     * decompressed - which it does whenever something between the mod and the server
     * honours {@code Content-Encoding} on its own.
     */
    static byte[] gunzip(byte[] body) throws IOException {
        if (body.length < GZIP_MAGIC.length
                || body[0] != GZIP_MAGIC[0] || body[1] != GZIP_MAGIC[1]) {
            return body;
        }
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(body))) {
            return gzip.readAllBytes();
        }
    }

    /**
     * Lets the checked failures of parsing travel through the {@link CompletableFuture}
     * chain, where they come back out as the cause of the completion failure.
     */
    private static <T> T unchecked(IoSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (IOException cause) {
            throw new UncheckedIOException(cause);
        }
    }

    @Override
    public void close() {
        this.http.close();
    }

    private interface IoSupplier<T> {
        T get() throws IOException;
    }
}
