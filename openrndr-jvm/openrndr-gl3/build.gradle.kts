plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
            if (System.getenv("CI") == "true") {
                exclude("**/*.class")
            }
        }
    }

    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(project(":openrndr-application"))
                implementation(project(":openrndr-draw"))
                implementation(project(":openrndr-shape"))
                implementation(project(":openrndr-binpack"))
                implementation(project(":openrndr-dds"))
                implementation(libs.kotlin.coroutines)
                implementation(libs.kotlin.logging)
                implementation(libs.lwjgl.core)
                implementation(libs.lwjgl.glfw)
                implementation(libs.lwjgl.jemalloc)
                implementation(libs.lwjgl.opengl)
                implementation(libs.lwjgl.stb)
                implementation(libs.lwjgl.tinyexr)
                implementation(libs.lwjgl.openal)
                implementation(libs.lwjgl.egl)
            }
        }

        val jvmTest by getting {
            dependencies {
                runtimeOnly(project(":openrndr-jvm:openrndr-gl3-natives-windows"))
                runtimeOnly(project(":openrndr-jvm:openrndr-gl3-natives-macos"))
                runtimeOnly(project(":openrndr-jvm:openrndr-gl3-natives-linux-x64"))
                runtimeOnly(libs.slf4j.simple)
                implementation(libs.spek.dsl)
                implementation(libs.kluent)

                implementation(kotlin("test-annotations-common"))
                implementation(kotlin("test-junit5"))
                runtimeOnly(libs.bundles.jupiter)
            }
        }
    }
}

