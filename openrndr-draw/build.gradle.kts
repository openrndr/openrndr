import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    id("org.openrndr.convention.kotlin-multiplatform")
    id("org.openrndr.convention.kotlin-multiplatform-js")
    id("org.openrndr.convention.publish-multiplatform")
}

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate { // or .custom depending on your setup
        common {
            group("commonJvm") {
                withJvm()
                group("jvm") { withJvm() }
                group("android") { withAndroidTarget() }
            }
        }
    }

    sourceSets {
        getByName("commonTest") {
            dependencies {
                implementation(libs.kotest.assertions)
            }
        }

        getByName("commonMain") {
            dependencies {
                api(project(":openrndr-math"))
                api(project(":openrndr-color"))
                api(project(":openrndr-shape"))
                api(project(":openrndr-event"))
                implementation(project(":openrndr-utils"))
                implementation(project(":openrndr-platform"))
                implementation(libs.kotlin.coroutines)
                implementation(libs.kotlin.logging)
            }
        }
        val commonJvmMain = getByName("commonJvmMain")

        if (platformConfiguration.android) {
            getByName("androidMain") {
                dependsOn(commonJvmMain)
            }
        }
        getByName("webMain") {
            dependencies {
                implementation(libs.kotlin.js)
                implementation(libs.kotlin.browser)
            }
        }
    }
}