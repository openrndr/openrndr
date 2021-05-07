plugins {
    kotlin("multiplatform")
}

val lwjglNatives = "natives-linux-arm64"
val lwjglVersion: String by rootProject.extra
val kotlinLanguageVersion: String by rootProject.extra
val kotlinApiVersion: String by rootProject.extra

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
            kotlinOptions.apiVersion = kotlinApiVersion
            kotlinOptions.languageVersion = kotlinLanguageVersion
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
            exclude("**/*.class")
        }
    }
    sourceSets {
        val jvmMain by getting {
            dependencies {
                runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:$lwjglNatives")
                runtimeOnly("org.lwjgl:lwjgl-openal:$lwjglVersion:$lwjglNatives")
            }
        }
    }
}