/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod.mixin;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import fr.clixmods.mcsc.mod.MCSkinCreatorClient;
import fr.clixmods.mcsc.mod.Platform;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
//? if >=1.20.5 {
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.repository.PackSource;
//?}
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Puts the mod's own files - its translations, its icon - in front of the game, on
 * Quilt, and only there.
 *
 * <p>A loader does not do this by itself: on Fabric the Fabric API's resource loader
 * does it, on NeoForge and Forge the loader's own, and on Quilt it was QSL, which
 * stopped following game versions at 1.21.1. Asking a Quilt player to install the
 * Fabric API for three files was the alternative. So this class is listed in
 * {@code mcskincreator.quilt.mixins.json}, which only {@code quilt.mod.json} names.
 *
 * <p>Every reload of the client's resources goes through
 * {@code ReloadableResourceManager#createReload} with the packs to read, under the same
 * signature on every supported version; the mod's files go in right after the game's
 * own, below every resource pack the player picked, which is where the other loaders
 * put a mod's files too. Building the pack is the one versioned line: before 1.20.5 a
 * pack is named by a string, after by a {@code PackLocationInfo}.
 */
@Mixin(ReloadableResourceManager.class)
public abstract class ResourceManagerMixin {
    @Shadow
    @Final
    private PackType type;

    @ModifyVariable(method = "createReload", at = @At("HEAD"), argsOnly = true)
    private List<PackResources> mcskincreator$addOwnFiles(List<PackResources> packs) {
        Optional<Path> root = Platform.unregisteredResources();
        if (this.type != PackType.CLIENT_RESOURCES || root.isEmpty()) {
            return packs;
        }
        //? if >=1.20.5 {
        PackResources own = new PathPackResources(new PackLocationInfo(MCSkinCreatorClient.MOD_ID,
                Component.literal(MCSkinCreatorClient.MOD_ID), PackSource.BUILT_IN, Optional.empty()), root.get());
        //?} else {
        /*PackResources own = new PathPackResources(MCSkinCreatorClient.MOD_ID, root.get(), true);
        *///?}
        // The list the game hands over is immutable.
        List<PackResources> withOwn = new ArrayList<>(packs);
        withOwn.add(Math.min(1, withOwn.size()), own);
        return withOwn;
    }
}
