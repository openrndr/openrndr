plugins {
    id("org.openrndr.convention.kotlin-multiplatform")
    id("org.openrndr.convention.kotlin-multiplatform-js")
    id("org.openrndr.convention.publish-multiplatform")
}

kotlin {
    sourceSets {
        getByName("commonMain") {
            dependencies {
                implementation(project(":openrndr-math"))
                implementation(project(":openrndr-draw"))
                implementation(project(":openrndr-utils"))
                implementation(libs.kotlin.coroutines)
            }
        }
        getByName("jsMain") {
            dependencies {
                implementation(libs.kotlin.js)
                implementation(libs.kotlin.browser)
                implementation(libs.kotlin.web)
            }
        }
    }
}