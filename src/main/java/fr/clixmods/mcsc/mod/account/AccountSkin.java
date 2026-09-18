/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.account;

import java.util.concurrent.CompletableFuture;

import fr.clixmods.mcsc.mod.skin.FrontSprite;
import fr.clixmods.mcsc.mod.skin.Png;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.PlayerModelType;

/**
 * Applying the composed skin to the player's Minecraft account: the whole of what the
 * rest of the mod may call, and the edge of the only package that ever holds the
 * session token.
 *
 * <p>The distinction this class exists to keep is the one the interface has to make
 * too. Everything else the editor does happens on this machine; an upload changes the
 * account, for everyone, everywhere, and it persists after the game is shut. It is
 * therefore the one action the mod will never take by itself: {@link #apply} runs once
 * per press of a button, and {@link #ready} refuses a second press until Mojang could
 * reasonably have finished with the first.
 *
 * <p>State lives here rather than in the screen because closing the editor must not
 * unlock anything. A cooldown that a player could clear by pressing Escape would be no
 * cooldown at all.
 */
public final class AccountSkin {
    private static final UploadCooldown COOLDOWN = new UploadCooldown();

    /** Set on the client thread, cleared on an HTTP client thread: see below. */
    private static volatile boolean uploading;

    private AccountSkin() {
    }

    /**
     * Whether this session could upload at all — not whether it may right now.
     *
     * <p>This is what decides that the button exists. It does not change while the game
     * runs, so a screen can ask it once when it builds itself.
     */
    public static boolean available(Minecraft client) {
        return GameSession.canUpload(client);
    }

    /** Whether an upload would be sent this instant, rather than refused. */
    public static boolean ready() {
        return !uploading && !COOLDOWN.locked(System.currentTimeMillis());
    }

    /** Whether one is in flight, which reads differently from waiting out a cooldown. */
    public static boolean uploading() {
        return uploading;
    }

    /** Whole seconds before the next upload is allowed, or 0 when one is allowed now. */
    public static long secondsLeft() {
        return COOLDOWN.secondsLeft(System.currentTimeMillis());
    }

    /**
     * Sends {@code sheet} to the account this session belongs to.
     *
     * <p>{@code sheet} is what the composer answered, in either of the two shapes it
     * uses: a PNG goes as it is, and a raw RGBA buffer is encoded first. That encoding
     * happens here, on the caller's thread, so the request itself carries nothing but
     * bytes — and the caller is the client thread, where the work is a 64x64 image and
     * costs nothing worth measuring.
     *
     * <p>The returned future completes on an HTTP client thread, so
     * whoever chains onto it steps back through {@code Minecraft#execute} before
     * touching the game. It fails with a {@link SkinUploadException} when Mojang refused
     * and with whatever the network raised when it never answered.
     */
    public static CompletableFuture<Void> apply(Minecraft client, byte[] sheet,
                                                PlayerModelType model) {
        if (!ready()) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("an upload is already in flight or too recent"));
        }

        byte[] png;
        try {
            png = toPng(sheet);
        } catch (RuntimeException malformed) {
            return CompletableFuture.failedFuture(malformed);
        }

        // Read here and handed straight on: the token is an argument from this line to
        // the request and is held nowhere in between.
        String accessToken = GameSession.accessToken(client);
        uploading = true;
        return MojangSkins.upload(accessToken, png, MojangSkins.variant(model == PlayerModelType.SLIM))
                .whenComplete((nothing, failure) -> {
                    uploading = false;
                    long now = System.currentTimeMillis();
                    if (failure == null) {
                        COOLDOWN.lockAfterUpload(now);
                        return;
                    }
                    // A refusal for going too fast is the one failure that says how long
                    // to wait, and ignoring it is how a session gets limited for longer.
                    SkinUploadException refusal = refusal(failure);
                    if (refusal != null && refusal.reason() == SkinUploadException.Reason.RATE_LIMITED) {
                        COOLDOWN.lockUntilRetry(now, refusal.retryAfter().orElse(null));
                    }
                });
    }

    /** Mojang's refusal inside a failure, or null when it never answered one. */
    public static SkinUploadException refusal(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof SkinUploadException upload) {
                return upload;
            }
        }
        return null;
    }

    /** A sheet as Mojang takes it, encoding the raw shape and leaving a PNG alone. */
    private static byte[] toPng(byte[] sheet) {
        if (Png.isPng(sheet)) {
            return sheet;
        }
        return Png.encode(sheet, FrontSprite.SKIN_SIZE, FrontSprite.SKIN_SIZE);
    }
}
