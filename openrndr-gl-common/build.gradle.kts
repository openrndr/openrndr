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
        getByName("commonMain") {
            dependencies {
                implementation(project(":openrndr-draw"))
                implementation(libs.kotlin.logging)
            }
        }

        val commonJvmMain = getByName("commonJvmMain")

        if (platformConfiguration.android) {
            getByName("androidMain") {
                dependsOn(commonJvmMain)
            }
        }
    }
}