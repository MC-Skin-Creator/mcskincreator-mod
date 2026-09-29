/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.scene;

import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;

/**
 * What the figure in the scene is doing.
 *
 * <p>The site had to write its ten animations out by hand — matrices, limb angles, the
 * game's own cycle lengths read off {@code HumanoidModel.setupAnim} and transposed into
 * another coordinate system, plus a test measuring which arm ends up inside the skull.
 * None of that is needed here, and that is the whole point of this class: <strong>the
 * mod is inside the game, so it states the situation and the game animates it.</strong>
 * Walking is not a set of angles, it is a walk speed; crouching is a boolean. Every
 * value below goes into a field the game's own player model already reads.
 *
 * <p>Which is also why the list is shorter than the site's. The site offers a wave and a
 * T-pose; neither exists in the game's player model, and reaching them would mean
 * driving the model's parts directly — the one route that is not the same call on both
 * Minecraft targets. Following the rule the interface is built on, a control whose
 * target is empty is absent rather than dead, so they are not offered.
 */
public enum ScenePose {
    /** Standing. The arms still sway: that is the game's, from {@code ageInTicks}. */
    IDLE("idle"),
    WALK("walk"),
    RUN("run"),
    SNEAK("sneak"),
    SWIM("swim"),
    ATTACK("attack"),
    SIT("sit"),
    SLEEP("sleep"),
    ELYTRA("elytra");

    /**
     * {@code limbSwingAmount}: ground speed times four, capped at one, which is what the
     * game feeds its own walk cycle. It sets the stride and the cadence together, so a
     * pose only chooses a speed — 4.317 blocks a second walking, 5.612 sprinting, 1.31
     * crouched.
     */
    private static final float WALK_SPEED = 0.863F;
    private static final float RUN_SPEED = 1.0F;
    private static final float SNEAK_SPEED = 0.262F;

    /** The game counts in ticks, twenty a second, and every cycle below is in ticks. */
    private static final float TICKS_PER_SECOND = 20.0F;

    /** The game plays a swing in six ticks; six of rest behind it, or the arm never drops. */
    private static final float ATTACK_TICKS = 12.0F;
    private static final float ATTACK_SWING_TICKS = 6.0F;

    /**
     * How wide the figure gets, in blocks: a standing player is a block across with the
     * arms out, one lying down is its own height long.
     */
    private static final float SPREAD_UPRIGHT = 1.2F;
    private static final float SPREAD_LYING = 2.1F;

    private final String id;

    ScenePose(String id) {
        this.id = id;
    }

    public String labelKey() {
        return "pose.mcskincreator." + this.id;
    }

    /**
     * The room this pose needs across the panel, in blocks.
     *
     * <p>A pose that lies down is as long as the figure is tall, and fitting it to the
     * upright width would run its feet off the side — the panel clips, so what runs off
     * is simply gone.
     */
    public float spread() {
        return this == SLEEP || this == SWIM || this == ELYTRA ? SPREAD_LYING : SPREAD_UPRIGHT;
    }

    /**
     * Puts this pose on {@code state}, at {@code seconds} into the animation.
     *
     * <p>{@code seconds} is handed over even at rest: it drives the permanent arm sway,
     * which is the only thing a standing figure does.
     *
     * @param playing false stops the <em>movement</em> and keeps the posture. Stopping
     *                and pausing are not the same thing, and this is stopping — the
     *                site made the same choice, for the same reason: a figure frozen
     *                halfway through a step reads as a bug. But a posture is not a
     *                movement: stopping while sitting has to leave the figure sitting,
     *                not stand it up, which is why the two are set apart below.
     */
    public void apply(AvatarRenderState state, float seconds, boolean playing) {
        state.ageInTicks = seconds * TICKS_PER_SECOND;

        // The posture: what the figure is, true whether or not anything is moving.
        switch (this) {
            case SNEAK -> state.isCrouching = true;
            case SWIM -> {
                state.pose = Pose.SWIMMING;
                state.isVisuallySwimming = true;
                state.isInWater = true;
                state.swimAmount = 1.0F;
            }
            case SIT -> state.isPassenger = true;
            case SLEEP -> {
                state.pose = Pose.SLEEPING;
                // Which way the figure lies. The game reads it to lay them down, and a
                // sleeper without one stays standing with their eyes shut.
                state.bedOrientation = Direction.SOUTH;
            }
            case ELYTRA -> {
                state.pose = Pose.FALL_FLYING;
                state.isFallFlying = true;
                state.fallFlyingTimeInTicks = state.ageInTicks;
            }
            default -> {
                // Standing, walking and swinging are postures the figure already has.
            }
        }

        if (!playing) {
            return;
        }

        // The movement: what the figure is doing, and what stopping takes away.
        switch (this) {
            case WALK -> gait(state, WALK_SPEED, seconds);
            case RUN -> gait(state, RUN_SPEED, seconds);
            case SNEAK -> gait(state, SNEAK_SPEED, seconds);
            case SWIM -> gait(state, RUN_SPEED, seconds);
            case ATTACK -> {
                float tick = seconds * TICKS_PER_SECOND % ATTACK_TICKS;
                state.attackArm = HumanoidArm.RIGHT;
                state.attackTime = Math.min(1.0F, tick / ATTACK_SWING_TICKS);
            }
            default -> {
                // Standing, sitting, lying and gliding have no cycle of their own:
                // ageInTicks above is all the movement they get.
            }
        }
    }

    /**
     * Wipes every field {@link #apply} can set, so a pose is what the figure does rather
     * than what it does <em>on top of</em> whatever it was doing.
     *
     * <p>Only needed where the state was filled in from a live entity — the workshop
     * builds a fresh one, which starts clean. The list is here, beside the one that sets
     * them, because two lists of the same fields in two files drift apart and the way it
     * shows is a character who crouches during a swim.
     */
    public static void reset(AvatarRenderState state) {
        state.walkAnimationSpeed = 0;
        state.walkAnimationPos = 0;
        state.attackTime = 0;
        state.swimAmount = 0;
        state.isCrouching = false;
        state.isVisuallySwimming = false;
        state.isInWater = false;
        state.isPassenger = false;
        state.isFallFlying = false;
        state.fallFlyingTimeInTicks = 0;
        state.bedOrientation = null;
        state.pose = Pose.STANDING;
    }

    /**
     * The walk cycle.
     *
     * <p>{@code walkAnimationPos} is what the game advances by {@code limbSwingAmount}
     * every tick, so the two are not independent: the position has to be integrated from
     * the same speed the amount reports, or the stride and the cadence disagree and the
     * figure skates.
     */
    private static void gait(AvatarRenderState state, float speed, float seconds) {
        state.walkAnimationSpeed = speed;
        state.walkAnimationPos = seconds * TICKS_PER_SECOND * speed;
    }
}
