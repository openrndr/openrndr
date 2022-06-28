plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = libs.versions.jvmTarget.get()
            kotlinOptions.apiVersion = libs.versions.kotlinApi.get()
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    sourceSets {
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(project(":openrndr-application"))
                implementation(project(":openrndr-math"))
                implementation(project(":openrndr-color"))
                implementation(libs.jsoup)
                implementation(libs.kotlin.logging)
            }
        }

        val jvmTest by getting {
            dependencies {
                runtimeOnly(project(":openrndr-jvm:openrndr-gl3-natives-windows"))
                runtimeOnly(project(":openrndr-jvm:openrndr-gl3-natives-macos"))
                runtimeOnly(project(":openrndr-jvm:openrndr-gl3-natives-linux-x64"))
                runtimeOnly(libs.slf4j.simple)
                runtimeOnly(project(":openrndr-nullgl"))
            }
        }
    }
}