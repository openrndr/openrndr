plugins {
    org.openrndr.convention.`kotlin-jvm`
    org.openrndr.convention.`publish-jvm`
}
dependencies {
    api(project(":openrndr-application"))
    implementation(libs.bundles.lwjgl.openal)
    implementation(libs.ffmpeg)
    implementation(project(":openrndr-jvm:openrndr-openal"))
    implementation(libs.kotlin.coroutines)
}