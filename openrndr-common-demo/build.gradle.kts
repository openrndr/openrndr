@file:Suppress("INACCESSIBLE_TYPE")

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

    jvm {}

    sourceSets {
        getByName("commonMain") {
            dependencies {
                implementation(project(":openrndr-application"))
                implementation(project(":openrndr-draw"))
                implementation(libs.kotlin.coroutines)
            }
        }

        getByName("jsMain") {
            dependencies {
                implementation(project(":openrndr-gl-common"))
                implementation(project(":openrndr-js:openrndr-webgl"))
            }
        }

        getByName("jvmMain") {
            dependencies {
                runtimeOnly(project(":openrndr-jvm:openrndr-application-glfw"))
                runtimeOnly(libs.slf4j.simple)
            }
        }
    }
}