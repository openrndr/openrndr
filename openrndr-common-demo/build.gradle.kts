@file:Suppress("INACCESSIBLE_TYPE")

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

    jvm {}

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
            val openrndrOS = when (org.gradle.internal.os.OperatingSystem.current()) {
                org.gradle.internal.os.OperatingSystem.WINDOWS -> when (System.getProperty("os.arch")) {
                    "x86-64", "x86_64", "amd64", "x64" -> "windows"
                    "aarch64", "arm-v8" -> "windows-arm64"
                    else -> error("arch not supported")
                }
                org.gradle.internal.os.OperatingSystem.LINUX -> "linux-x64"
                org.gradle.internal.os.OperatingSystem.MAC_OS -> {
                    when (System.getProperty("os.arch")) {
                        "x86-64", "x86_64", "amd64", "x64" -> "macos"
                        "aarch64", "arm-v8" -> "macos-arm64"
                        else -> error("arch not supported")
                    }
                }

                else -> error("platform not supported")

            }

            dependencies {
                runtimeOnly(project(":openrndr-jvm:openrndr-gl3-natives-$openrndrOS"))
                runtimeOnly(project(":openrndr-jvm:openrndr-gl3"))
                runtimeOnly(libs.slf4j.simple)
            }
        }
    }
}