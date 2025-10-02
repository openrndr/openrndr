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
    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation(libs.findLibrary("kotlin-logging").get())
            }
        }
    }
}
