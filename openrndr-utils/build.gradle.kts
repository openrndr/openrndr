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
                group("jvm") { withJvm() }
                group("android") { withAndroidTarget() }
            }
        }
    }
    sourceSets {
        getByName("commonMain") {
            dependencies {
                implementation(project(":openrndr-math"))
                implementation(libs.kotlin.coroutines)
            }
        }

        val commonJvmMain = getByName("commonJvmMain") {
//            dependsOn(commonMain)
//            kotlin.srcDirs("src/commonJvmMain")
        }
        if (platformConfiguration.android) {
            getByName("androidMain") {
                dependsOn(commonJvmMain)
            }
        }

//        getByName("jvmMain") {
//            dependsOn(commonJvmMain)
////            kotlin.srcDir("src/commonJvmMain/kotlin")
//        }


        getByName("webMain") {
            dependencies {
                implementation(libs.kotlin.js)
            }
        }
    }
}