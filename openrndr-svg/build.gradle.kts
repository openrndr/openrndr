plugins {
    kotlin("multiplatform")
}

val lwjglVersion: String by rootProject.extra
val kotlinLanguageVersion: String by rootProject.extra
val kotlinLoggingVersion: String by rootProject.extra
val kotlinJvmTarget: String by rootProject.extra
val kotlinApiVersion: String by rootProject.extra
val jsoupVersion: String by rootProject.extra

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = kotlinJvmTarget
            kotlinOptions.apiVersion = kotlinApiVersion
            kotlinOptions.languageVersion = kotlinLanguageVersion
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
                implementation("org.jsoup:jsoup:$jsoupVersion")
                implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
            }
        }

        val jvmTest by getting {
            dependencies {
                runtimeOnly(project(":openrndr-jvm:openrndr-gl3-natives-windows"))
                runtimeOnly(project(":openrndr-jvm:openrndr-gl3-natives-macos"))
                runtimeOnly(project(":openrndr-jvm:openrndr-gl3-natives-linux-x64"))
                runtimeOnly("org.slf4j:slf4j-simple:1.7.30")
                runtimeOnly(project(":openrndr-nullgl"))
            }
        }
    }
}