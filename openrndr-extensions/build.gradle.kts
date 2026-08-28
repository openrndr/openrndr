plugins {
    id("org.openrndr.convention.kotlin-multiplatform")
    id("org.openrndr.convention.kotlin-multiplatform-js")
    id("org.openrndr.convention.publish-multiplatform")
}

kotlin {
    sourceSets {
        getByName("commonMain") {
            dependencies {
                implementation(project(":openrndr-application"))
                implementation(project(":openrndr-draw"))
            }
        }

        getByName("jsMain") {
            dependencies {
                implementation(project(":openrndr-js:openrndr-webgl"))
            }
        }
    }
}