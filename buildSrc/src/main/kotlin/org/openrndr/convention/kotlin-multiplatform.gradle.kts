package org.openrndr.convention

import org.gradle.accessors.dm.LibrariesForLibs
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val libs = the<LibrariesForLibs>()

plugins {
    kotlin("multiplatform")
    id("maven-publish")
}

repositories {
    mavenCentral()
}

group = "org.openrndr"

tasks.withType<KotlinCompile>() {
    kotlinOptions.apiVersion = libs.versions.kotlinApi.get()
}

kotlin {
    jvm {
        jvmToolchain {
            languageVersion.set(JavaLanguageVersion.of(libs.versions.jvmTarget.get()))
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    js(IR) {
        browser()
        nodejs()
    }

    sourceSets {
        @Suppress("UNUSED_VARIABLE")
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
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