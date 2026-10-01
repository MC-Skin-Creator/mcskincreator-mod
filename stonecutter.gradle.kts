plugins {
    id("dev.kikugie.stonecutter")
}

// The version `src/` is currently preprocessed for. Kept on 1.21.11, the version the
// mod is being tested against in-game.
stonecutter active "1.21.11"

stonecutter parameters {
    // The loader is the second axis of the tree, next to the game version: a node
    // named "<version>-neoforge" builds for NeoForge, "<version>-quilt" for Quilt,
    // "<version>-forge" for Forge, every other one for Fabric. Sources branch on it with
    // `//? if fabric {`, `//? if quilt {`, `//? if neoforge {` and `//? if forge {`.
    val nodeName = node.metadata.project
    val loader = when {
        nodeName.endsWith("-neoforge") -> "neoforge"
        nodeName.endsWith("-quilt") -> "quilt"
        nodeName.endsWith("-forge") -> "forge"
        else -> "fabric"
    }
    constants.match(loader, "fabric", "quilt", "neoforge", "forge")

    swaps["mod_version"] = "\"${property("mod.version")}\";"
    swaps["minecraft"] = "\"${node.metadata.version}\";"
    node.project.findProperty("deps.fabric_api")?.let { dependencies["fapi"] = it as String }

    // Mojang renamed ResourceLocation to Identifier in 1.21.11 and changed nothing else
    // about it. A pure rename used in a dozen files is a replacement, not a conditional
    // in each of them: the sources say Identifier, and older targets read
    // ResourceLocation. Declared here rather than in a build script so that both
    // loaders' scripts get it from one place.
    replacements.regex(current.parsed >= "1.21.11") {
        replace("\\bResourceLocation\\b", "Identifier", "\\bIdentifier\\b", "ResourceLocation")
    }

    // The figure's render state has had three names, and the files that pose it - the
    // ones with a `//~ figure` line - are written against the newest. From 1.21.9 it is
    // AvatarRenderState; from 1.21.2 to 1.21.8 PlayerRenderState, with the same fields;
    // before 1.21.2 there are no render states, and it is the mod's own FigureState,
    // which carries those fields under the same names. The two replacements chain, so
    // the oldest targets go all the way down. Scoped to the files that ask for it:
    // FigureState itself must not be renamed on the targets that have the real class.
    replacements.string("figure", current.parsed >= "1.21.9") {
        replace("net.minecraft.client.renderer.entity.state.PlayerRenderState",
                "net.minecraft.client.renderer.entity.state.AvatarRenderState")
        replace("PlayerRenderState", "AvatarRenderState")
    }
    replacements.string("figure", current.parsed >= "1.21.2") {
        replace("fr.clixmods.mcsc.mod.scene.FigureState",
                "net.minecraft.client.renderer.entity.state.PlayerRenderState")
        replace("FigureState", "PlayerRenderState")
    }
}
