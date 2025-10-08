plugins {
    id("org.openrndr.convention.kotlin-multiplatform")
}

kotlin {
//    js(IR) {
//        browser {
//            commonWebpackConfig {
//                outputFileName = "openrndr-program.js"
//                cssSupport {
//                    enabled.set(true)
//                }
//            }
//        }
//        binaries.executable()
//    }
    wasmJs() {
        browser {
            commonWebpackConfig {
                outputFileName = "openrndr-program.js"
                cssSupport {
                    enabled.set(true)
                }
            }
            binaries.executable()
        }
    }

    sourceSets {
        val wasmJsMain by getting {
            dependencies {
                implementation(project(":openrndr-application"))
                implementation(project(":openrndr-draw"))
                implementation(project(":openrndr-gl-common"))
                implementation(project(":openrndr-js:openrndr-webgl"))
                implementation(libs.kotlin.coroutines)
                implementation(libs.kotlin.logging)
            }
        }
    }
}