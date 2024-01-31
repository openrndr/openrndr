import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    org.openrndr.convention.`kotlin-multiplatform`
}

kotlin {
    jvm {

        compilations {
            val main by getting

            @Suppress("UNUSED_VARIABLE")
            val demo by creating {
                associateWith(main)
            }
        }

        testRuns["test"].executionTask {
            useJUnitPlatform {
                if (System.getenv("CI") != null) {
                    exclude("**/*.class")
                }
            }
            testLogging.exceptionFormat = TestExceptionFormat.FULL
        }
        testRuns.create("heavy") {
            setExecutionSourceFrom(
                testRuns["test"].executionSource.classpath,
                testRuns["test"].executionSource.testClassesDirs
            )
            executionTask {
                useJUnitPlatform()
                testLogging.exceptionFormat = TestExceptionFormat.FULL
            }
        }
    }
    sourceSets {
        @Suppress("UNUSED_VARIABLE")
        val jvmMain by getting {
            dependencies {
                implementation(project(":openrndr-application"))
                implementation(project(":openrndr-draw"))
                implementation(project(":openrndr-shape"))
                implementation(project(":openrndr-binpack"))
                implementation(project(":openrndr-dds"))
                implementation(project(":openrndr-extensions"))
                implementation(project(":openrndr-gl-common"))
                implementation(libs.kotlin.coroutines)
                implementation(libs.lwjgl.core)
                implementation(libs.lwjgl.glfw)
                implementation(libs.lwjgl.jemalloc)
                implementation(libs.lwjgl.opengl)
                implementation(libs.lwjgl.opengles)
                implementation(libs.lwjgl.stb)
                implementation(libs.lwjgl.tinyexr)
                implementation(libs.lwjgl.openal)
                implementation(libs.lwjgl.egl)
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jvmTest by getting {
            dependencies {
                runtimeOnly(project(":openrndr-jvm:openrndr-gl3-natives-windows"))
                runtimeOnly(project(":openrndr-jvm:openrndr-gl3-natives-macos"))
                runtimeOnly(project(":openrndr-jvm:openrndr-gl3-natives-macos-arm64"))
                runtimeOnly(project(":openrndr-jvm:openrndr-gl3-natives-linux-x64"))
                runtimeOnly(libs.slf4j.simple)
            }
        }

        val jvmDemo by getting {
            dependencies {
                runtimeOnly(libs.slf4j.simple)
                runtimeOnly(project(":openrndr-jvm:openrndr-gl3-natives-macos-arm64"))
                runtimeOnly(project(":openrndr-jvm:openrndr-gl3"))
            }
        }
    }
}

gradle.taskGraph.whenReady {
    // Exclude heavy tests when running allTests
    if (allTasks.any { it.name == "allTests" }) {
        tasks["jvmHeavyTest"].enabled = false
    }
}

kotlin {
    jvm().mainRun {
        classpath(kotlin.jvm().compilations.getByName("demo").output.allOutputs)
        classpath(kotlin.jvm().compilations.getByName("demo").configurations.runtimeDependencyConfiguration!!)
    }
}

tasks.withType<JavaExec>().matching { it.name == "jvmRun" }.configureEach { workingDir = rootDir }