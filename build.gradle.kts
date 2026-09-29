import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    // Applies the Loom variant matching this node's Minecraft version.
    id("dev.kikugie.loom-back-compat")
}

// Mod version and Minecraft version are separate concepts; the artifact name carries
// both: mcskincreator-0.1.0+mc1.21.11.jar
version = "${property("mod.version")}+mc${sc.current.version}"
base.archivesName = property("mod.id") as String

// Mojang's requirement per game version, not a preference of ours.
val requiredJava: JavaVersion = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    else -> JavaVersion.VERSION_21
}

// Mojang renamed ResourceLocation to Identifier in 1.21.11 and changed nothing else about
// it. A pure rename used in a dozen files is a replacement, not a conditional in each of
// them: the sources say Identifier, and older targets read ResourceLocation.
sc.replacements.regex(sc.current.parsed >= "1.21.11") {
    replace("\\bResourceLocation\\b", "Identifier", "\\bIdentifier\\b", "ResourceLocation")
}

dependencies {
    fun fapi(vararg modules: String) {
        for (it in modules) modImplementation(fabricApi.module(it, sc.properties["deps.fabric_api"]))
    }

    minecraft("com.mojang:minecraft:${sc.current.version}")
    // Yarn has no build past 1.21.11, so official Mojang mappings are the only option
    // that spans every target from one source tree.
    loomx.applyMojangMappings()

    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    // Only the module the mod actually uses, to keep each target's setup quick.
    fapi("fabric-screen-api-v1")

    // JUnit 5, one version for every target. Nothing else is declared for the tests:
    // Loom already puts this target's own Minecraft jar on their classpath, which is
    // what lets them speak in Component and PlayerModelType without a game.
    testImplementation(platform("org.junit:junit-bom:${property("deps.junit")}"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    // Gradle stopped supplying the launcher itself; without it the test task starts
    // and finds no engine.
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

loom {
    runConfigs.all {
        preferGradleTask = true
        generateRunConfig = true
        runDirectory = rootProject.file("run")
    }
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

        // A failing assertion says what it was in the console: the default prints
        // the test's name and leaves the reason in the HTML report.
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
            register("loader", "deps.fabric_loader")
            // Declared per target so neither jar claims a Java it cannot run on.
            put("java", requiredJava.majorVersion)
        }

        inputs.property("java", requiredJava.majorVersion)
        // The mixin config is expanded too, for its compatibility level alone: Mixin
        // checks it against the class file version of the mixin classes, and those are
        // Java 21 on one target and Java 25 on the other. One hardcoded level would be
        // wrong on one of them.
        filesMatching(listOf("fabric.mod.json", "mcskincreator.mixins.json")) { expand(props) }
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
        from(loomx.modJar.flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
    }
}
