plugins {
    kotlin("jvm")
}

val lwjglVersion: String by rootProject.extra
val ffmpeg_version: String by rootProject.extra

dependencies {
    api(project(":openrndr-application"))
    implementation("org.lwjgl:lwjgl:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-openal:$lwjglVersion")
    implementation("org.bytedeco:ffmpeg:$ffmpeg_version")
    implementation(project(":openrndr-core"))
    implementation(project(":openrndr-jvm:openrndr-openal"))
}
