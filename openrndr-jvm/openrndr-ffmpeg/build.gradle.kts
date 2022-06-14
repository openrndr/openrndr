dependencies {
    api(project(":openrndr-application"))
    implementation(libs.bundles.lwjgl.openal)
    implementation(libs.ffmpeg)
    implementation(project(":openrndr-core"))
    implementation(project(":openrndr-jvm:openrndr-openal"))
}
