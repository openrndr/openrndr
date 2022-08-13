plugins {
    org.openrndr.convention.`kotlin-jvm`
}
dependencies {
    api(project(":openrndr-application"))
    implementation(libs.bundles.lwjgl.openal)
    implementation(libs.ffmpeg)
    implementation(project(":openrndr-jvm:openrndr-openal"))
}