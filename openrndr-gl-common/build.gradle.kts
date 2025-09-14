plugins {
    id("org.openrndr.convention.kotlin-multiplatform")
    id("org.openrndr.convention.publish-multiplatform")
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":openrndr-draw"))
            }

        }

        val jvmMain by getting {
        }

        val jsMain by getting {
        }
    }

}