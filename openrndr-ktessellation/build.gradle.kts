plugins {
    id("org.openrndr.convention.kotlin-multiplatform")
    id("org.openrndr.convention.kotlin-multiplatform-js")
    id("org.openrndr.convention.publish-multiplatform")
}

kotlin {
    sourceSets {
        getByName("commonMain") {
            dependencies {
                implementation(project(":openrndr-math"))
            }
        }

        getByName("commonTest") {
            dependencies {
                implementation(libs.kotest.assertions)
                implementation(project(":openrndr-shape"))
            }
        }
    }
}