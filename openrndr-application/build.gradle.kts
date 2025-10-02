plugins {
    id("org.openrndr.convention.kotlin-multiplatform")
    id("org.openrndr.convention.kotlin-multiplatform-js")
    id("org.openrndr.convention.publish-multiplatform")
}

kotlin {
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
        val commonMain by getting {
            dependencies {
                api(project(":openrndr-math"))
                api(project(":openrndr-draw"))
                api(project(":openrndr-animatable"))
                api(project(":openrndr-platform"))
            }
        }

        val commonJvmMain by getting

        val jvmMain by getting {
            dependencies {
                implementation(libs.kotlin.coroutines)
            }
        }
        val androidMain by getting {
            dependsOn(commonJvmMain)
            dependencies {
                implementation(libs.kotlin.coroutines)
            }
        }
    }
}