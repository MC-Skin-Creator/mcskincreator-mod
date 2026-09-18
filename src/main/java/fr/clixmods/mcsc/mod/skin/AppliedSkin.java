/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.skin;

import java.io.IOException;
import java.util.UUID;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

/**
 * The skin this client wears for the player who just applied one, until the game is
 * next started.
 *
 * <p>Uploading to Mojang changes the account, and the account is not what a running
 * client draws. The profile the client was handed when it joined still carries the old
 * {@code textures} property, and Minecraft's own caches sit on top of that, so the new
 * skin only appears on a restart — a minute in which the upload looks exactly like a
 * failure.
 *
 * <p>This closes that minute without pretending to. It is a local override on the
 * rendering, nothing more: the pixels are the ones that were just sent, so what the
 * player sees is what their account now holds. It is set only once Mojang has accepted
 * the upload, never before — a client wearing a skin that failed to send would be the
 * one lie worse than the wait.
 *
 * <p>It lasts the session. There is no reliable moment at which the real profile can be
 * seen to have caught up — the client is not told, and the property it holds is not
 * refreshed until it reconnects — so rather than guess at one, the override simply
 * stands. It cannot drift: the only thing that replaces it is another upload, which
 * replaces the account skin in the same breath.
 *
 * <p>Other players are a different question, and not one a client-side mod can answer:
 * their clients read the profile from the server. They see the change when the profile
 * CDN hands it out, which is what the interface warns about before the upload.
 */
public final class AppliedSkin {
    /** One texture for the session: applying again replaces its pixels in place. */
    private static final ManagedTexture TEXTURE = new ManagedTexture("applied");

    /** Whose skin this is. Null until something is applied, and the switch for the lot. */
    private static volatile UUID profileId;
    private static volatile PlayerModelType model = PlayerModelType.WIDE;

    private AppliedSkin() {
    }

    /**
     * Puts {@code sheet} on {@code profileId} from now on.
     *
     * <p>{@code sheet} is what went to Mojang, in either of the two shapes the composer
     * answers with. Must run on the client thread: it uploads a texture.
     */
    public static void wear(UUID profileId, byte[] sheet, PlayerModelType model)
            throws IOException {
        NativeImage image = PreviewSkin.decode(sheet);
        try {
            TEXTURE.upload(image);
        } catch (RuntimeException | Error failure) {
            image.close();
            throw failure;
        }
        AppliedSkin.model = model;
        // Set last: it is what makes the override live, and it must not go live over a
        // texture that is not there yet.
        AppliedSkin.profileId = profileId;
    }

    /** Drops the override and the texture with it. Must run on the client thread. */
    public static void forget() {
        profileId = null;
        TEXTURE.close();
    }

    public static boolean isWorn() {
        return profileId != null && TEXTURE.isUploaded();
    }

    /**
     * The skin to draw {@code id} with, or null to leave the game's own answer alone.
     *
     * <p>Called for every player on every frame, so the miss — which is every player on
     * nearly every frame — is a volatile read and a comparison.
     *
     * <p>Only the body is replaced. The cape and the elytra are the ones the game
     * resolved, because they belong to the account and this mod has not touched them;
     * an override that dropped them would take a player's cape off to show them a skin.
     */
    public static PlayerSkin worn(UUID id, PlayerSkin resolved) {
        UUID wearer = profileId;
        if (wearer == null || !wearer.equals(id) || !TEXTURE.isUploaded()) {
            return null;
        }
        return new PlayerSkin(new RuntimeTexture(TEXTURE.id()),
                resolved == null ? null : resolved.cape(),
                resolved == null ? null : resolved.elytra(),
                model,
                // Not signed by Mojang: this is the mod's own texture standing in for
                // one, and saying otherwise to the game would be a lie with no upside.
                false);
    }
}
