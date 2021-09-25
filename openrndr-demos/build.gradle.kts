import org.gradle.internal.os.OperatingSystem

plugins {
    kotlin("jvm")
}

val openrndrOS = when (OperatingSystem.current()) {
    OperatingSystem.WINDOWS -> "windows"
    OperatingSystem.MAC_OS -> "macos"
    else -> "linux-x64"
}

dependencies {
    runtimeOnly(project(":openrndr-jvm:openrndr-gl3"))
    implementation(project(":openrndr-filter"))
    implementation(project(":openrndr-extensions"))
    runtimeOnly(project(":openrndr-jvm:openrndr-gl3-natives-$openrndrOS"))
    runtimeOnly(project(":openrndr-jvm:openrndr-ffmpeg-natives-$openrndrOS"))
    implementation(project(":openrndr-jvm:openrndr-ffmpeg"))
    runtimeOnly("org.slf4j:slf4j-simple:1.7.29")
}