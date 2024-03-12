plugins {
    org.openrndr.convention.`kotlin-multiplatform`
    org.openrndr.convention.`publish-multiplatform`
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        @Suppress("UNUSED_VARIABLE")
        val commonMain by getting {
            dependencies {
                implementation(project(":openrndr-math"))
                implementation(libs.kotlin.serialization.core)
            }
        }
    }
}