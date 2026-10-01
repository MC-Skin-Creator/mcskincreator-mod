/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.scene;

import fr.clixmods.mcsc.mod.skin.SkinLook;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;

/**
 * The figure's render state on the targets that have none: 1.20.1 to 1.21.1.
 *
 * <p>Render states arrived in 1.21.2. Before them the game draws a player straight from
 * the entity, and the editor's figure has no entity - it opens from the title screen,
 * where there is no world to have one in. So the pose is written here instead, into
 * fields named exactly as {@code AvatarRenderState} names them, and the files that pose
 * the figure carry a {@code figure} replacement marker: Stonecutter swaps the one type
 * for the other in them, so {@link ScenePose}, {@link WorldPose} and
 * {@link PosedPlayer} are the same code on every target. {@code ModelFigure} then reads
 * this to bend the player model and turn it, the way the game's own renderer reads its
 * state from 1.21.2 on.
 *
 * <p>Only the fields the poses set are here. A field the poses come to need is added to
 * both classes under the same name, or the older targets stop compiling - which is the
 * point of naming them the same.
 */
public final class FigureState {
    /** What the figure wears; the game's state carries its own skin object here. */
    public SkinLook skin;

    public float boundingBoxWidth;
    public float boundingBoxHeight;
    public float ageInTicks;

    /** The body's turn, in degrees; 180 faces the viewer. */
    public float bodyRot;
    /** The head's turn and nod, in degrees, relative to the body. */
    public float yRot;
    public float xRot;

    public Pose pose = Pose.STANDING;
    public HumanoidArm mainArm = HumanoidArm.RIGHT;
    public HumanoidArm attackArm = HumanoidArm.RIGHT;
    public float attackTime;

    public float walkAnimationPos;
    public float walkAnimationSpeed;

    public boolean isCrouching;
    public boolean isPassenger;
    public boolean isVisuallySwimming;
    public boolean isInWater;
    public float swimAmount;
    public boolean isFallFlying;
    public float fallFlyingTimeInTicks;
    public Direction bedOrientation;

    public boolean hasPose(Pose pose) {
        return this.pose == pose;
    }
}
