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
import java.util.function.Supplier;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.resources.Identifier;
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

    /**
     * The other override: what is being edited right now, on the person editing it.
     *
     * <p>It exists for the editor's two in-game cameras. Looking at your own character
     * in the world is only worth doing if the character is wearing the thing you are
     * drawing, and nothing else can put it there — the profile the client joined with
     * still describes the old skin.
     *
     * <p>It is deliberately the <em>narrowest</em> override that answers that: one
     * player, this client only, and only while the editor is open. It is not the fitting
     * room of issue #11 and must not grow into it by accident. Nothing is sent anywhere,
     * nobody else sees it, and {@link #stopPreviewing()} takes it off — which the editor
     * calls from its own teardown, so closing the window ends it even if the editor was
     * closed by the game rather than by the player.
     */
    private static final ManagedTexture PREVIEW = new ManagedTexture("editing");

    private static volatile UUID previewId;
    private static volatile PlayerModelType previewModel = PlayerModelType.WIDE;

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

    /**
     * Wraps a skin lookup so it answers the applied skin for {@code id}.
     *
     * <p>The mixin covers what is drawn from a player <em>entity</em>, and the menus
     * have none: the panel on the title screen asks {@code SkinManager} for a lookup of
     * the profile, which resolves the account's skin and so goes on showing the old one
     * until its cache and the profile behind it are rebuilt — that is, until the game
     * restarts. This is the same override, at the other door.
     *
     * <p>Wrapping the supplier rather than the result is what keeps it live:
     * {@code MenuFigure} keeps the supplier and calls it as it draws, on both
     * targets, so a panel built before the upload shows the new skin the moment it
     * lands, without being rebuilt.
     */
    public static Supplier<PlayerSkin> over(UUID id, Supplier<PlayerSkin> lookup) {
        return () -> {
            PlayerSkin resolved = lookup.get();
            PlayerSkin applied = worn(id, resolved);
            return applied == null ? resolved : applied;
        };
    }

    /**
     * Shows {@code sheet} on {@code profileId} for as long as the editor is open.
     *
     * <p>Takes precedence over an applied skin, because an edit in progress is newer
     * than the last upload and is the thing the player is looking at.
     *
     * <p>Must run on the client thread: it uploads a texture.
     */
    public static void preview(UUID profileId, byte[] sheet, PlayerModelType model)
            throws IOException {
        NativeImage image = PreviewSkin.decode(sheet);
        try {
            PREVIEW.upload(image);
        } catch (RuntimeException | Error failure) {
            image.close();
            throw failure;
        }
        AppliedSkin.previewModel = model;
        AppliedSkin.previewId = profileId;
    }

    /** Takes the edit back off. Idempotent, and cheap when nothing was being previewed. */
    public static void stopPreviewing() {
        if (AppliedSkin.previewId == null) {
            return;
        }
        AppliedSkin.previewId = null;
        PREVIEW.close();
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
        UUID editor = previewId;
        if (editor != null && editor.equals(id) && PREVIEW.isUploaded()) {
            return bodyOver(resolved, PREVIEW.id(), previewModel);
        }
        UUID wearer = profileId;
        if (wearer == null || !wearer.equals(id) || !TEXTURE.isUploaded()) {
            return null;
        }
        return bodyOver(resolved, TEXTURE.id(), model);
    }

    /** The game's answer with its body swapped, and its cape and elytra kept. */
    private static PlayerSkin bodyOver(PlayerSkin resolved, Identifier body,
                                       PlayerModelType model) {
        return new PlayerSkin(new RuntimeTexture(body),
                resolved == null ? null : resolved.cape(),
                resolved == null ? null : resolved.elytra(),
                model,
                // Not signed by Mojang: this is the mod's own texture standing in for
                // one, and saying otherwise to the game would be a lie with no upside.
                false);
    }
}
