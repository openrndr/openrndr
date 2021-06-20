plugins {
    kotlin("multiplatform")
}

val lwjglVersion: String by rootProject.extra
val kotlinLanguageVersion: String by rootProject.extra
val kotlinApiVersion: String by rootProject.extra
val kotlinLoggingVersion: String by rootProject.extra

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
                implementation(project (":openrndr-core"))
                implementation(project (":openrndr-math"))
                implementation("org.lwjgl:lwjgl:$lwjglVersion")
                implementation("org.lwjgl:lwjgl-openal:$lwjglVersion")
                implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
            }
        }
//        val jvmTest by getting {
//            dependencies {
//                runtimeOnly(project (":openrndr-jvm:openrndr-openal-natives-windows"))
//                runtimeOnly(project (":openrndr-jvm:openrndr-openal-natives-macos"))
//                runtimeOnly(project (":openrndr-jvm:openrndr-openal-natives-linux-x64"))
//                runtimeOnly("org.slf4j:slf4j-simple:1.7.5")
//            }
//        }
    }
}
