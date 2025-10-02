@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.kotlin.dsl.invoke
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    id("org.openrndr.convention.kotlin-multiplatform")
    id("org.openrndr.convention.publish-multiplatform")
//    id("org.openrndr.convention.variant")
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

        if (platformConfiguration.android) {
            val androidMain by getting {
                dependsOn(commonJvmMain)
            }
        }
    }
}

//tasks {
//    @Suppress("UNUSED_VARIABLE")
//    val test by getting(Test::class) {
//        onlyIf { !project.hasProperty("skip.gl3.tests") }
//
//        if (DefaultNativePlatform.getCurrentOperatingSystem().isMacOsX) {
//            jvmArgs = jvmArgs + "-XstartOnFirstThread"
//        }
//        useJUnitPlatform()
//        testLogging.exceptionFormat = TestExceptionFormat.FULL
//    }
//}
/*
variants {
    val nativeLibs = listOf(
        libs.lwjgl.core,
        libs.lwjgl.glfw,
        libs.lwjgl.opengl,
        libs.lwjgl.opengles,
        libs.lwjgl.jemalloc,
    )

    platform(OperatingSystemFamily.MACOS, MachineArchitecture.ARM64, sourceSets = "jvmMain") {
        jar {

        }
        dependencies {
            nativeLibs.forEach {
                runtimeOnly(it.get().withClassifier("natives-macos-arm64"))
            }
        }
    }
    platform(OperatingSystemFamily.MACOS, MachineArchitecture.X86_64) {
        jar {

        }
        dependencies {
            nativeLibs.forEach {
                runtimeOnly(it.get().withClassifier("natives-macos"))
            }
        }
    }
    platform(OperatingSystemFamily.LINUX, MachineArchitecture.ARM64) {
        dependencies {
            nativeLibs.forEach {
                runtimeOnly(it.get().withClassifier("natives-linux-arm64"))
            }
        }
    }
    platform(OperatingSystemFamily.LINUX, MachineArchitecture.X86_64) {
        dependencies {
            nativeLibs.forEach {
                runtimeOnly(it.get().withClassifier("natives-linux"))
            }
        }
    }
    platform(OperatingSystemFamily.WINDOWS, MachineArchitecture.ARM64) {
        dependencies {
            nativeLibs.forEach {
                runtimeOnly(it.get().withClassifier("natives-windows-arm64"))
            }
        }
    }
    platform(OperatingSystemFamily.WINDOWS, MachineArchitecture.X86_64) {
        dependencies {
            nativeLibs.forEach {
                runtimeOnly(it.get().withClassifier("natives-windows"))
            }
        }
    }
}
*/
//val main by sourceSets.getting
//val apiElements by configurations.getting
//val runtimeElements by configurations.getting
//
//dependencies {
//    implementation(project(":openrndr-application"))
//    implementation(project(":openrndr-draw"))
//    implementation(project(":openrndr-shape"))
//    implementation(project(":openrndr-binpack"))
//    implementation(project(":openrndr-extensions"))
//    implementation(project(":openrndr-gl-common"))
//    implementation(libs.kotlin.coroutines)
//    implementation(libs.lwjgl.core)
//    implementation(libs.lwjgl.jemalloc)
//    implementation(libs.lwjgl.opengl)
//    implementation(libs.lwjgl.opengles)
//    implementation(project(":openrndr-filter"))
//    api(project(":openrndr-math"))
//    testImplementation(libs.kotlin.reflect)
//    testImplementation(libs.kotest.assertions)
//    testImplementation(project(":openrndr-jvm:openrndr-application-glfw"))
//    demoImplementation(project(":openrndr-draw"))
//    demoImplementation(project(":openrndr-application"))
//    demoImplementation(project(":openrndr-extensions"))
//    demoRuntimeOnly(libs.slf4j.simple)
//    demoRuntimeOnly(project(":openrndr-jvm:openrndr-gl3"))
//    demoRuntimeOnly(project(":openrndr-jvm:openrndr-application-glfw"))
//}

kotlin {
    jvm().mainRun {
        classpath(kotlin.jvm().compilations.getByName("demo").output.allOutputs)
        classpath(kotlin.jvm().compilations.getByName("demo").configurations.runtimeDependencyConfiguration!!)
    }
}

tasks.withType<JavaExec>().matching { it.name == "jvmRun" }.configureEach { workingDir = rootDir }
