@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

val currentOperatingSystemName: String = DefaultNativePlatform.getCurrentOperatingSystem().toFamilyName()
val currentArchitectureName: String = DefaultNativePlatform.getCurrentArchitecture().name

val arch = when(currentArchitectureName) {
    "arm64-v8", "aarch64" -> "arm64"
    "x86-64", "x86_64" -> "x64"
    else -> error("unknown architecture $currentArchitectureName")
}

val osArch = when(val c = "$currentOperatingSystemName-$arch") {
    "windows-x64" -> "windows"
    "macos-x64" -> "macos"
    else -> c
}

plugins {
    org.openrndr.convention.`kotlin-multiplatform`
    org.openrndr.convention.`publish-multiplatform`
    alias(libs.plugins.kotlin.serialization)
}


kotlin {
    jvm {

        compilations {
            val main by getting

            val demo by creating {
                associateWith(main)
            }
        }
    }

    sourceSets {
        val jvmMain by getting {
            dependencies {
                api(project(":openrndr-application"))
                implementation(libs.bundles.lwjgl.openal)
                implementation(libs.ffmpeg)
                implementation(project(":openrndr-jvm:openrndr-openal"))
                implementation(libs.kotlin.coroutines)
                implementation(libs.kotlin.serialization.core)
                implementation(libs.kotlin.serialization.json)
            }
        }

        val jvmDemo by getting {
            dependencies {
                runtimeOnly(libs.slf4j.simple)
                runtimeOnly(project(":openrndr-jvm:openrndr-gl3-natives-$osArch"))
                runtimeOnly(project(":openrndr-jvm:openrndr-openal-natives-$osArch"))
                runtimeOnly(project(":openrndr-jvm:openrndr-gl3"))
                runtimeOnly(project(":openrndr-jvm:openrndr-ffmpeg-natives-$osArch"))
            }
        }
    }
}

kotlin {
    jvm().mainRun {
        classpath(kotlin.jvm().compilations.getByName("demo").output.allOutputs)
        classpath(kotlin.jvm().compilations.getByName("demo").configurations.runtimeDependencyConfiguration!!)
    }
}

tasks.withType<JavaExec>().matching { it.name == "jvmRun" }.configureEach { workingDir = rootDir }
