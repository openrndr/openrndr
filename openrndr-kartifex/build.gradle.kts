plugins {
    org.openrndr.convention.`kotlin-multiplatform`
    org.openrndr.convention.`publish-multiplatform`
}

kotlin {
    sourceSets {

        val commonMain by getting {
            dependencies {
                implementation(project(":openrndr-utils"))
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val commonTest by getting {
            dependencies {
                implementation(libs.kotest.assertions)
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jvmTest by getting {
            dependencies {
                implementation("io.lacuna:artifex:0.1.0-alpha1")
            }
        }
    }
}