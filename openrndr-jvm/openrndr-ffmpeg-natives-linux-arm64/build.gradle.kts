plugins {
    kotlin("jvm")
}

val javacpp_version: String by rootProject.extra
val ffmpeg_version: String by rootProject.extra
val os = "linux-arm64"

dependencies {
    runtimeOnly("org.bytedeco:ffmpeg:$ffmpeg_version:$os")
    runtimeOnly("org.bytedeco:javacpp:$javacpp_version:$os")
}
