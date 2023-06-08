plugins {
    org.openrndr.convention.`kotlin-multiplatform`
}

kotlin {
    sourceSets {
        @Suppress("UNUSED_VARIABLE")
        val commonMain by getting {
            dependencies {
                implementation(project(":openrndr-draw"))
            }

        }

        @Suppress("UNUSED_VARIABLE")
        val jvmMain by getting {
            dependencies {
//                implementation(project(":openrndr-jvm:openrndr-gl3"))
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jsMain by getting {
            dependencies {
//                implementation(project(":openrndr-js:openrndr-webgl"))
            }
        }
    }

}