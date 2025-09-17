plugins {
    id("org.openrndr.convention.kotlin-jvm")
    id("org.openrndr.convention.publish-jvm")
    id("org.openrndr.convention.variant")
}

variants {
    val nativeLibs = listOf(libs.lwjgl.openal)

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
    implementation(project(":openrndr-math"))
    implementation(libs.lwjgl.openal)
}
