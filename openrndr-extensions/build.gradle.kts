plugins {
    id("org.openrndr.convention.kotlin-multiplatform")
    id("org.openrndr.convention.kotlin-multiplatform-js")
    id("org.openrndr.convention.publish-multiplatform")
}

kotlin {
    sourceSets {
        val commonMain = getByName("commonMain") {
            dependencies {
                implementation(project(":openrndr-application"))
                implementation(project(":openrndr-draw"))
            }
        }

        val jsMain = getByName("jsMain") {
            dependencies {
                implementation(project(":openrndr-js:openrndr-webgl"))
            }
        }
    }
}