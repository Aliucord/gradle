plugins {
    kotlin("jvm") version "1.5.21"
    id("java-gradle-plugin")
    id("maven-publish")
}

group = "com.aliucord"
version = "0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

repositories {
    mavenCentral()
    google()
    maven("https://jitpack.io")
}

dependencies {
    implementation(kotlin("stdlib", kotlin.coreLibrariesVersion))
    compileOnly(gradleApi())

    compileOnly("com.google.guava:guava:30.1.1-jre")
    compileOnly("com.android.tools:sdk-common:30.0.0")
    compileOnly("com.android.tools.build:gradle:7.0.0")

    implementation("com.github.Aliucord.dex2jar:dex-translator:d5a5efb06c")
    implementation("com.github.Aliucord.jadx:jadx-core:1a213e978d")
    implementation("com.github.Aliucord.jadx:jadx-dex-input:1a213e978d")
    implementation("com.github.js6pak:jadb:fix-modified-time-SNAPSHOT")
}

gradlePlugin {
    plugins {
        create("com.aliucord.gradle") {
            id = "com.aliucord.gradle"
            implementationClass = "com.aliucord.gradle.AliucordPlugin"
        }
    }
}