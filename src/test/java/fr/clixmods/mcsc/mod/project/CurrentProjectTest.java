/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;


/** The project in progress, as it is written down between two openings of the editor. */
class CurrentProjectTest {
    @TempDir
    Path folder;

    private static final SavedSkin PROJECT = new SavedSkin("abc-1", "Knight", 1234L,
            JsonParser.parseString("{\"v\":1,\"slim\":false,\"layers\":[{\"kind\":\"preset\","
                    + "\"cat\":\"skin\",\"preset\":\"skin-uni\"}]}").getAsJsonObject());

    private static CurrentProject.State state(String fingerprint) {
        return new CurrentProject.State(PROJECT, new CurrentProject.Account(fingerprint, "https://x"));
    }

    @Test
    void whatIsWrittenIsWhatIsReadBack() {
        Path file = this.folder.resolve("config").resolve(CurrentProject.FILE_NAME);
        CurrentProject.State written = state("f00d");

        CurrentProject.write(file, written);

        assertEquals(written, CurrentProject.read(file));
    }

    @Test
    void noFileIsNoProject() {
        assertNull(CurrentProject.read(this.folder.resolve(CurrentProject.FILE_NAME)));
    }

    @Test
    void aDamagedFileIsNoProjectRatherThanAFailure() throws IOException {
        Path file = this.folder.resolve(CurrentProject.FILE_NAME);
        Files.writeString(file, "{ not quite");

        assertNull(CurrentProject.read(file));
    }

    @Test
    void theFirstOpeningHasNoProjectToResume() {
        assertEquals(CurrentProject.Start.FIRST, CurrentProject.decide(null, "f00d"));
        assertEquals(CurrentProject.Start.FIRST, CurrentProject.decide(
                new CurrentProject.State(null, new CurrentProject.Account("f00d", "")), "f00d"));
    }

    @Test
    void theSameSkinOnTheAccountResumesTheProject() {
        assertEquals(CurrentProject.Start.RESUME, CurrentProject.decide(state("f00d"), "f00d"));
    }

    @Test
    void anotherSkinOnTheAccountIsAChange() {
        assertEquals(CurrentProject.Start.CHANGED, CurrentProject.decide(state("f00d"), "beef"));
    }

    @Test
    void anAccountThatCannotBeReadIsNeverTakenForAChange() {
        assertEquals(CurrentProject.Start.RESUME, CurrentProject.decide(state("f00d"), null));
        assertEquals(CurrentProject.Start.RESUME, CurrentProject.decide(
                new CurrentProject.State(PROJECT, null), "beef"));
    }
}
