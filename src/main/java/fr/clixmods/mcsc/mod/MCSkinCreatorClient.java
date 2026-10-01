/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod;

import fr.clixmods.mcsc.mod.ui.MenuButtons;
//? if fabric || quilt {
import net.fabricmc.api.ClientModInitializer;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
*///?} else {
/*import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
*///?}
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client entry point of the mod.
 *
 * <p>There is deliberately no server entry point: the mod edits a skin and talks
 * to remote services on behalf of the player, none of which belongs on a server.
 *
 * <p>Fabric finds it through {@code fabric.mod.json} and calls
 * {@code onInitializeClient}; Quilt does the same through {@code quilt.mod.json} - the
 * {@code client} entrypoint and its interface are Quilt Loader's own, shipped inside it
 * since QSL stopped following game versions. NeoForge finds it by its annotation and
 * calls the constructor, on the client only; Forge does the same, and keeps it off a
 * server through {@code clientSideOnly} in {@code mods.toml}, its annotation having no
 * side. All four end in the same {@code start}.
 */
//? if fabric || quilt {
public final class MCSkinCreatorClient implements ClientModInitializer {
//?} elif neoforge {
/*@Mod(value = MCSkinCreatorClient.MOD_ID, dist = Dist.CLIENT)
public final class MCSkinCreatorClient {
*///?} else {
/*@Mod(MCSkinCreatorClient.MOD_ID)
public final class MCSkinCreatorClient {
*///?}
    public static final String MOD_ID = "mcskincreator";

    public static final Logger LOGGER = LoggerFactory.getLogger("MC Skin Creator");

    //? if fabric || quilt {
    @Override
    public void onInitializeClient() {
        start();
    }
    //?} elif neoforge {
    /*public MCSkinCreatorClient() {
        start();
    }
    *///?} else {
    /*// The context is unused, but it is the constructor Forge documents and looks for.
    public MCSkinCreatorClient(FMLJavaModLoadingContext context) {
        start();
    }
    *///?}

    private static void start() {
        // Printed on every launch so a player can tell at a glance, from the log
        // alone, whether the mod is actually loaded and which build they run.
        LOGGER.info("MC Skin Creator {} loaded", version());
        MenuButtons.register();
    }

    /** The mod version as declared in the loader's metadata file. */
    public static String version() {
        return Platform.modVersion();
    }
}
