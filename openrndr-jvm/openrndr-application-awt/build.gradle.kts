plugins {
    id("org.openrndr.convention.kotlin-jvm")
    id("org.openrndr.convention.publish-jvm")
    id("org.openrndr.convention.variant")
}

repositories {
    // Compose, used by the demos, depends on AndroidX libraries
    google()
}

variants {
    val nativeLibs = listOf(libs.lwjgl.opengl, libs.lwjgl.opengles)

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
    implementation(project(":openrndr-jvm:openrndr-textshapingdriver-harfbuzz"))
    implementation(project(":openrndr-bidi-text-driver"))
    implementation(project(":openrndr-jvm:openrndr-fontdriver-freetype"))
    implementation(project(":openrndr-jvm:openrndr-imagedriver-stb"))
    implementation(project(":openrndr-jvm:openrndr-gl3"))

    implementation(libs.kotlin.coroutines)
    implementation(libs.lwjgl3.awt)
    implementation(libs.lwjgl.jawt)
    implementation(libs.lwjgl.egl)
    implementation(libs.lwjgl.opengl)
    implementation(libs.lwjgl.opengles)

    demoImplementation(libs.slf4j.simple)

    // Compose Desktop for DemoCompose01
    val os = System.getProperty("os.name").lowercase()
    val arm = System.getProperty("os.arch") in setOf("aarch64", "arm64")
    demoImplementation(
        when {
            os.contains("mac") -> if (arm) libs.compose.desktop.macos.arm64 else libs.compose.desktop.macos.x64
            os.contains("win") -> libs.compose.desktop.windows.x64
            else -> if (arm) libs.compose.desktop.linux.arm64 else libs.compose.desktop.linux.x64
        }
    )
    demoImplementation(libs.compose.material)
    // the Compose compiler plugin only compiles the demos, the main compilation has no Compose runtime
    "kotlinCompilerPluginClasspathDemo"(libs.kotlin.compose.compiler.plugin)
}

sourceSets {
    val main = getByName("main")
    getByName("demo") {
        runtimeClasspath += main.runtimeClasspath + main.output
        compileClasspath += main.compileClasspath + main.output
    }
}
