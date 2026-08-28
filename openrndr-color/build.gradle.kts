plugins {
    id("org.openrndr.convention.kotlin-multiplatform")
    id("org.openrndr.convention.kotlin-multiplatform-js")
    id("org.openrndr.convention.publish-multiplatform")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        getByName("commonMain") {
            dependencies {
                implementation(project(":openrndr-math"))
                implementation(libs.kotlin.serialization.core)
            }
        }
    }
}