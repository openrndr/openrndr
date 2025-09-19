import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

plugins {
    id("org.openrndr.convention.kotlin-jvm")
    id("org.openrndr.convention.publish-jvm")
    id("org.openrndr.convention.variant")
}


tasks {
    @Suppress("UNUSED_VARIABLE")
    val test by getting(Test::class) {
        onlyIf { !project.hasProperty("skip.gl3.tests") }

        if (DefaultNativePlatform.getCurrentOperatingSystem().isMacOsX) {
            allJvmArgs = allJvmArgs + "-XstartOnFirstThread"
        }
        useJUnitPlatform()
        testLogging.exceptionFormat = TestExceptionFormat.FULL
    }
}

variants {
    val nativeLibs = listOf(libs.lwjgl.stb, libs.lwjgl.tinyexr)

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
    implementation(project(":openrndr-utils"))
    implementation(project(":openrndr-draw"))
    implementation(project(":openrndr-dds"))
    implementation(libs.lwjgl.core)
    implementation(libs.lwjgl.stb)
    implementation(libs.lwjgl.tinyexr)
    testImplementation(project(":openrndr-application"))
    testImplementation(project(":openrndr-jvm:openrndr-gl3"))
    testRuntimeOnly(project(":openrndr-jvm:openrndr-application-glfw"))
}
