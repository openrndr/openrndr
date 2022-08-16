import org.openrndr.convention.currentOperatingSystemName

plugins {
    org.openrndr.convention.`kotlin-jvm`
}

val nativeClassifier = when (currentOperatingSystemName) {
    "linux" -> "linux-x64"
    "macos", "windows" -> currentOperatingSystemName
    else -> throw IllegalStateException("Unknown OS: $currentOperatingSystemName")
}

dependencies {
    implementation(project(":openrndr-jvm:openrndr-gl3"))
    implementation(project(":openrndr-filter"))
    implementation(project(":openrndr-extensions"))
    runtimeOnly(project(":openrndr-jvm:openrndr-gl3", "jvmRuntimeElements"))
    runtimeOnly(project(":openrndr-jvm:openrndr-gl3", "natives-${nativeClassifier}RuntimeElements"))
    runtimeOnly(project(":openrndr-jvm:openrndr-openal", "jvmRuntimeElements"))
    runtimeOnly(project(":openrndr-jvm:openrndr-openal", "natives-${nativeClassifier}RuntimeElements"))
    runtimeOnly(project(":openrndr-jvm:openrndr-ffmpeg-natives-$nativeClassifier"))
    implementation(project(":openrndr-jvm:openrndr-ffmpeg"))
    implementation(libs.kotlin.coroutines)
    runtimeOnly(libs.slf4j.simple)
}