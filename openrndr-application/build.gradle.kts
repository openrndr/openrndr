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
        val commonMain = getByName("commonMain") {
            dependencies {

                implementation(libs.kotlin.logging)
                implementation(libs.kotlin.atomicfu)
                implementation(libs.kotlin.coroutines)

                api(project(":openrndr-math"))
                api(project(":openrndr-draw"))
                api(project(":openrndr-animatable"))
                api(project(":openrndr-platform"))
            }
        }

        val commonJvmMain = getByName("commonJvmMain")

        val jvmMain = getByName("jvmMain") {

        }
        if (platformConfiguration.android) {
            val androidMain = getByName("androidMain") {
                dependsOn(commonJvmMain)
                dependencies {
                    implementation(libs.kotlin.coroutines)
                }
            }
        }
    }
}