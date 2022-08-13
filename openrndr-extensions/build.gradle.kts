plugins {
    org.openrndr.convention.`kotlin-multiplatform`
}

kotlin {
    sourceSets {
        @Suppress("UNUSED_VARIABLE")
        val commonMain by getting {
            dependencies {
                implementation(project(":openrndr-application"))
                implementation(project(":openrndr-draw"))
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jsMain by getting {
            dependencies {
                implementation(project(":openrndr-js:openrndr-webgl"))
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jvmMain by getting {
            dependencies {
                implementation(libs.kotlin.logging)
            }
        }
    }
}