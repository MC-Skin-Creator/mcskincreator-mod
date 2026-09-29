/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod;

import java.nio.file.Path;

//? if fabric {
import net.fabricmc.loader.api.FabricLoader;
//?} else {
/*import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
*///?}

/**
 * What the mod asks of the loader it runs on, outside of hooking into the game.
 *
 * <p>Fabric and NeoForge answer the same three questions under different names, so
 * the answers are read here and nowhere else: the rest of the mod never names a
 * loader. The menu entry and the entry point are the only other loader-facing code,
 * because they are where each loader hands the mod control.
 */
public final class Platform {
    private Platform() {
    }

    /** The game's config folder, where the mod keeps the project and the saved skins. */
    public static Path configDir() {
        //? if fabric {
        return FabricLoader.getInstance().getConfigDir();
        //?} else {
        /*return FMLPaths.CONFIGDIR.get();
        *///?}
    }

    /** The game's own folder, the one holding {@code mods/} and {@code config/}. */
    public static Path gameDir() {
        //? if fabric {
        return FabricLoader.getInstance().getGameDir();
        //?} else {
        /*return FMLPaths.GAMEDIR.get();
        *///?}
    }

    /** The mod version as the loader read it from the mod's metadata file. */
    public static String modVersion() {
        //? if fabric {
        return FabricLoader.getInstance()
                .getModContainer(MCSkinCreatorClient.MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
        //?} else {
        /*// Null outside a running game - in the tests - where Fabric still answers.
        ModList mods = ModList.get();
        if (mods == null) {
            return "unknown";
        }
        return mods.getModContainerById(MCSkinCreatorClient.MOD_ID)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("unknown");
        *///?}
    }
}
