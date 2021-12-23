plugins {
    kotlin("multiplatform")
}

val lwjglVersion: String by rootProject.extra
val kotlinJvmTarget: String by rootProject.extra
val kotlinLanguageVersion: String by rootProject.extra
val kotlinApiVersion: String by rootProject.extra
val kluentVersion: String by rootProject.extra
val spekVersion: String by rootProject.extra
val kotlinLoggingVersion: String by rootProject.extra
val kotlinxCoroutinesVersion: String by rootProject.extra
val kotlinxSerializationVersion: String by rootProject.extra
val kotestVersion: String by rootProject.extra
val junitJupiterVersion: String by rootProject.extra


kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = kotlinJvmTarget
            kotlinOptions.apiVersion = kotlinApiVersion
            kotlinOptions.languageVersion = kotlinLanguageVersion
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
//            exclude("**/*.class")
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
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")

                implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
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
                implementation("org.spekframework.spek2:spek-dsl-jvm:$spekVersion")
                implementation("org.amshove.kluent:kluent:$kluentVersion")

                implementation(kotlin("test-annotations-common"))
                implementation(kotlin("test-junit5"))
                runtimeOnly("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
                runtimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")


            }
        }
    }
}

