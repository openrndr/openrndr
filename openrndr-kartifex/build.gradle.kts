plugins {
    id("org.openrndr.convention.kotlin-multiplatform")
    id("org.openrndr.convention.kotlin-multiplatform-js")
    id("org.openrndr.convention.publish-multiplatform")
}

kotlin {
    sourceSets {

        val commonMain by getting {
            dependencies {
                implementation(project(":openrndr-utils"))
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotest.assertions)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation("io.lacuna:artifex:0.1.0-alpha1")
            }
        }
    }
}