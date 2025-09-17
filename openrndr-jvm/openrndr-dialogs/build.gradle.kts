plugins {
    id("org.openrndr.convention.kotlin-jvm")
    id("org.openrndr.convention.publish-jvm")
    id("org.openrndr.convention.variant")
}

variants {
    val nativeLibs = listOf(libs.lwjgl.nfd)

    platform( OperatingSystemFamily.MACOS, MachineArchitecture.ARM64) {
        dependencies {
            nativeLibs.forEach {
                runtimeOnly(it.get().withClassifier("natives-macos-arm64"))
            }
        }
    }
    platform( OperatingSystemFamily.MACOS, MachineArchitecture.X86_64) {
        dependencies {
            nativeLibs.forEach {
                runtimeOnly(it.get().withClassifier("natives-macos"))
            }
        }
    }
    platform( OperatingSystemFamily.LINUX, MachineArchitecture.ARM64) {
        dependencies {
            nativeLibs.forEach {
                runtimeOnly(it.get().withClassifier("natives-linux-arm64"))
            }
        }
    }
    platform( OperatingSystemFamily.LINUX, MachineArchitecture.X86_64) {
        dependencies {
            nativeLibs.forEach {
                runtimeOnly(it.get().withClassifier("natives-linux"))
            }
        }
    }
    platform( OperatingSystemFamily.WINDOWS, MachineArchitecture.ARM64) {
        dependencies {
            nativeLibs.forEach {
                runtimeOnly(it.get().withClassifier("natives-windows-arm64"))
            }
        }
    }
    platform( OperatingSystemFamily.WINDOWS, MachineArchitecture.X86_64) {
        dependencies {
            nativeLibs.forEach {
                runtimeOnly(it.get().withClassifier("natives-windows"))
            }
        }
    }
}

val main by sourceSets.getting

dependencies {
    implementation(project(":openrndr-application"))
    implementation(project(":openrndr-event"))
    implementation(libs.lwjgl.nfd)
    "demoImplementation"(main.output)
    //"demoImplementation"(main.runtimeClasspath)
}

