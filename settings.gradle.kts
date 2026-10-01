pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
        maven("https://maven.neoforged.net/releases") { name = "NeoForged" }
        maven("https://maven.minecraftforge.net/") { name = "MinecraftForge" }
    }

    // The NeoForge targets' build plugin, pinned here because their buildscript is
    // applied per node and cannot carry a version of its own.
    plugins {
        id("net.neoforged.moddev") version "2.0.148"

        // The Forge targets' build plugins, pinned for the same reason: ForgeGradle 7,
        // and Forge's own Jar-in-Jar, which ForgeGradle leaves to a separate plugin.
        id("net.minecraftforge.gradle") version "7.0.40"
        id("net.minecraftforge.jarjar") version "0.2.3"
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.8"

    // Picks the Loom variant matching each Minecraft version. Loom 1.18+ targets the
    // 26.x game versions and refuses to run below a Java 25 JVM, so one plugin version
    // cannot serve both targets on its own.
    id("dev.kikugie.loom-back-compat") version "0.4.2"

    // Lets Gradle download the JDK a target needs instead of failing on a missing one.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    create(rootProject) {
        // Fabric targets: named after the game version alone, as they always were.
        versions("1.21.10", "1.21.11")
        version("26.1.x", "26.1.2")
        version("26.2.x", "26.2")
        version("26.3.x", "26.3")

        // NeoForge targets: the same game versions, the same sources, another build
        // script. The -neoforge suffix is what stonecutter.gradle.kts reads the loader
        // from.
        val loaderNodes = listOf(
            "1.21.10" to "1.21.10",
            "1.21.11" to "1.21.11",
            "26.1.x" to "26.1.2",
            "26.2.x" to "26.2",
            "26.3.x" to "26.3",
        )
        for ((name, minecraft) in loaderNodes) {
            version("$name-neoforge", minecraft).buildscript("build.neoforge.gradle.kts")
        }

        // Forge targets: the same again, built by ForgeGradle. The -forge suffix is
        // read the same way; "-neoforge" does not end in "-forge", so the two never
        // answer to each other's name.
        for ((name, minecraft) in loaderNodes) {
            version("$name-forge", minecraft).buildscript("build.forge.gradle.kts")
        }

        vcsVersion = "1.21.11"
    }
}

rootProject.name = "mcskincreator-mod"
