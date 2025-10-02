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
                api(project(":openrndr-color"))
                api(project(":openrndr-shape"))
                api(project(":openrndr-event"))
                implementation(project(":openrndr-utils"))
                implementation(project(":openrndr-platform"))
                implementation(libs.kotlin.coroutines)
            }
        }
        val commonJvmMain by getting


        if (platformConfiguration.android) {
            val androidMain by getting {
                dependsOn(commonJvmMain)
            }
        }
    }
}