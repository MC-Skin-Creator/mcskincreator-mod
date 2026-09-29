/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.account;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import fr.clixmods.mcsc.mod.MCSkinCreatorClient;
import fr.clixmods.mcsc.mod.skin.FrontSprite;
import fr.clixmods.mcsc.mod.skin.Png;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.PlayerModelType;

/**
 * Applying the composed skin to the player's Minecraft account: the whole of what the
 * rest of the mod may call, and the edge of the only package that ever holds the
 * session token.
 *
 * <p>An upload changes the account, for everyone, everywhere, and it persists after the
 * game is shut — but it is not something the player should wait on. {@link #apply} only
 * queues the sheet: a background thread sends it once the {@link UploadCooldown} allows,
 * which is at once when nothing was sent lately and after the wait otherwise. The player
 * is never blocked and never told; what they see is the skin the caller puts on them
 * locally.
 *
 * <p>Only the newest queued sheet is ever sent. Applying three times during a cooldown
 * costs one upload of the last one, which is what the account should end up wearing.
 *
 * <p>State lives here rather than in the screen because closing the editor must not
 * cancel anything, and must not clear the cooldown either.
 */
public final class AccountSkin {
    private static final UploadCooldown COOLDOWN = new UploadCooldown();

    private static final ScheduledExecutorService TIMER =
            Executors.newSingleThreadScheduledExecutor(task -> {
                Thread thread = new Thread(task, "mcskincreator-upload");
                thread.setDaemon(true);
                return thread;
            });

    private static final Object LOCK = new Object();

    /** The newest sheet not yet sent. Guarded by {@link #LOCK}. */
    private static Pending pending;

    /** Whether a send is scheduled or in flight, so there is never a second one. Guarded by {@link #LOCK}. */
    private static boolean armed;

    private record Pending(byte[] png, boolean slim) {
    }

    private AccountSkin() {
    }

    /**
     * Whether this session could upload at all.
     *
     * <p>This is what decides that the button exists. It does not change while the game
     * runs, so a screen can ask it once when it builds itself.
     */
    public static boolean available(Minecraft client) {
        return GameSession.canUpload(client);
    }

    /**
     * Queues {@code sheet} for the account this session belongs to, and returns at once.
     *
     * <p>{@code sheet} is what the composer answered, in either of the two shapes it
     * uses: a PNG goes as it is, and a raw RGBA buffer is encoded first, here, on the
     * caller's thread — a 64x64 image, which costs nothing worth measuring.
     *
     * <p>Whether it reaches Mojang is not reported: a refusal or a dead network is
     * logged, and a rate limit is waited out and retried.
     *
     * @throws RuntimeException when {@code sheet} is not an image, before anything is queued
     */
    public static void apply(Minecraft client, byte[] sheet, PlayerModelType model) {
        byte[] png = toPng(sheet);
        synchronized (LOCK) {
            pending = new Pending(png, model == PlayerModelType.SLIM);
            if (armed) {
                return;
            }
            armed = true;
        }
        arm(client);
    }

    /** Schedules the next send for when the cooldown lets it go. */
    private static void arm(Minecraft client) {
        long wait = COOLDOWN.millisLeft(System.currentTimeMillis());
        TIMER.schedule(() -> send(client), wait, TimeUnit.MILLISECONDS);
    }

    private static void send(Minecraft client) {
        Pending next;
        synchronized (LOCK) {
            next = pending;
            pending = null;
            if (next == null) {
                armed = false;
                return;
            }
        }
        try {
            // Read here and handed straight on: the token is an argument from this line to
            // the request and is held nowhere in between.
            String accessToken = GameSession.accessToken(client);
            MojangSkins.upload(accessToken, next.png(), MojangSkins.variant(next.slim()))
                    .whenComplete((nothing, failure) -> finished(client, next, failure));
        } catch (RuntimeException cause) {
            finished(client, next, cause);
        }
    }

    private static void finished(Minecraft client, Pending sent, Throwable failure) {
        long now = System.currentTimeMillis();
        boolean retry = false;
        if (failure == null) {
            COOLDOWN.lockAfterUpload(now);
        } else {
            // A refusal for going too fast is the one failure that says how long to
            // wait, and ignoring it is how a session gets limited for longer. It is also
            // the one worth trying again; the rest would only fail the same way.
            SkinUploadException refusal = refusal(failure);
            if (refusal != null && refusal.reason() == SkinUploadException.Reason.RATE_LIMITED) {
                COOLDOWN.lockUntilRetry(now, refusal.retryAfter().orElse(null));
                retry = true;
            } else {
                // The status and what threw, never the request: the log is the one place
                // the token must not reach.
                MCSkinCreatorClient.LOGGER.warn("Applying the skin to the account failed",
                        failure);
            }
        }
        synchronized (LOCK) {
            if (retry && pending == null) {
                pending = sent;
            }
            if (pending == null) {
                armed = false;
                return;
            }
        }
        arm(client);
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
