
plugins {
    id("org.openrndr.convention.kotlin-jvm")
    id("org.openrndr.convention.publish-jvm")
    id("org.openrndr.convention.variant")
}

variants {
    val nativeLibs = listOf(libs.lwjgl.stb)

    platform(OperatingSystemFamily.MACOS, MachineArchitecture.ARM64) {
        dependencies {
            nativeLibs.forEach {
                runtimeOnly(it.get().withClassifier("natives-macos-arm64"))
            }
        }
    }
    platform(OperatingSystemFamily.MACOS, MachineArchitecture.X86_64) {
        dependencies {
            runtimeOnly("org.lwjgl:lwjgl-opengl:${libs.versions.lwjgl.get()}:natives-macos")
        }
    }
    platform(OperatingSystemFamily.LINUX, MachineArchitecture.ARM64) {
        dependencies {
            runtimeOnly("org.lwjgl:lwjgl-opengl:${libs.versions.lwjgl.get()}:natives-linux-arm64")
        }
    }
    platform(OperatingSystemFamily.LINUX, MachineArchitecture.X86_64) {
        dependencies {
            runtimeOnly("org.lwjgl:lwjgl-opengl:${libs.versions.lwjgl.get()}:natives-linux")
        }
    }
    platform(OperatingSystemFamily.WINDOWS, MachineArchitecture.ARM64) {
        dependencies {
            runtimeOnly("org.lwjgl:lwjgl-opengl:${libs.versions.lwjgl.get()}:natives-windows-arm64")
        }
    }
    platform(OperatingSystemFamily.WINDOWS, MachineArchitecture.X86_64) {
        dependencies {
            runtimeOnly("org.lwjgl:lwjgl-opengl:${libs.versions.lwjgl.get()}:natives-windows")
        }
    }
}


//
//kotlin {
//    jvm {
//
//        compilations {
//            val main by getting
//
//            val demo by creating {
//                associateWith(main)
//            }
//        }
//
//        testRuns["test"].executionTask {
//            failOnNoDiscoveredTests = false
//            allJvmArgs = allJvmArgs + "-Dorg.openrndr.gl3.skip_glfw_termination"
//            allJvmArgs = allJvmArgs + "-Dorg.openrndr.gl3.debug=true"
//            useJUnitPlatform {
//                if (System.getenv("CI") != null) {
//                    exclude("**/*.class")
//                }
//            }
//            testLogging.exceptionFormat = TestExceptionFormat.FULL
//        }
//        testRuns.create("heavy") {
//
//            setExecutionSourceFrom(
//                testRuns["test"].executionSource.classpath,
//                testRuns["test"].executionSource.testClassesDirs
//            )
//            executionTask {
//                allJvmArgs = allJvmArgs + "-Dorg.openrndr.gl3.skip_glfw_termination"
//                useJUnitPlatform()
//                testLogging.exceptionFormat = TestExceptionFormat.FULL
//            }
//        }
//    }
//    sourceSets {
//        val jvmMain by getting {
//            dependencies {
//                implementation(project(":openrndr-application"))
//                implementation(project(":openrndr-draw"))
//                implementation(project(":openrndr-shape"))
//                implementation(project(":openrndr-binpack"))
//                implementation(project(":openrndr-dds"))
//                implementation(project(":openrndr-extensions"))
//                implementation(project(":openrndr-gl-common"))
//                implementation(libs.kotlin.coroutines)
//                implementation(libs.lwjgl.core)
//                implementation(libs.lwjgl.glfw)
//                implementation(libs.lwjgl.jemalloc)
//                implementation(libs.lwjgl.opengl)
//                implementation(libs.lwjgl.opengles)
//                implementation(libs.lwjgl.stb)
//                implementation(libs.lwjgl.tinyexr)
//                implementation(libs.lwjgl.openal)
//                implementation(libs.lwjgl.egl)
//                implementation(project(":openrndr-filter"))
//            }
//        }
//
//        val jvmTest by getting {
//            dependencies {
//                runtimeOnly(project(":openrndr-jvm:openrndr-gl3-natives-windows"))
//                runtimeOnly(project(":openrndr-jvm:openrndr-gl3-natives-macos"))
//                runtimeOnly(project(":openrndr-jvm:openrndr-gl3-natives-macos-arm64"))
//                runtimeOnly(project(":openrndr-jvm:openrndr-gl3-natives-linux-x64"))
//                runtimeOnly(libs.slf4j.simple)
//            }
//        }
//
//        val jvmDemo by getting {
//            dependencies {
//                runtimeOnly(libs.slf4j.simple)
//                runtimeOnly(project(":openrndr-jvm:openrndr-gl3-natives-macos-arm64"))
//                runtimeOnly(project(":openrndr-jvm:openrndr-gl3"))
//            }
//        }
//    }
//}
dependencies {
    implementation(project(":openrndr-application"))
    implementation(project(":openrndr-draw"))
    implementation(project(":openrndr-shape"))
    implementation(project(":openrndr-binpack"))
    implementation(project(":openrndr-extensions"))
    implementation(libs.kotlin.coroutines)
    implementation(libs.lwjgl.stb)
    api(project(":openrndr-math"))
}

/*
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
 */

