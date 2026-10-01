/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.skin;

import java.util.UUID;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
//? if >=1.21.9 {
import net.minecraft.core.ClientAsset;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
//?} elif >=1.20.2 {
/*import net.minecraft.client.resources.PlayerSkin;
*///?} else {
/*import java.util.concurrent.atomic.AtomicReference;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
*///?}

/**
 * The one place that knows how the game spells a skin, and turns a {@link SkinLook} into
 * it and back.
 *
 * <p>Three spellings across the supported versions. From 1.21.9 a {@code PlayerSkin} is
 * made of client assets and its model is a {@code PlayerModelType}. From 1.20.2 to 1.21.8
 * it is made of texture locations and its model is a nested {@code PlayerSkin.Model}.
 * 1.20.1 has no skin object at all: a player answers its texture and its model name
 * separately, and the local profile's skin arrives through a callback. Everything else
 * in the mod speaks in {@link SkinLook} and {@link SkinModel}, and none of it is
 * versioned.
 */
public final class GameSkins {
    private GameSkins() {
    }

    /** The skin a figure wears while nothing has been composed yet. */
    public static Identifier defaultTexture() {
        //? if >=1.21.9 {
        return DefaultPlayerSkin.getDefaultSkin().body().texturePath();
        //?} elif >=1.20.2 {
        /*return DefaultPlayerSkin.getDefaultTexture();
        *///?} else {
        /*return DefaultPlayerSkin.getDefaultSkin();
        *///?}
    }

    /** The profile id of whoever is signed in to the game. */
    public static UUID localId(Minecraft client) {
        return client.getUser().getProfileId();
    }

    /**
     * What the signed-in player wears, asked again on every call: the skin is fetched in
     * the background, so the first answers are the default one.
     */
    public static Supplier<SkinLook> localLookup(Minecraft client) {
        //? if >=1.21.9 {
        Supplier<PlayerSkin> lookup = client.getSkinManager().createLookup(client.getGameProfile(), false);
        return () -> look(lookup.get());
        //?} elif >=1.20.2 {
        /*Supplier<PlayerSkin> lookup = client.getSkinManager().lookupInsecure(client.getGameProfile());
        return () -> look(lookup.get());
        *///?} else {
        /*// 1.20.1 hands the skin to a callback once it is downloaded, on the client
        // thread; until then the figure wears the default skin for this profile.
        GameProfile profile = client.getUser().getGameProfile();
        UUID id = profile.getId();
        AtomicReference<SkinLook> look = new AtomicReference<>(new SkinLook(
                DefaultPlayerSkin.getDefaultSkin(id), null, model(DefaultPlayerSkin.getSkinModelName(id))));
        client.getSkinManager().registerSkins(profile, (type, location, texture) -> {
            if (type == MinecraftProfileTexture.Type.SKIN) {
                look.set(new SkinLook(location, null, model(texture.getMetadata("model"))));
            }
        }, false);
        return look::get;
        *///?}
    }

    //? if >=1.21.9 {
    /**
     * The skin the figure's render state carries: the look, as the game's own object.
     *
     * <p>Before render states the figure state carries the {@link SkinLook} itself, so
     * this answers its argument there.
     */
    public static PlayerSkin figureSkin(SkinLook look) {
        return PlayerSkin.insecure(new RuntimeTexture(look.texture()),
                look.cape() == null ? null : new RuntimeTexture(look.cape()), null, type(look.model()));
    }

    /**
     * The skin a player wears once the mod has dressed them: the look's body and model,
     * and the cape and elytra the game had already resolved for them.
     */
    public static PlayerSkin wear(PlayerSkin resolved, SkinLook look) {
        return new PlayerSkin(new RuntimeTexture(look.texture()),
                resolved == null ? null : resolved.cape(),
                resolved == null ? null : resolved.elytra(),
                type(look.model()),
                false);
    }

    private static SkinLook look(PlayerSkin skin) {
        return new SkinLook(skin.body().texturePath(),
                skin.cape() == null ? null : skin.cape().texturePath(),
                skin.model() == PlayerModelType.SLIM ? SkinModel.SLIM : SkinModel.WIDE);
    }

    private static PlayerModelType type(SkinModel model) {
        return model == SkinModel.SLIM ? PlayerModelType.SLIM : PlayerModelType.WIDE;
    }

    // A texture the mod registered itself, handed to the game where it expects one of
    // its own assets. The game's own implementations derive the texture path from the
    // asset id; a runtime texture is registered under its name directly, so the two are
    // the same here.
    private record RuntimeTexture(Identifier id) implements ClientAsset.Texture {
        @Override
        public Identifier texturePath() {
            return this.id;
        }
    }
    //?} elif >=1.20.2 {
    /*// Before render states the figure state carries the look itself.
    public static SkinLook figureSkin(SkinLook look) {
        return look;
    }

    // The skin a player wears once the mod has dressed them: the look's body and model,
    // and the cape and elytra the game had already resolved for them.
    public static PlayerSkin wear(PlayerSkin resolved, SkinLook look) {
        return new PlayerSkin(look.texture(), null,
                resolved == null ? null : resolved.capeTexture(),
                resolved == null ? null : resolved.elytraTexture(),
                look.model() == SkinModel.SLIM ? PlayerSkin.Model.SLIM : PlayerSkin.Model.WIDE,
                false);
    }

    private static SkinLook look(PlayerSkin skin) {
        return new SkinLook(skin.texture(), skin.capeTexture(),
                skin.model() == PlayerSkin.Model.SLIM ? SkinModel.SLIM : SkinModel.WIDE);
    }
    *///?} else {
    /*// Before render states the figure state carries the look itself.
    public static SkinLook figureSkin(SkinLook look) {
        return look;
    }

    // The model name 1.20.1 picks a player renderer by: "slim" or "default".
    public static String modelName(SkinModel model) {
        return model == SkinModel.SLIM ? "slim" : "default";
    }

    private static SkinModel model(String name) {
        return "slim".equals(name) ? SkinModel.SLIM : SkinModel.WIDE;
    }
    *///?}
}
