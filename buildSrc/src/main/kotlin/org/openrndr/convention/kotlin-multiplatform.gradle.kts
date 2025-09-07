package org.openrndr.convention

import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion


val libs = the<LibrariesForLibs>()

plugins {
    kotlin("multiplatform")
}

repositories {
    mavenCentral()
}

group = "org.openrndr"

tasks.withType<KotlinCompilationTask<*>> {
    compilerOptions {
        apiVersion.set(KotlinVersion.valueOf("KOTLIN_${libs.versions.kotlinApi.get().replace(".", "_")}"))
        languageVersion.set(KotlinVersion.valueOf("KOTLIN_${libs.versions.kotlinLanguage.get().replace(".", "_")}"))
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}
tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(libs.versions.jvmTarget.get()))
        freeCompilerArgs.add("-Xjdk-release=${libs.versions.jvmTarget.get()}")
    }
}

kotlin {
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

    js(IR) {
        browser()
        nodejs()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlin.stdlib)
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(libs.kotlin.logging)
            }
        }

        val jsMain by getting {
            dependencies {
                implementation(libs.kotlin.logging)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        val jvmTest by getting {
            dependencies {
                runtimeOnly(libs.bundles.jupiter)
                runtimeOnly(libs.slf4j.simple)
            }
        }
    }
}

val currentOperatingSystemName: String = DefaultNativePlatform.getCurrentOperatingSystem().toFamilyName()
val currentArchitectureName: String = DefaultNativePlatform.getCurrentArchitecture().name

configurations.matching {
    it.name.endsWith("runtimeClasspath", ignoreCase = true)
}.configureEach {
    attributes {
        attribute(OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE, objects.named(currentOperatingSystemName))
        attribute(MachineArchitecture.ARCHITECTURE_ATTRIBUTE, objects.named(currentArchitectureName))
    }
}
