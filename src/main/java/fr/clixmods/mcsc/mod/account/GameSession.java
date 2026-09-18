/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.account;

import net.minecraft.client.Minecraft;
import net.minecraft.client.User;

/**
 * What the running session is, and — once, at the moment of the call — its token.
 *
 * <p>Kept package-private on purpose. {@link AccountSkin} is the only public way into
 * this package, so the token cannot be read from the interface, from a panel, or from
 * anything that logs: the whole of its life is the argument list of
 * {@link MojangSkins#upload}.
 *
 * <p>There is no {@code User.Type} to ask on the versions this mod supports — it is
 * gone from {@code net.minecraft.client.User}, checked with {@code javap} against both
 * targets — so the session is read off what it carries. An account signed in through
 * Microsoft has an Xbox user id and a token; a session that is not signed in has
 * neither, whichever launcher started it.
 */
final class GameSession {
    private GameSession() {
    }

    /**
     * Whether this session is one Mojang would accept a skin from.
     *
     * <p>Answers false when unsure, because the two mistakes do not cost the same: an
     * apply button hidden from a session that would have worked is a feature someone
     * has to ask about, while one offered to a session with no token is a refusal
     * nobody can act on. It is also why the message shown in its place says the session
     * is not a signed-in one rather than asserting the player is offline.
     */
    static boolean canUpload(Minecraft client) {
        User user = client.getUser();
        if (user == null) {
            return false;
        }
        return !user.getAccessToken().isBlank()
                && user.getXuid().filter(xuid -> !xuid.isBlank()).isPresent();
    }

    /**
     * The session's access token.
     *
     * <p>Never stored, never logged, never given to anything but
     * {@link MojangSkins#upload}. Every caller of this method is in this file's package
     * and there is exactly one.
     */
    static String accessToken(Minecraft client) {
        User user = client.getUser();
        return user == null ? "" : user.getAccessToken();
    }
}
