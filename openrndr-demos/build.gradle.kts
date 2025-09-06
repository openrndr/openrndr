@file:Suppress("INACCESSIBLE_TYPE")


plugins {
    org.openrndr.convention.`kotlin-jvm`
}
configurations.matching(Configuration::isCanBeResolved).configureEach {
    attributes {
        attribute(OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE, objects.named("macos"))
        attribute(MachineArchitecture.ARCHITECTURE_ATTRIBUTE, objects.named("aarch64"))
//        attribute(osAttribute, "macos")
//        attribute(archAttribute, "arm64")
    }
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
//    runtimeOnly(project(":openrndr-jvm:openrndr-ffmpeg"))
    implementation(libs.kotlin.coroutines)
    implementation(libs.kotlin.reflect)
    runtimeOnly(libs.slf4j.simple)
}
