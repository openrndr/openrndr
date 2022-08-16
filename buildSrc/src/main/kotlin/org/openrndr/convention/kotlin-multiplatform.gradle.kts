package org.openrndr.convention

import org.gradle.accessors.dm.LibrariesForLibs
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val libs = the<LibrariesForLibs>()

plugins {
    kotlin("multiplatform")
    id("maven-publish")
    id("org.openrndr.convention.dokka")
}

repositories {
    mavenCentral()
}

group = "org.openrndr"

tasks.withType<KotlinCompile>() {
    kotlinOptions.apiVersion = libs.versions.kotlinApi.get()
}

dependencies {
    components {
        for (module in LwjglModules.all) {
            withModule<LwjglRule_gradle.LwjglRule>("org.lwjgl:$module")
        }
    }
}

kotlin {
    jvm {
        jvmToolchain {
            this as JavaToolchainSpec
            languageVersion.set(JavaLanguageVersion.of(libs.versions.jvmTarget.get()))
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
        addHostMachineAttributesToConfiguration("jvmRuntimeClasspath")
        addHostMachineAttributesToConfiguration("jvmTestRuntimeClasspath")
    }

    js(IR) {
        browser()
        nodejs()
    }

    sourceSets {
        @Suppress("UNUSED_VARIABLE")
        val commonMain by getting {
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
            }
        }
    }
}