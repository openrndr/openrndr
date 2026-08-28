@Suppress("DSL_SCOPE_VIOLATION")
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
                implementation(libs.kotlin.serialization.core)
            }
        }

        getByName("commonTest") {
            dependencies {
                implementation(libs.kotlin.serialization.json)
                implementation(libs.kotest.assertions)
            }
        }

        getByName("jvmTest") {
            dependencies {
                implementation(libs.kotlin.serialization.json)
            }
        }
    }
}
