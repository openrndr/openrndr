plugins {
    org.openrndr.convention.`kotlin-multiplatform`
    org.openrndr.convention.`publish-multiplatform`
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":openrndr-application"))
                implementation(project(":openrndr-draw"))
            }
        }

        val jsMain by getting {
            dependencies {
                implementation(project(":openrndr-js:openrndr-webgl"))
            }
        }
    }
}