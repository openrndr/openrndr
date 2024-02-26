plugins {
    org.openrndr.convention.`kotlin-multiplatform`
}

kotlin {

    js(IR) {

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
    jvm {


    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":openrndr-application"))
                implementation(project(":openrndr-draw"))
                implementation(libs.kotlin.coroutines)
            }
        }

        val jsMain by getting {
            dependencies {
                implementation(project(":openrndr-gl-common"))
                implementation(project(":openrndr-js:openrndr-webgl"))
            }
        }

        val jvmMain by getting {
            dependencies {
                runtimeOnly(project(":openrndr-jvm:openrndr-gl3-natives-macos-arm64"))
                runtimeOnly(project(":openrndr-jvm:openrndr-gl3"))
                runtimeOnly(libs.slf4j.simple)
            }
        }

    }
}