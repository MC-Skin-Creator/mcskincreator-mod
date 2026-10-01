import org.gradle.api.tasks.testing.logging.TestExceptionFormat

// The NeoForge targets' build script: the same sources as build.gradle.kts, built by
// ModDevGradle instead of Loom. What does not depend on the loader - the Java level,
// the engine repository, the tests, the jar name - is kept word for word the same as
// there and in build.quilt.gradle.kts, so a change to one of those belongs in all three.
plugins {
    id("net.neoforged.moddev")
}

// The loader is in the file name and not in the version: the Fabric release globs
// match "+mc<version>.jar", and a NeoForge jar must not answer to them.
// mcskincreator-0.1.0+mc1.21.11-neoforge.jar
version = "${property("mod.version")}+mc${sc.current.version}-neoforge"
base.archivesName = property("mod.id") as String

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

neoForge {
    version = sc.properties["deps.neoforge"]

    mods {
        register(property("mod.id") as String) {
            sourceSet(sourceSets.main.get())
        }
    }

    runs {
        register("client") {
            client()
            gameDirectory = rootProject.file("run")
        }
    }

    // The tests speak in Component and PlayerModelType, like on Fabric. NeoForge
    // patches those classes, so the tests need NeoForge on their classpath as well
    // as the game.
    addModdingDependenciesTo(sourceSets.test.get())
}

dependencies {
    // The texture engine, inside the mod jar through NeoForge's Jar-in-Jar, for the
    // same reason as include() on Fabric: without it the mod compiles and then dies in
    // game on a NoClassDefFoundError.
    val engine = "fr.clixmods.mcsc:mcsc-engine:${property("deps.mcsc_engine")}"
    implementation(engine)
    jarJar(engine)

    testImplementation(platform("org.junit:junit-bom:${property("deps.junit")}"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava

    toolchain {
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

tasks {
    test {
        useJUnitPlatform()

        testLogging {
            events("failed")
            exceptionFormat = TestExceptionFormat.FULL
        }
    }

    processResources {
        fun MutableMap<String, String>.register(key: String, property: String) {
            val value: String = sc.properties[property]
            inputs.property(key, value)
            set(key, value)
        }

        val props = buildMap {
            register("id", "mod.id")
            register("name", "mod.name")
            register("version", "mod.version")
            register("minecraft", "mod.mc_compat")
            register("neoforge", "deps.neoforge_compat")
            put("java", requiredJava.majorVersion)
            put("poseMixins", poseMixins)
            // Only the Forge targets that run under obfuscated names need a refmap.
            put("refmap", "")
        }

        inputs.property("java", requiredJava.majorVersion)
        inputs.property("poseMixins", poseMixins)
        filesMatching(listOf("META-INF/neoforge.mods.toml", "mcskincreator.mixins.json")) { expand(props) }
        // The other loaders' metadata has nothing to say to NeoForge, and Quilt's mixin
        // config stands in for an event NeoForge already has.
        exclude("fabric.mod.json", "META-INF/mods.toml", "quilt.mod.json",
                "mcskincreator.quilt.mixins.json")
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
        from(jar.flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
    }
}
