plugins {
    `kotlin-dsl`
    `maven-publish`
}

group = "com.aliucord"

kotlin {
    jvmToolchain(17)
}

dependencies {
    compileOnly(libs.android.gradle)
    compileOnly(libs.android.sdk)

    implementation(libs.dex2jar)
    implementation(libs.jadx.core)
    implementation(libs.jadx.dexInput)
    implementation(libs.jadb)
}

gradlePlugin {
    plugins {
        create("aliucord-plugin") {
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
