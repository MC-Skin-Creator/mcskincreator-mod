/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.account;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;

/**
 * Mojang was reached and did not apply the skin.
 *
 * <p>It carries the status code, a reason drawn from it, and — for a refusal that says
 * so — how long to wait. It deliberately carries nothing else. The request that earned
 * it was signed with the player's access token, and a failure ends up in logs and in
 * crash reports, so no header, no body and no request of that exchange is kept here.
 * That is the rule the mod's reputation rests on, and it is why this class looks
 * thinner than it could be.
 */
public class SkinUploadException extends IOException {
    private static final long serialVersionUID = 1L;

    /**
     * What the player can do about it, which is the only part of a refusal they can act
     * on. The status itself is kept for the log.
     */
    public enum Reason {
        /** 401 or 403: Mojang no longer accepts this session. Restarting the game renews it. */
        SESSION_EXPIRED,
        /** 429: too many skin changes, too close together. Waiting is the whole remedy. */
        RATE_LIMITED,
        /** Anything else Mojang answered, including a payload it would not take. */
        REFUSED
    }

    private final int status;
    private final Duration retryAfter;

    SkinUploadException(int status, Duration retryAfter) {
        super("Mojang answered HTTP " + status + " on the skin upload");
        this.status = status;
        this.retryAfter = retryAfter;
    }

    public int status() {
        return this.status;
    }

    public Reason reason() {
        return switch (this.status) {
            case 401, 403 -> Reason.SESSION_EXPIRED;
            case 429 -> Reason.RATE_LIMITED;
            default -> Reason.REFUSED;
        };
    }

    /** How long Mojang asked to be left alone, when it said so at all. */
    public Optional<Duration> retryAfter() {
        return Optional.ofNullable(this.retryAfter);
    }
}
