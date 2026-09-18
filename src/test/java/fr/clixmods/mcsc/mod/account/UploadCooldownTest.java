/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

/**
 * The wait between two uploads, pinned here rather than in front of the game.
 *
 * <p>The clock is a parameter precisely so these can be written: every one of them is a
 * minute or ten of waiting that nobody would sit through twice.
 */
class UploadCooldownTest {
    private static final long NOW = 1_000_000L;

    private final UploadCooldown cooldown = new UploadCooldown();

    @Test
    void nothingIsLockedBeforeAnUpload() {
        assertFalse(this.cooldown.locked(NOW));
        assertEquals(0, this.cooldown.secondsLeft(NOW));
    }

    @Test
    void anUploadLocksForTheModsOwnFloor() {
        this.cooldown.lockAfterUpload(NOW);

        assertTrue(this.cooldown.locked(NOW));
        assertEquals(UploadCooldown.AFTER_UPLOAD.toSeconds(), this.cooldown.secondsLeft(NOW));
    }

    @Test
    void theLockEndsExactlyWhenTheWaitDoes() {
        this.cooldown.lockAfterUpload(NOW);
        long deadline = NOW + UploadCooldown.AFTER_UPLOAD.toMillis();

        assertTrue(this.cooldown.locked(deadline - 1));
        assertFalse(this.cooldown.locked(deadline));
        assertEquals(0, this.cooldown.secondsLeft(deadline));
    }

    @Test
    void theLastFractionOfASecondStillReadsAsOne() {
        this.cooldown.lockUntilRetry(NOW, Duration.ofMillis(1));

        // Rounded up: a button refusing to work must not be counting down from zero.
        assertEquals(1, this.cooldown.secondsLeft(NOW));
    }

    @Test
    void aRefusalIsTakenAtItsWord() {
        this.cooldown.lockUntilRetry(NOW, Duration.ofSeconds(90));

        assertEquals(90, this.cooldown.secondsLeft(NOW));
    }

    @Test
    void aRefusalThatSaysNothingFallsBackToAFixedWait() {
        this.cooldown.lockUntilRetry(NOW, null);

        assertEquals(UploadCooldown.AFTER_RATE_LIMIT.toSeconds(), this.cooldown.secondsLeft(NOW));
    }

    @Test
    void anAbsurdDelayIsTreatedAsNoAnswer() {
        // A header this far out is a mistake or a clock, not a limit, and honouring it
        // would lock the button for a day with nothing on screen to explain why.
        this.cooldown.lockUntilRetry(NOW, Duration.ofDays(1));

        assertEquals(UploadCooldown.AFTER_RATE_LIMIT.toSeconds(), this.cooldown.secondsLeft(NOW));
    }

    @Test
    void aZeroOrNegativeDelayIsTreatedAsNoAnswerToo() {
        this.cooldown.lockUntilRetry(NOW, Duration.ZERO);
        assertEquals(UploadCooldown.AFTER_RATE_LIMIT.toSeconds(), this.cooldown.secondsLeft(NOW));

        this.cooldown.clear();
        this.cooldown.lockUntilRetry(NOW, Duration.ofSeconds(-5));
        assertEquals(UploadCooldown.AFTER_RATE_LIMIT.toSeconds(), this.cooldown.secondsLeft(NOW));
    }

    @Test
    void aShorterWaitNeverCutsALongerOneShort() {
        this.cooldown.lockUntilRetry(NOW, Duration.ofSeconds(600));
        this.cooldown.lockAfterUpload(NOW);

        assertEquals(600, this.cooldown.secondsLeft(NOW));
    }

    @Test
    void aLongerWaitReplacesAShorterOne() {
        this.cooldown.lockAfterUpload(NOW);
        this.cooldown.lockUntilRetry(NOW, Duration.ofSeconds(600));

        assertEquals(600, this.cooldown.secondsLeft(NOW));
    }
}
