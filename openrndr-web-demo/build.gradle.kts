plugins {
    id("org.openrndr.convention.kotlin-multiplatform")
    id("org.openrndr.convention.kotlin-multiplatform-js")
}

kotlin {
    js {
        browser {
            commonWebpackConfig {
                outputFileName = "openrndr-program.js"
                cssSupport {
                    enabled.set(true)
                }
            }
        }
        binaries.executable()
    }
    wasmJs {
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
        getByName("webMain") {
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