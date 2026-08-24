package org.openrndr.convention

import org.gradle.kotlin.dsl.kotlin
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
}
val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

kotlin {
    js {
        browser()
        nodejs()
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        nodejs()
    }
    sourceSets {
        create("webMain") {
            dependencies {
                implementation(libs.findLibrary("kotlin-logging").get())
            }
        }
    }
}
