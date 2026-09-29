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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import fr.clixmods.mcsc.mod.catalog.Catalog;
import fr.clixmods.mcsc.mod.catalog.CatalogCategory;
import fr.clixmods.mcsc.mod.catalog.CatalogText;
import fr.clixmods.mcsc.mod.catalog.ThumbCrop;

/** The transport: gzip, the catalogue's ETag, and what is kept on disk. */
class McscApiCacheTest {
    private static final String CATALOG = "{\"categories\":[{\"id\":\"skin\",\"region\":\"body\","
            + "\"label\":\"Peau\",\"items\":[{\"id\":\"a\",\"name\":\"A\"}]}]}";

    @TempDir
    Path folder;

    private HttpServer server;
    private McscApi api;
    /** What each request to /catalog carried, in order. */
    private final List<String> conditions = new ArrayList<>();
    private final List<String> encodings = new ArrayList<>();
    private volatile Consumer<HttpExchange> handler;

    @BeforeEach
    void start() throws IOException {
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        this.server.createContext("/api/v1/catalog", exchange -> {
            synchronized (this.conditions) {
                this.conditions.add(exchange.getRequestHeaders().getFirst("If-None-Match"));
                this.encodings.add(exchange.getRequestHeaders().getFirst("Accept-Encoding"));
            }
            this.handler.accept(exchange);
            exchange.close();
        });
        this.server.start();
        this.api = new McscApi("http://127.0.0.1:" + this.server.getAddress().getPort() + "/api/v1",
                this.folder);
    }

    @AfterEach
    void stop() {
        this.api.close();
        this.server.stop(0);
    }

    @Test
    void aCompressedAnswerIsDecompressed() throws Exception {
        this.handler = exchange -> reply(exchange, 200, gzip(CATALOG), "gzip", null);

        Catalog catalog = this.api.catalog().get();

        assertEquals("skin", catalog.categories().get(0).id());
        assertEquals("gzip", this.encodings.get(0), "the client has to ask for it");
    }

    @Test
    void anUncompressedAnswerIsReadAsIs() throws Exception {
        this.handler = exchange -> reply(exchange, 200, CATALOG.getBytes(StandardCharsets.UTF_8), null, null);

        assertEquals(1, this.api.catalog().get().categories().size());
    }

    @Test
    void theEtagIsKeptAndSentBackExactlyAsReceived() throws Exception {
        for (String etag : new String[] {"\"0123456789abcdef0123456789abcdef\"",
                "W/\"0123456789abcdef0123456789abcdef\""}) {
            this.conditions.clear();
            this.handler = exchange -> reply(exchange, 200, gzip(CATALOG), "gzip", etag);
            this.api.catalog().get();
            assertNull(this.conditions.get(0));

            this.handler = exchange -> reply(exchange, 304, new byte[0], null, etag);
            this.api.catalog().get();
            assertEquals(etag, this.conditions.get(1));
        }
    }

    @Test
    void aNotModifiedAnswerReusesTheCache() throws Exception {
        this.handler = exchange -> reply(exchange, 200, gzip(CATALOG), "gzip", "\"abc\"");
        this.api.catalog().get();

        this.handler = exchange -> reply(exchange, 304, new byte[0], null, "\"abc\"");
        Catalog catalog = this.api.catalog().get();

        assertEquals("skin", catalog.categories().get(0).id());
    }

    @Test
    void anAnswerWithoutAnEtagClearsTheStoredOne() throws Exception {
        this.handler = exchange -> reply(exchange, 200, gzip(CATALOG), "gzip", "\"abc\"");
        this.api.catalog().get();
        assertTrue(Files.exists(this.folder.resolve("catalog.etag")));

        this.handler = exchange -> reply(exchange, 200, gzip(CATALOG), "gzip", null);
        this.api.catalog().get();
        assertFalse(Files.exists(this.folder.resolve("catalog.etag")));

        this.api.catalog().get();
        assertNull(this.conditions.get(2));
    }

    @Test
    void aDamagedCacheSendsNoCondition() throws Exception {
        Files.writeString(this.folder.resolve("catalog.json"), "{ not a catalogue");
        Files.writeString(this.folder.resolve("catalog.etag"), "\"abc\"");
        this.handler = exchange -> reply(exchange, 200, gzip(CATALOG), "gzip", "\"def\"");

        this.api.catalog().get();

        assertNull(this.conditions.get(0), "a 304 would leave nothing to read");
        assertEquals("\"def\"", Files.readString(this.folder.resolve("catalog.etag")).trim());
    }

    @Test
    void theCacheIsUsedWhenTheServerCannotBeReached() throws Exception {
        this.handler = exchange -> reply(exchange, 200, gzip(CATALOG), "gzip", "\"abc\"");
        this.api.catalog().get();

        this.server.stop(0);

        assertEquals(1, this.api.catalog().get().categories().size());
    }

    @Test
    void anAtlasIsDownloadedOnceAndKeptByHash() throws Exception {
        byte[] atlas = new byte[McscApi.ATLAS_BUFFER_BYTES * 2];
        atlas[0] = 5;
        int[] hits = {0};
        this.server.createContext("/api/v1/atlas/hair/h1", exchange -> {
            hits[0]++;
            reply(exchange, 200, gzip(atlas), "gzip", null);
            exchange.close();
        });
        var category = new CatalogCategory("hair", "head", new CatalogText("hair", "hair", "hair"), false,
                ThumbCrop.ALL, "/api/v1/atlas/hair/h1", List.of());

        assertEquals(2, this.api.atlas(category).get().size());
        assertArrayEquals(new byte[] {5}, new byte[] {this.api.atlas(category).get().get(0)[0]});
        assertEquals(1, hits[0], "the second call comes from the disk");
    }

    // ------------------------------------------------------------------ helpers

    private static void reply(HttpExchange exchange, int status, byte[] body, String encoding, String etag) {
        try {
            if (encoding != null) {
                exchange.getResponseHeaders().set("Content-Encoding", encoding);
            }
            if (etag != null) {
                exchange.getResponseHeaders().set("ETag", etag);
            }
            exchange.sendResponseHeaders(status, status == 304 ? -1 : body.length);
            if (status != 304) {
                exchange.getResponseBody().write(body);
            }
        } catch (IOException cause) {
            throw new IllegalStateException(cause);
        }
    }

    private static byte[] gzip(String text) {
        return gzip(text.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] gzip(byte[] bytes) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(bytes);
        } catch (IOException cause) {
            throw new UncheckedIOException(cause);
        }
        return out.toByteArray();
    }
}
