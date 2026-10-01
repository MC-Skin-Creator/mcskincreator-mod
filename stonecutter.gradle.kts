plugins {
    id("dev.kikugie.stonecutter")
}

// The version `src/` is currently preprocessed for. Kept on 1.21.11, the version the
// mod is being tested against in-game.
stonecutter active "1.21.11"

stonecutter parameters {
    // The loader is the second axis of the tree, next to the game version: a node
    // named "<version>-neoforge" builds for NeoForge, "<version>-forge" for Forge, every
    // other one for Fabric. Sources branch on it with `//? if fabric {`,
    // `//?} elif neoforge {` and `//?} else {` for Forge.
    val nodeName = node.metadata.project
    val loader = when {
        nodeName.endsWith("-neoforge") -> "neoforge"
        nodeName.endsWith("-forge") -> "forge"
        else -> "fabric"
    }
    constants.match(loader, "fabric", "neoforge", "forge")

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
}
