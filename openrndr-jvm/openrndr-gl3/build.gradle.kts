plugins {
    kotlin("multiplatform")
}

val lwjglVersion: String by rootProject.extra
val kotlinLanguageVersion: String by rootProject.extra
val kotlinApiVersion: String by rootProject.extra


kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
            kotlinOptions.apiVersion = kotlinApiVersion
            kotlinOptions.languageVersion = kotlinLanguageVersion
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
            exclude("**/*.class")
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
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0-RC")

                implementation("io.github.microutils:kotlin-logging:2.0.6")
                implementation("org.lwjgl:lwjgl:$lwjglVersion")
                implementation("org.lwjgl:lwjgl-glfw:$lwjglVersion")
                implementation("org.lwjgl:lwjgl-jemalloc:$lwjglVersion")
                implementation("org.lwjgl:lwjgl-openal:$lwjglVersion")
                implementation("org.lwjgl:lwjgl-opengl:$lwjglVersion")
                implementation("org.lwjgl:lwjgl-stb:$lwjglVersion")
                implementation("org.lwjgl:lwjgl-egl:$lwjglVersion")
                implementation("org.lwjgl:lwjgl-tinyexr:$lwjglVersion")
            }
        }

        val jvmTest by getting {
            dependencies {
                runtimeOnly(project(":openrndr-jvm:openrndr-gl3-natives-windows"))
                runtimeOnly(project(":openrndr-jvm:openrndr-gl3-natives-macos"))
                runtimeOnly(project(":openrndr-jvm:openrndr-gl3-natives-linux-x64"))
                runtimeOnly("org.slf4j:slf4j-simple:1.7.30")
            }
        }
    }
}

