plugins {
    id("org.openrndr.convention.kotlin-multiplatform-js")
    id("org.openrndr.convention.publish-multiplatform")
}

kotlin {
    sourceSets {
        getByName("webMain") {
            dependencies {
                api(project(":openrndr-application"))
                api(project(":openrndr-draw"))
                implementation(project(":openrndr-gl-common"))
                implementation(libs.kotlin.coroutines)
                implementation(libs.kotlin.js)
                implementation(libs.kotlin.browser)
                implementation(libs.kotlin.web)
                implementation(libs.kotlin.stdlib)
            }
        }
        getByName("wasmJsMain") {
            dependencies {
                implementation(libs.kotlin.stdlib)
            }
        }
    }
}