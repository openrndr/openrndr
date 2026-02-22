plugins {
    id("org.openrndr.convention.kotlin-multiplatform")
    id("org.openrndr.convention.publish-multiplatform")
}

kotlin {
    sourceSets {
        val androidMain by getting {
            dependencies {
                api(project(":openrndr-application"))
                api(project(":openrndr-color"))
                api(project(":openrndr-draw"))
                api(project(":openrndr-jvm:openrndr-gl3"))
                api(project(":openrndr-jvm:openrndr-fontdriver-android"))
                api(project(":openrndr-math"))
                implementation(project(":openrndr-gl-common"))
                implementation(libs.kotlin.coroutines)
            }
        }
    }

}