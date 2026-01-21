package org.openrndr.convention

import com.android.build.api.dsl.androidLibrary
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

fun arch(arch: String = System.getProperty("os.arch")): String {
    return when (arch) {
        "x86-64", "x86_64", "amd64" -> "x86-64"
        "arm64", "aarch64" -> "aarch64"
        else -> error("unsupported arch $arch")
    }
}

project.extensions.create("platformConfiguration", PlatformConfiguration::class.java )
val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

plugins {
    kotlin("multiplatform")
}

if (property("openrndr.platform.android")=="true") {
    apply(plugin = "com.android.kotlin.multiplatform.library")
}

repositories {
    mavenCentral()
    google()
}

group = "org.openrndr"

tasks.withType<KotlinCompilationTask<*>> {
    compilerOptions {
        apiVersion.set(
            KotlinVersion.valueOf(
                "KOTLIN_${
                    libs.findVersion("kotlinApi").get().displayName.replace(
                        ".",
                        "_"
                    )
                }"
            )
        )
        languageVersion.set(
            KotlinVersion.valueOf(
                "KOTLIN_${
                    libs.findVersion("kotlinLanguage").get().displayName.replace(".", "_")
                }"
            )
        )
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}
tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(libs.findVersion("jvmTarget").get().displayName))
        freeCompilerArgs.add("-Xjdk-release=${libs.findVersion("jvmTarget").get().displayName}")
    }
}

kotlin {
    if (property("openrndr.platform.android")=="true") {
        androidLibrary {
            namespace = "org.openrndr"
            compileSdk = 33
            minSdk = 24
        }
    }

    jvm {
        testRuns["test"].executionTask {
            if (DefaultNativePlatform.getCurrentOperatingSystem().isMacOsX) {
                allJvmArgs = allJvmArgs + "-XstartOnFirstThread"
            }
            allJvmArgs = allJvmArgs + "-Dorg.openrndr.gl3.skip_glfw_termination"
            useJUnitPlatform()
            testLogging.exceptionFormat = TestExceptionFormat.FULL
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.findLibrary("kotlin-stdlib").get())
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(libs.findLibrary("kotlin-logging").get())
            }
        }

        if (property("openrndr.platform.android")=="true") {
            val androidMain by getting {
                dependencies {
                    implementation(libs.findLibrary("kotlin-logging").get())
                }
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.findLibrary("kotlin-test").get())
            }
        }

        val jvmTest by getting {
            dependencies {
                runtimeOnly(libs.findBundle("jupiter").get())
                runtimeOnly(libs.findLibrary("slf4j-simple").get())
            }
        }
    }
}

val currentOperatingSystemName: String = DefaultNativePlatform.getCurrentOperatingSystem().toFamilyName()
//val currentArchitectureName: String = DefaultNativePlatform.getCurrentArchitecture().name
val currentArchitectureName: String = arch()

configurations.matching {
    it.name.endsWith("runtimeClasspath", ignoreCase = true)
}.configureEach {
    attributes {
        attribute(OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE, objects.named(currentOperatingSystemName))
        attribute(MachineArchitecture.ARCHITECTURE_ATTRIBUTE, objects.named(currentArchitectureName))
    }
}
