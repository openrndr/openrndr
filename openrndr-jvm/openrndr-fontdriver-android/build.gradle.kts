plugins {
    id("org.openrndr.convention.kotlin-multiplatform")
    id("org.openrndr.convention.publish-multiplatform")
}

kotlin {
    sourceSets {
        val androidMain by getting {
            dependencies {
                api(project(":openrndr-draw"))
            }
        }
    }

}