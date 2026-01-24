
plugins {
    id("org.openrndr.convention.kotlin-jvm")
    id("org.openrndr.convention.publish-jvm")
    alias(libs.plugins.kotlin.serialization)
    id("org.openrndr.convention.variant")
}

variants {
    platform(OperatingSystemFamily.MACOS, MachineArchitecture.ARM64) {
        dependencies {
            runtimeOnly(libs.javacpp.get().group + ":" + libs.javacpp.get().name + ":" + libs.javacpp.get().version + ":macosx-arm64")
            runtimeOnly(libs.ffmpeg.get().group + ":" + libs.ffmpeg.get().name + ":" + libs.ffmpeg.get().version + ":macosx-arm64-gpl")
        }
    }
    platform(OperatingSystemFamily.MACOS, MachineArchitecture.X86_64) {
        dependencies {
            runtimeOnly(libs.javacpp.get().group + ":" + libs.javacpp.get().name + ":" + libs.javacpp.get().version + ":macosx-x86_64")
            runtimeOnly(libs.ffmpeg.get().group + ":" + libs.ffmpeg.get().name + ":" + libs.ffmpeg.get().version + ":macosx-x86_64-gpl")
        }
    }
    platform(OperatingSystemFamily.LINUX, MachineArchitecture.ARM64) {
        dependencies {
            runtimeOnly(libs.javacpp.get().group + ":" + libs.javacpp.get().name + ":" + libs.javacpp.get().version + ":linux-arm64")
            runtimeOnly(libs.ffmpeg.get().group + ":" + libs.ffmpeg.get().name + ":" + libs.ffmpeg.get().version + ":linux-arm64")
        }
    }
    platform(OperatingSystemFamily.LINUX, MachineArchitecture.X86_64) {
        dependencies {
            runtimeOnly(libs.javacpp.get().group + ":" + libs.javacpp.get().name + ":" + libs.javacpp.get().version + ":linux-x86_64")
            runtimeOnly(libs.ffmpeg.get().group + ":" + libs.ffmpeg.get().name + ":" + libs.ffmpeg.get().version + ":linux-x86_64")
        }
    }
    platform(OperatingSystemFamily.WINDOWS, MachineArchitecture.ARM64) {
        dependencies {
            runtimeOnly(libs.javacpp.get().group + ":" + libs.javacpp.get().name + ":" + libs.javacpp.get().version + ":windows-arm64")
            runtimeOnly(libs.ffmpeg.get().group + ":" + libs.ffmpeg.get().name + ":" + libs.ffmpeg.get().version + ":windows-arm64-gpl")
        }
    }
    platform(OperatingSystemFamily.WINDOWS, MachineArchitecture.X86_64) {
        dependencies {
            runtimeOnly(libs.javacpp.get().group + ":" + libs.javacpp.get().name + ":" + libs.javacpp.get().version + ":windows-x86_64")
            runtimeOnly(libs.ffmpeg.get().group + ":" + libs.ffmpeg.get().name + ":" + libs.ffmpeg.get().version + ":windows-x86_64-gpl")
        }
    }
}

dependencies {
    api(project(":openrndr-application"))
    implementation(libs.ffmpeg)
    implementation(project(":openrndr-jvm:openrndr-openal"))
    implementation(libs.kotlin.coroutines)
    implementation(libs.kotlin.serialization.core)
    implementation(libs.kotlin.serialization.json)
    implementation(libs.lwjgl.core)
    demoImplementation(project(":openrndr-jvm:openrndr-ffmpeg"))
    demoImplementation(libs.slf4j.simple)
    demoImplementation(project(":openrndr-jvm:openrndr-gl3"))
    demoImplementation(project(":openrndr-jvm:openrndr-application-glfw"))
}

sourceSets {
    val main by getting
    val demo by getting

    demo {
        runtimeClasspath += main.runtimeClasspath
        compileClasspath += main.compileClasspath
    }
}