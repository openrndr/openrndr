package org.openrndr.convention

import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile


val libs = the<LibrariesForLibs>()

plugins {
    kotlin("multiplatform")
}

repositories {
    mavenCentral()
}

group = "org.openrndr"

tasks.withType<KotlinCompile<*>>() {
    kotlinOptions.apiVersion = libs.versions.kotlinApi.get()
    kotlinOptions.languageVersion = libs.versions.kotlinLanguage.get()
    kotlinOptions.freeCompilerArgs += "-Xexpect-actual-classes"
}

kotlin {
    jvm {
        jvmToolchain {
            languageVersion.set(JavaLanguageVersion.of(libs.versions.jvmTarget.get()))
            vendor.set(JvmVendorSpec.ADOPTIUM)
        }

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
        @Suppress("UNUSED_VARIABLE")
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

        @Suppress("UNUSED_VARIABLE")
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jvmTest by getting {
            dependencies {
                runtimeOnly(libs.bundles.jupiter)
                runtimeOnly(libs.slf4j.simple)
            }
        }
    }
}

