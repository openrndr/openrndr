plugins {
    id("org.openrndr.convention.kotlin-multiplatform")
    id("org.openrndr.convention.kotlin-multiplatform-js")
    id("org.openrndr.convention.publish-multiplatform")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":openrndr-math"))
                api(project(":openrndr-color"))
                api(project(":openrndr-utils"))
                api(project(":openrndr-ktessellation"))
                implementation(project(":openrndr-kartifex"))
                implementation(libs.kotlin.logging)
                implementation(libs.kotlin.serialization.core)
            }
        }
    }
}