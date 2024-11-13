@file:Suppress("INACCESSIBLE_TYPE")

plugins {
    org.openrndr.convention.`kotlin-jvm`
}

val openrndrOS = when(org.gradle.internal.os.OperatingSystem.current()) {
    org.gradle.internal.os.OperatingSystem.WINDOWS -> when (System.getProperty("os.arch")) {
        "x86-64", "x86_64", "amd64", "x64" -> "windows"
        "aarch64", "arm-v8" -> "windows-arm64"
        else -> error("arch not supported")
    }
    org.gradle.internal.os.OperatingSystem.LINUX -> when (System.getProperty("os.arch")) {
        "x86-64", "x86_64", "amd64", "x64" -> "linux-x64"
        "aarch64", "arm-v8" -> "linux-arm64"
        else -> error("arch not supported")
    }
    org.gradle.internal.os.OperatingSystem.MAC_OS -> {
        when (System.getProperty("os.arch")) {
            "x86-64", "x86_64", "amd64", "x64" -> "macos"
            "aarch64", "arm-v8" -> "macos-arm64"
            else -> error("arch not supported")
        }
    }
    else -> error("platform not supported")

}

dependencies {
    implementation(project(":openrndr-jvm:openrndr-gl3"))
    implementation(project(":openrndr-filter"))
    implementation(project(":openrndr-extensions"))
    implementation(project(":openrndr-jvm:openrndr-dialogs"))
    runtimeOnly(project(":openrndr-jvm:openrndr-gl3-natives-$openrndrOS"))
    runtimeOnly(project(":openrndr-jvm:openrndr-openal-natives-$openrndrOS"))
    runtimeOnly(project(":openrndr-jvm:openrndr-ffmpeg-natives-$openrndrOS"))
    implementation(project(":openrndr-jvm:openrndr-ffmpeg"))
    implementation(libs.kotlin.coroutines)
    implementation(libs.kotlin.reflect)
    runtimeOnly(libs.slf4j.simple)
}