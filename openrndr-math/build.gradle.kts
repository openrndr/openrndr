@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("org.openrndr.convention.kotlin-multiplatform")
    id("org.openrndr.convention.publish-multiplatform")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlin.serialization.core)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.serialization.json)
                implementation(libs.kotest.assertions)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(libs.kotlin.serialization.json)
            }
        }
    }
}
