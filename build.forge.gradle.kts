import org.gradle.api.tasks.testing.logging.TestExceptionFormat

// The Forge targets' build script: the same sources as build.gradle.kts, built by
// ForgeGradle 7 instead of Loom. What does not depend on the loader - the Java level,
// the engine repository, the tests, the jar name - is kept word for word the same as
// there, so a change to one of those belongs in all three files.
plugins {
    id("net.minecraftforge.gradle")
    // ForgeGradle 7 leaves Jar-in-Jar to Forge's separate plugin.
    id("net.minecraftforge.jarjar")
}

// The loader is in the file name and not in the version, like on NeoForge: the Fabric
// release globs match "+mc<version>.jar", and a Forge jar must not answer to them.
// mcskincreator-0.1.0+mc1.21.11-forge.jar
version = "${property("mod.version")}+mc${sc.current.version}-forge"
base.archivesName = property("mod.id") as String

// Mojang's requirement per game version, not a preference of ours.
val requiredJava: JavaVersion = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    else -> JavaVersion.VERSION_21
}

// Forge ships Sponge's Mixin 0.8.7, whose compatibility levels stop at JAVA_21: it has
// no JAVA_25 to read. Java 25 mixin classes only make it log a warning - the game's own
// classes are Java 25 on 26.x - so the level is capped, not the bytecode.
val mixinJava: String = minOf(requiredJava.majorVersion.toInt(), 21).toString()

minecraft {
    // 1.21.x still ships obfuscated; 26.x is unobfuscated and takes no mappings at all.
    // Forge runs the game under Mojang's names on both, which are the names the sources
    // are written in, so the jar needs no remapping.
    if (sc.current.parsed < "26.1") {
        mappings("official", sc.current.version)
    }

    runs {
        register("client") {
            workingDir.set(rootProject.layout.projectDirectory.dir("run"))
        }
    }
}

repositories {
    // Where ForgeGradle puts the game and Forge once it has set them up, and where
    // their libraries come from.
    minecraft.mavenizer(this)
    maven(fg.forgeMaven)
    maven(fg.minecraftLibsMaven)
    mavenCentral()

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

// The jar that ships is the one with the engine inside: jarJar takes the plain jar's
// name, and the plain jar steps aside as "-slim".
jarJar.register {
    archiveClassifier = null
}

dependencies {
    // The game and Forge together. implementation also puts them on the test
    // classpath, where the tests read Component and PlayerModelType, like on the other
    // loaders.
    implementation(minecraft.dependency("net.minecraftforge:forge:${sc.current.version}-${property("deps.forge")}"))

    // The texture engine, inside the mod jar through Forge's Jar-in-Jar, for the same
    // reason as include() on Fabric: without it the mod compiles and then dies in game
    // on a NoClassDefFoundError.
    val engine = "fr.clixmods.mcsc:mcsc-engine:${property("deps.mcsc_engine")}"
    implementation(engine)
    "jarJar"(engine)

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
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }

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
            register("forge", "deps.forge_compat")
            put("java", mixinJava)
        }

        inputs.property("java", mixinJava)
        filesMatching(listOf("META-INF/mods.toml", "mcskincreator.mixins.json")) { expand(props) }
        // The other loaders' metadata has nothing to say to Forge.
        exclude("fabric.mod.json", "META-INF/neoforge.mods.toml")
    }

    jar {
        archiveClassifier = "slim"
    }

    withType<Jar>().configureEach {
        // Forge reads mixin configs from the manifest rather than from mods.toml. Set
        // on the jarJar task as well as the plain jar, so the jar that ships has it
        // whether or not jarJar copies the plain jar's manifest.
        manifest.attributes("MixinConfigs" to "mcskincreator.mixins.json")
    }

    // Not the jarJar task: it already carries everything the plain jar does, and a
    // second LICENSE would be a duplicate entry.
    withType<Jar>().matching { it.name != "jarJar" }.configureEach {
        val id = project.property("mod.id")
        inputs.property("mod_id", id)
        from(rootProject.file("LICENSE")) { rename { "${it}_$id" } }
    }

    named("assemble") { dependsOn("jarJar") }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds the mod jar and copies it to build/libs/{mod version}/"

        inputs.property("version", project.property("mod.version"))
        // The jarJar task's output, engine included - never the -slim jar.
        from(named("jarJar"))
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
    }
}
