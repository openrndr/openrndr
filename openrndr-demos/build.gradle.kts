@file:Suppress("INACCESSIBLE_TYPE")


plugins {
    org.openrndr.convention.`kotlin-jvm`
}


dependencies {
    implementation(project(":openrndr-application"))
    implementation(project(":openrndr-draw"))
    implementation(project(":openrndr-filter"))
    implementation(project(":openrndr-extensions"))
    implementation(project(":openrndr-jvm:openrndr-dialogs"))
    implementation(project(":openrndr-jvm:openrndr-gl3"))
    implementation(project(":openrndr-jvm:openrndr-openal"))
    implementation(project(":openrndr-jvm:openrndr-ffmpeg"))
    implementation(libs.kotlin.coroutines)
    implementation(libs.kotlin.reflect)
    runtimeOnly(libs.slf4j.simple)
}
