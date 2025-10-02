@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    id("org.openrndr.convention.kotlin-multiplatform")
    id("org.openrndr.convention.kotlin-multiplatform-js")
    id("org.openrndr.convention.publish-multiplatform")
}

kotlin {
    applyDefaultHierarchyTemplate { // or .custom depending on your setup
        common {
            group("commonJvm") {
                group("jvm") { withJvm() }
                group("android") { withAndroidTarget() }
            }
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":openrndr-math"))
                implementation(libs.kotlin.coroutines)
            }
        }

        val commonJvmMain by getting {

        }
//            dependsOn(commonMain)
//            kotlin.srcDirs("src/commonJvmMain")
//
//        }
//
        val androidMain by getting {
            dependsOn(commonJvmMain)
        }
//
//        val jvmMain by getting {
//            dependsOn(commonJvmMain)
////            kotlin.srcDir("src/commonJvmMain/kotlin")
//        }

    }
}