pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
        maven("https://jitpack.io")
        maven("https://maven.aliucord.com/snapshots")
    }
}

rootProject.name = "gradle"
