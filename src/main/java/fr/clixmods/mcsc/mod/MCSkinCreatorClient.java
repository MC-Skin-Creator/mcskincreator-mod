/*
 * MC Skin Creator - Minecraft mod
 * Copyright (c) 2026 clixmods. All rights reserved.
 *
 * Proprietary, source-available. See the LICENSE file at the root of this
 * repository.
 */
package fr.clixmods.mcsc.mod;

import fr.clixmods.mcsc.mod.ui.MenuButtons;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client entry point of the mod.
 *
 * <p>There is deliberately no server entry point: the mod edits a skin and talks
 * to remote services on behalf of the player, none of which belongs on a server.
 */
public final class MCSkinCreatorClient implements ClientModInitializer {
    public static final String MOD_ID = "mcskincreator";

    public static final Logger LOGGER = LoggerFactory.getLogger("MC Skin Creator");

    @Override
    public void onInitializeClient() {
        // Printed on every launch so a player can tell at a glance, from the log
        // alone, whether the mod is actually loaded and which build they run.
        LOGGER.info("MC Skin Creator {} loaded", version());
        MenuButtons.register();
    }

    /** The mod version as declared in {@code fabric.mod.json}. */
    public static String version() {
        return FabricLoader.getInstance()
                .getModContainer(MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }
}
