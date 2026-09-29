/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.account;

import java.time.Duration;

/**
 * How long the mod holds back the next upload after it has spoken to Mojang.
 *
 * <p>Mojang limits how often a profile's skin may change, and the fast way to get a
 * session rate-limited is to send changes as fast as they are made. Uploads that come
 * too soon are queued behind this wait rather than refused.
 *
 * <p>The floor after a success is the mod's own and is deliberately generous; it is not
 * a transcription of a published limit, because Mojang publishes none. What Mojang does
 * say, it says on a 429, and {@link #lockUntilRetry} takes it at its word: a
 * {@code Retry-After} is honoured as sent, and its absence falls back to a wait long
 * enough to be worth calling one.
 *
 * <p>All of it is plain arithmetic over a clock passed in, so the waits are pinned by
 * tests rather than by sitting in front of the game for a minute.
 */
final class UploadCooldown {
    /** The mod's own floor between two uploads. */
    static final Duration AFTER_UPLOAD = Duration.ofSeconds(60);

    /** What a 429 with no {@code Retry-After} costs. */
    static final Duration AFTER_RATE_LIMIT = Duration.ofMinutes(10);

    /** Long enough to be a mistake rather than a limit, and so not worth honouring. */
    static final Duration LONGEST_RETRY_AFTER = Duration.ofHours(1);

    /**
     * Volatile because it is written where the upload finishes — an HTTP client thread —
     * and read where the button is drawn, which is the client thread. Without it the
     * button could go on offering an upload the lock has already refused.
     */
    private volatile long lockedUntil;

    /** Locks for the mod's own floor: an upload has just been accepted. */
    void lockAfterUpload(long now) {
        lockUntil(now + AFTER_UPLOAD.toMillis());
    }

    /**
     * Locks for as long as Mojang asked, or for {@link #AFTER_RATE_LIMIT} when it did
     * not say. An absurd delay is treated as no answer rather than as an hours-long
     * lock the player cannot explain.
     */
    void lockUntilRetry(long now, Duration retryAfter) {
        Duration wait = retryAfter == null
                || retryAfter.isNegative()
                || retryAfter.isZero()
                || retryAfter.compareTo(LONGEST_RETRY_AFTER) > 0
                ? AFTER_RATE_LIMIT
                : retryAfter;
        lockUntil(now + wait.toMillis());
    }

    /** Never shortens a lock already running: two refusals in a row take the longer wait. */
    private void lockUntil(long deadline) {
        this.lockedUntil = Math.max(this.lockedUntil, deadline);
    }

    boolean locked(long now) {
        return now < this.lockedUntil;
    }

    /** Milliseconds until the lock ends, or 0 when there is none. */
    long millisLeft(long now) {
        return Math.max(0, this.lockedUntil - now);
    }

    /**
     * Whole seconds left, rounded up so the last fraction of a second still reads as
     * "1" rather than as "0" on a button that is still refusing to work.
     */
    long secondsLeft(long now) {
        long remaining = this.lockedUntil - now;
        return remaining <= 0 ? 0 : (remaining + 999) / 1000;
    }

    /** Only for the tests and for starting over; nothing in the game unlocks by hand. */
    void clear() {
        this.lockedUntil = 0;
    }
}
