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
        val commonMain = getByName("commonMain")
        val commonJvmMain = getByName("commonJvmMain")
//
//        val commonJvmMain = getByName("commonJvmMain") {
//            dependsOn(commonMain)
//            kotlin.srcDirs("src/commonJvmMain")
//
//        }
//
        if (platformConfiguration.android) {
            val androidMain = getByName("androidMain") {
                dependsOn(commonJvmMain)
            }
        }
//
//        val jvmMain = getByName("jvmMain") {
//            dependsOn(commonJvmMain)
//        }
    }
}