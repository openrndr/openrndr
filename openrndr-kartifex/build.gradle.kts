plugins {
    id("org.openrndr.convention.kotlin-multiplatform")
    id("org.openrndr.convention.kotlin-multiplatform-js")
    id("org.openrndr.convention.publish-multiplatform")
}

kotlin {
    sourceSets {

        val commonMain = getByName("commonMain") {
            dependencies {
                implementation(project(":openrndr-utils"))
            }
        }

        val commonTest = getByName("commonTest") {
            dependencies {
                implementation(libs.kotest.assertions)
            }
        }

        val jvmTest = getByName("jvmTest") {
            dependencies {
                implementation("io.lacuna:artifex:0.1.0-alpha1")
            }
        }
    }
}