/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.project;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The library as it is kept on disk. */
class SavedSkinTest {
    @TempDir
    Path folder;

    private static JsonObject data() {
        return JsonParser.parseString("{\"v\": 1, \"slim\": true, \"layers\": []}").getAsJsonObject();
    }

    private static List<String> ids(SkinLibrary library) throws IOException {
        return library.list().stream().map(SavedSkin::id).toList();
    }

    @Test
    void aSavedSkinComesBackAsItWas() throws IOException {
        SkinLibrary library = new SkinLibrary(this.folder.resolve("nested"));
        library.save(new SavedSkin("a1", "Ranger", 1750000000000L, data()));

        List<SavedSkin> skins = library.list();

        assertEquals(1, skins.size());
        assertEquals("Ranger", skins.get(0).name());
        assertEquals(1750000000000L, skins.get(0).at());
        assertTrue(skins.get(0).data().get("slim").getAsBoolean());
    }

    @Test
    void theLastChangedComesFirst() throws IOException {
        SkinLibrary library = new SkinLibrary(this.folder);
        library.save(new SavedSkin("old", "Old", 1, data()));
        library.save(new SavedSkin("new", "New", 2, data()));

        assertEquals(List.of("new", "old"), ids(library));
    }

    @Test
    void savingAgainReplacesTheEntryAndForgetsItsPicture() throws IOException {
        SkinLibrary library = new SkinLibrary(this.folder);
        library.save(new SavedSkin("a1", "Ranger", 1, data()));
        library.savePicture("a1", new byte[] {1});

        library.save(new SavedSkin("a1", "Renamed", 2, data()));

        assertEquals("Renamed", library.list().get(0).name());
        assertEquals(1, library.list().size());
        assertFalse(library.picture("a1").isPresent(), "the picture drawn is of the version before");
    }

    @Test
    void aDamagedFileIsSkippedNotFatal() throws IOException {
        SkinLibrary library = new SkinLibrary(this.folder);
        library.save(new SavedSkin("ok", "Fine", 1, data()));
        Files.writeString(this.folder.resolve("bad.json"), "{not json");
        Files.writeString(this.folder.resolve("empty.json"), "{\"id\": \"empty\", \"name\": \"No project\"}");
        Files.writeString(this.folder.resolve("other.json"),
                "{\"id\": \"someone-else\", \"data\": {\"layers\": []}}");

        assertEquals(List.of("ok"), ids(library));
    }

    @Test
    void aMissingFolderIsAnEmptyLibrary() throws IOException {
        assertTrue(new SkinLibrary(this.folder.resolve("nothing")).list().isEmpty());
    }

    @Test
    void deletingTakesTheSkinAndItsPicture() throws IOException {
        SkinLibrary library = new SkinLibrary(this.folder);
        library.save(new SavedSkin("a1", "Ranger", 1, data()));
        library.savePicture("a1", new byte[] {1, 2, 3});
        assertArrayEquals(new byte[] {1, 2, 3}, library.picture("a1").orElseThrow());

        library.delete("a1");

        assertTrue(library.list().isEmpty());
        assertFalse(library.picture("a1").isPresent());
    }

    @Test
    void anIdentifierCannotLeaveTheFolder() {
        SkinLibrary library = new SkinLibrary(this.folder);
        assertThrows(IllegalArgumentException.class, () -> library.delete("../escape"));
    }

    @Test
    void aDrawnIdentifierIsAFileName() {
        assertTrue(SavedSkin.newId().matches("[A-Za-z0-9_-]{1,64}"));
    }
}
