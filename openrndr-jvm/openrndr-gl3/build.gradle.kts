@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    id("org.openrndr.convention.kotlin-multiplatform")
    id("org.openrndr.convention.publish-multiplatform")
}

kotlin {
    jvm {
        compilations {
            val demo by creating {
            }
        }
    }

    applyDefaultHierarchyTemplate{
        // or .custom depending on your setup
        common {
            group("commonJvm") {
                group("jvm") { withJvm() }
                group("android") { withAndroidTarget() }
            }
        }
    }
    sourceSets {
        val commonMain by getting

        val commonJvmMain by getting {
            dependencies {
                implementation(project(":openrndr-application"))
                implementation(project(":openrndr-draw"))
                implementation(project(":openrndr-shape"))
                implementation(project(":openrndr-binpack"))
                implementation(project(":openrndr-extensions"))
                implementation(project(":openrndr-gl-common"))
                implementation(libs.kotlin.coroutines)
                implementation(libs.kotlin.logging)
                implementation(project(":openrndr-filter"))
                api(project(":openrndr-math"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(libs.lwjgl.core)
                implementation(libs.lwjgl.jemalloc)
                implementation(libs.lwjgl.opengl)
                implementation(libs.lwjgl.opengles)
                implementation(project(":openrndr-jvm:openrndr-gl3-support"))
            }
        }

        val jvmDemo by getting {
            dependencies {
                implementation(project(":openrndr-application"))
                implementation(project(":openrndr-extensions"))
                runtimeOnly(libs.slf4j.simple)
                runtimeOnly(project(":openrndr-jvm:openrndr-gl3-support"))
                runtimeOnly(project(":openrndr-jvm:openrndr-application-glfw"))
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(project(":openrndr-jvm:openrndr-application-glfw"))
            }
        }

        if (platformConfiguration.android) {
            val androidMain by getting {
                dependsOn(commonJvmMain)
            }
        }
    }
}
tasks.withType<Test>().configureEach {
    outputs.cacheIf { false }
}

kotlin {
    jvm().mainRun {
        classpath(kotlin.jvm().compilations.getByName("demo").output.allOutputs)
        classpath(kotlin.jvm().compilations.getByName("demo").configurations.runtimeDependencyConfiguration!!)
    }
}

tasks.withType<JavaExec>().matching { it.name == "jvmRun" }.configureEach { workingDir = rootDir }
