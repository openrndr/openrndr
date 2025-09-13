plugins {
    id("org.openrndr.convention.kotlin-multiplatform")
    id("org.openrndr.convention.publish-multiplatform")
}

kotlin {
    sourceSets {
        val jsMain by getting {
            dependencies {
                api(project(":openrndr-application"))
                api(project(":openrndr-draw"))
                implementation(project(":openrndr-gl-common"))
                implementation(libs.kotlin.coroutines)
            }
        }
    }
}