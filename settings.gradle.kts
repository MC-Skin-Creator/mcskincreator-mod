pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
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
        versions("1.21.10", "1.21.11")
        version("26.1.x", "26.1.2")
        version("26.2.x", "26.2")
        version("26.3.x", "26.3")
        vcsVersion = "1.21.11"
    }
}

rootProject.name = "mcskincreator-mod"
