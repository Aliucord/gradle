plugins {
    kotlin("jvm") version "1.7.20"
    id("java-gradle-plugin")
    id("maven-publish")
}

group = "com.aliucord"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

repositories {
    mavenCentral()
    google()
    maven("https://jitpack.io")
    maven("https://maven.aliucord.com/snapshots")
}

dependencies {
    implementation(kotlin("stdlib", kotlin.coreLibrariesVersion))
    compileOnly(gradleApi())

    compileOnly("com.google.guava:guava:30.1.1-jre")
    compileOnly("com.android.tools:sdk-common:31.0.0")
    compileOnly("com.android.tools.build:gradle:7.0.4")

    implementation("com.github.Aliucord.dex2jar:dex-translator:808b91d679")
    implementation("com.aliucord.jadx:jadx-core:1.4.5-SNAPSHOT")
    implementation("com.aliucord.jadx:jadx-dex-input:1.4.5-SNAPSHOT")
    implementation("com.aliucord:jadb:1.2.1-SNAPSHOT")
}

gradlePlugin {
    plugins {
        create("com.aliucord.gradle") {
            id = "com.aliucord.gradle"
            implementationClass = "com.aliucord.gradle.AliucordPlugin"
        }
    }
}

publishing {
    repositories {
        val username = System.getenv("MAVEN_USERNAME")
        val password = System.getenv("MAVEN_PASSWORD")

        if (username != null && password != null) {
            maven {
                credentials {
                    this.username = username
                    this.password = password
                }
                setUrl("https://maven.aliucord.com/snapshots")
            }
        } else {
            mavenLocal()
        }
    }
}
