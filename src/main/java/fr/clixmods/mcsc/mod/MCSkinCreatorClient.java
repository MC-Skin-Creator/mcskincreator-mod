/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod;

import fr.clixmods.mcsc.mod.ui.MenuButtons;
//? if fabric {
import net.fabricmc.api.ClientModInitializer;
//?} else {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
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
 * {@code onInitializeClient}; NeoForge finds it by its annotation and calls the
 * constructor, on the client only. Both end in the same {@code start}.
 */
//? if fabric {
public final class MCSkinCreatorClient implements ClientModInitializer {
//?} else {
/*@Mod(value = MCSkinCreatorClient.MOD_ID, dist = Dist.CLIENT)
public final class MCSkinCreatorClient {
*///?}
    public static final String MOD_ID = "mcskincreator";

    public static final Logger LOGGER = LoggerFactory.getLogger("MC Skin Creator");

    //? if fabric {
    @Override
    public void onInitializeClient() {
        start();
    }
    //?} else {
    /*public MCSkinCreatorClient() {
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
