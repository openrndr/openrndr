plugins {
    org.openrndr.convention.`kotlin-multiplatform`
}

data class JvmNativeVariant(val targetName: String, val os: String, val arch: String)

val jvmNativeVariants = listOf(
    JvmNativeVariant("natives-linux-arm64", OperatingSystemFamily.LINUX, "arm64"),
    JvmNativeVariant("natives-linux-x86_64", OperatingSystemFamily.LINUX, "x86-64"),
    JvmNativeVariant("natives-macos-arm64", OperatingSystemFamily.MACOS, "arm64"),
    JvmNativeVariant("natives-macos", OperatingSystemFamily.MACOS, "x86-64"),
    JvmNativeVariant("natives-windows", OperatingSystemFamily.WINDOWS, "x86-64")
)

kotlin {
    for ((targetName, os, arch) in jvmNativeVariants) {
        jvm(targetName) {
            attributes.attribute(OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE, objects.named(os))
            attributes.attribute(MachineArchitecture.ARCHITECTURE_ATTRIBUTE, objects.named(arch))
        }
        configurations[targetName + "ApiElements"].attributes {
            attribute(OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE, objects.named(os))
            attribute(MachineArchitecture.ARCHITECTURE_ATTRIBUTE, objects.named(arch))
        }
        configurations[targetName + "RuntimeElements"].attributes {
            attribute(OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE, objects.named(os))
            attribute(MachineArchitecture.ARCHITECTURE_ATTRIBUTE, objects.named(arch))
        }
    }
    sourceSets {
        @Suppress("UNUSED_VARIABLE")
        val jvmMain by getting {
            dependencies {
                implementation(project(":openrndr-math"))
                implementation(libs.bundles.lwjgl.openal)
            }
        }
        for ((targetName, _, _) in jvmNativeVariants) {
            getByName(targetName + "Main") {
                dependencies {
                    runtimeOnly(libs.bundles.lwjgl.openal)
                }
            }
        }
    }
}