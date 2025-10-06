package org.openrndr.convention

import org.gradle.kotlin.dsl.kotlin

plugins {
    kotlin("multiplatform")
}
val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

kotlin {
    js(IR) {
        browser()
        nodejs()
    }
    wasmJs() {
        browser()
        nodejs()
    }
    sourceSets {
        val webMain by creating {
            dependencies {
                implementation(libs.findLibrary("kotlin-logging").get())
            }
        }
    }
}
