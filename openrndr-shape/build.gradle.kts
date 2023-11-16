plugins {
    org.openrndr.convention.`kotlin-multiplatform`
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        @Suppress("UNUSED_VARIABLE")
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