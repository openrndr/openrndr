@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    org.openrndr.convention.`kotlin-multiplatform`
    org.openrndr.convention.`publish-multiplatform`
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
            }
        }

        val jvmDemo by getting {
            dependencies {
                runtimeOnly(libs.slf4j.simple)
                runtimeOnly(project(":openrndr-jvm:openrndr-gl3-natives-macos-arm64"))
                runtimeOnly(project(":openrndr-jvm:openrndr-gl3"))
                runtimeOnly(project(":openrndr-jvm:openrndr-ffmpeg-natives-macos-arm64"))
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