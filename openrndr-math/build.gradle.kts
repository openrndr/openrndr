@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    org.openrndr.convention.`kotlin-multiplatform`
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        @Suppress("UNUSED_VARIABLE")
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlin.serialization.core)
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.serialization.json)
                implementation(libs.kotest)
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jvmTest by getting {
            dependencies {
                implementation(libs.kotlin.serialization.json)
            }
        }
    }
}
