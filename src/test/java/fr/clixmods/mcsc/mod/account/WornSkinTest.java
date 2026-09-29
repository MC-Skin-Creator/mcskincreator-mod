/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.account;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import org.junit.jupiter.api.Test;

/** Reading which skin an account wears out of its public profile. */
class WornSkinTest {

    private static String profile(String textures) {
        String value = Base64.getEncoder().encodeToString(textures.getBytes(StandardCharsets.UTF_8));
        return "{\"id\":\"069a79f444e94726a5befca90e38aaf5\",\"name\":\"Notch\","
                + "\"properties\":[{\"name\":\"textures\",\"value\":\"" + value + "\"}]}";
    }

    @Test
    void theSkinIsReadOverHttpsFromTheTextureServer() {
        String json = profile("{\"textures\":{\"SKIN\":{\"url\":"
                + "\"http://textures.minecraft.net/texture/2920abc\"}}}");

        assertEquals(Optional.of("https://textures.minecraft.net/texture/2920abc"),
                WornSkin.skinAddress(json));
    }

    @Test
    void aDefaultSkinHasNoAddress() {
        assertEquals(Optional.empty(), WornSkin.skinAddress(profile("{\"textures\":{}}")));
    }

    @Test
    void anAddressElsewhereIsRefused() {
        String json = profile("{\"textures\":{\"SKIN\":{\"url\":\"http://example.com/texture/1\"}}}");

        assertEquals(Optional.empty(), WornSkin.skinAddress(json));
    }

    @Test
    void aProfileThatIsNotOneHasNoAddress() {
        assertEquals(Optional.empty(), WornSkin.skinAddress("not json"));
        assertEquals(Optional.empty(), WornSkin.skinAddress("{\"properties\":[]}"));
    }
}
