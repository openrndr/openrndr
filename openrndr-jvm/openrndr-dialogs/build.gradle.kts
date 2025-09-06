import org.gradle.kotlin.dsl.invoke

plugins {
    org.openrndr.convention.`kotlin-jvm-with-natives`
    org.openrndr.convention.`publish-jvm`
    id("org.openrndr.convention.variant")
}

variants {
    platform( OperatingSystemFamily.MACOS, MachineArchitecture.ARM64) {
        jar {

        }
        dependencies {
            runtimeOnly("org.lwjgl:lwjgl-nfd:${libs.versions.lwjgl.get()}:natives-macos-arm64")
        }
    }
    platform( OperatingSystemFamily.MACOS, MachineArchitecture.X86_64) {
        dependencies {
            runtimeOnly("org.lwjgl:lwjgl-nfd:${libs.versions.lwjgl.get()}:natives-macos")
        }
    }
    platform( OperatingSystemFamily.LINUX, MachineArchitecture.ARM64) {
        dependencies {
            runtimeOnly("org.lwjgl:lwjgl-nfd:${libs.versions.lwjgl.get()}:natives-linux-arm64")
        }
    }
    platform( OperatingSystemFamily.LINUX, MachineArchitecture.X86_64) {
        dependencies {
            runtimeOnly("org.lwjgl:lwjgl-nfd:${libs.versions.lwjgl.get()}:natives-linux")
        }
    }
    platform( OperatingSystemFamily.WINDOWS, MachineArchitecture.ARM64) {
        dependencies {
            runtimeOnly("org.lwjgl:lwjgl-nfd:${libs.versions.lwjgl.get()}:natives-windows-arm64")
        }
    }
    platform( OperatingSystemFamily.WINDOWS, MachineArchitecture.X86_64) {
        dependencies {
            runtimeOnly("org.lwjgl:lwjgl-nfd:${libs.versions.lwjgl.get()}:natives-windows")
        }
    }
}

dependencies {
    implementation(project(":openrndr-application"))
    implementation(project(":openrndr-event"))
    implementation(libs.lwjgl.nfd)
}

