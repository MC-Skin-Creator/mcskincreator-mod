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

    // Render states arrived in 1.21.2. Before them the figure is posed on the mod's own
    // FigureState, which carries the AvatarRenderState fields the poses write, under the
    // same names - so the code that poses it is the same code, and only the type is
    // swapped. Scoped to the files that ask for it with a `//~ figure` line: FigureState
    // itself must not be renamed on the targets that have the real class.
    replacements.string("figure", current.parsed >= "1.21.2") {
        replace("fr.clixmods.mcsc.mod.scene.FigureState",
                "net.minecraft.client.renderer.entity.state.AvatarRenderState")
        replace("FigureState", "AvatarRenderState")
    }
}
