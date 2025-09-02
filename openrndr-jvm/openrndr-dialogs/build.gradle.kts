plugins {
    org.openrndr.convention.`kotlin-jvm-with-natives`
    org.openrndr.convention.`publish-jvm`
}

dependencies {
//    "windowsX64MainRuntimeOnly"("org.lwjgl","lwjgl-nfd", version="${libs.versions.lwjgl.get()}", classifier="natives-windows")
//    "windowsArm64MainRuntimeOnly"("org.lwjgl","lwjgl-nfd", version="${libs.versions.lwjgl.get()}", classifier="natives-windows-arm64")
//    "macosX64MainRuntimeOnly"("org.lwjgl","lwjgl-nfd", version="${libs.versions.lwjgl.get()}", classifier="natives-macos")
    "macosArm64MainImplementation"("org.lwjgl","lwjgl-nfd", version="${libs.versions.lwjgl.get()}", classifier="natives-macos-arm64")
//    "linuxX64MainRuntimeOnly"("org.lwjgl","lwjgl-nfd", version="${libs.versions.lwjgl.get()}", classifier="natives-linux")
//    "linuxArm64MainRuntimeOnly"("org.lwjgl","lwjgl-nfd", version="${libs.versions.lwjgl.get()}", classifier="natives-linux-arm64")

    implementation(project(":openrndr-application"))
    implementation(project(":openrndr-event"))
    implementation(libs.lwjgl.nfd)
}

