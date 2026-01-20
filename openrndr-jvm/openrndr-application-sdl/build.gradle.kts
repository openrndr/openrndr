plugins {
    id("org.openrndr.convention.kotlin-jvm")
    id("org.openrndr.convention.publish-jvm")
    id("org.openrndr.convention.variant")
}

variants {
    val nativeLibs = listOf(libs.lwjgl.sdl)

    platform(OperatingSystemFamily.MACOS, MachineArchitecture.ARM64) {
        dependencies {
            nativeLibs.forEach {
                runtimeOnly(it.get().withClassifier("natives-macos-arm64"))
            }
        }
    }
    platform(OperatingSystemFamily.MACOS, MachineArchitecture.X86_64) {
        dependencies {
            nativeLibs.forEach {
                runtimeOnly(it.get().withClassifier("natives-macos"))
            }
        }
    }
    platform(OperatingSystemFamily.LINUX, MachineArchitecture.ARM64) {
        dependencies {
            nativeLibs.forEach {
                runtimeOnly(it.get().withClassifier("natives-linux-arm64"))
            }
        }
    }
    platform(OperatingSystemFamily.LINUX, MachineArchitecture.X86_64) {
        dependencies {
            nativeLibs.forEach {
                runtimeOnly(it.get().withClassifier("natives-linux"))
            }
        }
    }
    platform(OperatingSystemFamily.WINDOWS, MachineArchitecture.ARM64) {
        dependencies {
            nativeLibs.forEach {
                runtimeOnly(it.get().withClassifier("natives-windows-arm64"))
            }
        }
    }
    platform(OperatingSystemFamily.WINDOWS, MachineArchitecture.X86_64) {
        dependencies {
            nativeLibs.forEach {
                runtimeOnly(it.get().withClassifier("natives-windows"))
            }
        }
    }
}


dependencies {
    implementation(project(":openrndr-application"))
    implementation(project(":openrndr-jvm:openrndr-fontdriver-stb"))
    implementation(project(":openrndr-jvm:openrndr-imagedriver-stb"))
    implementation(project(":openrndr-jvm:openrndr-gl3"))

    implementation(libs.kotlin.coroutines)
    implementation(libs.lwjgl.sdl)
    implementation(libs.lwjgl.opengl)
    implementation(libs.lwjgl.opengles)

    demoImplementation(libs.slf4j.simple)
}

sourceSets {
    val main by getting
    val demo by getting

    demo {
        runtimeClasspath += main.runtimeClasspath
        compileClasspath += main.compileClasspath
    }
}

