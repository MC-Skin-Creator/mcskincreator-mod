import net.fabricmc.loom.api.LoomGradleExtensionAPI
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

// The Quilt targets' build script: the same sources as build.gradle.kts, built by
// Quilt Loom instead of Fabric Loom, against Quilt Loader and nothing else - no QSL,
// which stopped following game versions at 1.21.1, and no Fabric API. What does not
// depend on the loader - the Java level, the engine repository, the tests, the jar
// name - is kept word for word the same as in the other two scripts.
//
// Quilt Loom is already on this script's classpath: loom-back-compat puts it there,
// told to by the loomx.* lines of the Quilt tables in stonecutter.properties.toml.
// It is applied by hand rather than through loom-back-compat's project plugin, which
// only knows Fabric Loom's plugin ids, so this file has no generated accessors: the
// configurations are named as strings and the extension is asked for by type.
//
// Without a plugins block, nothing points this script's classpath at
// pluginManagement's repositories, so it names its own: Quilt's for Quilt Loom, and
// the ones Quilt Loom's own dependencies come from.
buildscript {
    repositories {
        maven("https://maven.quiltmc.org/repository/release/") { name = "Quilt" }
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        mavenCentral()
        gradlePluginPortal()
    }
}

// Like Fabric Loom, Quilt Loom comes in two variants in one jar: one remaps the
// obfuscated game (below 26), the other builds against the unobfuscated one (26+).
val unobfuscated = sc.current.parsed >= "26.1"
apply(plugin = if (unobfuscated) "org.quiltmc.loom.no_remap" else "org.quiltmc.loom.remap")

val loom = the<LoomGradleExtensionAPI>()

// The loader is in the file name and not in the version, as on NeoForge:
// mcskincreator-0.1.0+mc1.21.11-quilt.jar
version = "${property("mod.version")}+mc${sc.current.version}-quilt"
the<BasePluginExtension>().archivesName = property("mod.id") as String

// Mojang's requirement per game version, not a preference of ours.
val requiredJava: JavaVersion = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    else -> JavaVersion.VERSION_17
}

// The mixins that pose the character for the in-world view, written into the mixin
// config. From 1.21.2 one on the render state the renderer fills in - AvatarRenderer's
// from 1.21.9, PlayerRenderer's before - and up to 1.21.5 an accessor for the player
// renderers, which the editor's figure is drawn with there. Before 1.21.2 there are no
// render states, and two take their place: the renderer turns and lays the body down,
// the model bends the limbs.
val poseMixins: String = when {
    sc.current.parsed >= "1.21.6" -> listOf("AvatarRendererMixin")
    sc.current.parsed >= "1.21.2" -> listOf("AvatarRendererMixin", "EntityRenderDispatcherAccessor")
    else -> listOf("PlayerRendererMixin", "PlayerModelMixin")
}.joinToString(", ") { "\"$it\"" }

repositories {
    maven("https://maven.quiltmc.org/repository/release/") { name = "Quilt" }

    // See build.gradle.kts: the texture engine lives on GitHub Packages.
    maven("https://maven.pkg.github.com/MC-Skin-Creator/mcskincreator-engine") {
        name = "mcscEngine"
        credentials {
            username = providers.gradleProperty("gpr.user")
                    .orElse(providers.environmentVariable("GITHUB_ACTOR")).orNull
            password = providers.gradleProperty("gpr.token")
                    .orElse(providers.environmentVariable("GITHUB_TOKEN")).orNull
        }
        content { includeGroup("fr.clixmods.mcsc") }
    }
}

dependencies {
    "minecraft"("com.mojang:minecraft:${sc.current.version}")
    // The unobfuscated game carries Mojang's names already; the older ones are
    // remapped to them, as on Fabric.
    if (!unobfuscated) {
        "mappings"(loom.officialMojangMappings())
    }

    // The unobfuscated variant has no mod* configurations: nothing is remapped there.
    "${if (unobfuscated) "implementation" else "modImplementation"}"(
        "org.quiltmc:quilt-loader:${property("deps.quilt_loader")}")

    // The texture engine, inside the mod jar through Quilt's Jar-in-Jar, for the same
    // reason as on Fabric: without it the mod compiles and then dies in game on a
    // NoClassDefFoundError.
    val engine = "fr.clixmods.mcsc:mcsc-engine:${property("deps.mcsc_engine")}"
    "implementation"(engine)
    "include"(engine)

    "testImplementation"(platform("org.junit:junit-bom:${property("deps.junit")}"))
    "testImplementation"("org.junit.jupiter:junit-jupiter")
    "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
}

configure<JavaPluginExtension> {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava

    toolchain {
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

tasks {
    named<Test>("test") {
        useJUnitPlatform()

        testLogging {
            events("failed")
            exceptionFormat = TestExceptionFormat.FULL
        }
    }

    named<ProcessResources>("processResources") {
        fun MutableMap<String, String>.register(key: String, property: String) {
            val value: String = sc.properties[property]
            inputs.property(key, value)
            set(key, value)
        }

        val props = buildMap {
            register("group", "mod.group")
            register("id", "mod.id")
            register("name", "mod.name")
            register("version", "mod.version")
            register("minecraft", "mod.mc_compat")
            register("loader", "deps.quilt_loader")
            put("java", requiredJava.majorVersion)
            put("poseMixins", poseMixins)
            // Only the Forge targets that run under obfuscated names need a refmap.
            put("refmap", "")
            // A remapped jar says which names it was remapped to; an unobfuscated one
            // has none to name, and Quilt reads every game after 25 that way already.
            put("intermediate_mappings",
                if (unobfuscated) "" else "\"intermediate_mappings\": \"net.fabricmc:intermediary\",")
        }

        inputs.property("java", requiredJava.majorVersion)
        inputs.property("poseMixins", poseMixins)
        inputs.property("intermediate_mappings", props.getValue("intermediate_mappings"))
        filesMatching(listOf("quilt.mod.json", "mcskincreator.mixins.json", "mcskincreator.quilt.mixins.json")) {
            expand(props)
        }
        // The other loaders' metadata has nothing to say to Quilt.
        exclude("fabric.mod.json", "META-INF/neoforge.mods.toml", "META-INF/mods.toml")
    }

    withType<Jar> {
        val id = project.property("mod.id")
        inputs.property("mod_id", id)
        from(rootProject.file("LICENSE")) { rename { "${it}_$id" } }
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds the mod jar and copies it to build/libs/{mod version}/"

        inputs.property("version", project.property("mod.version"))
        // The jar that ships: remapped below 26, the plain one above.
        from(named<AbstractArchiveTask>(if (unobfuscated) "jar" else "remapJar").flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
    }
}
