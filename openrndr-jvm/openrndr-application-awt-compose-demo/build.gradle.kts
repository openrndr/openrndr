// Demos of OPENRNDR programs in Compose Desktop user interfaces, this module is not published
plugins {
    id("org.openrndr.convention.kotlin-jvm")
    alias(libs.plugins.kotlin.compose)
}

repositories {
    // Compose depends on AndroidX libraries
    google()
}

dependencies {
    implementation(project(":openrndr-application"))
    implementation(project(":openrndr-draw"))
    implementation(project(":openrndr-math"))
    implementation(project(":openrndr-color"))
    implementation(project(":openrndr-jvm:openrndr-application-awt"))
    implementation(libs.slf4j.simple)

    val os = System.getProperty("os.name").lowercase()
    val arm = System.getProperty("os.arch") in setOf("aarch64", "arm64")
    implementation(
        when {
            os.contains("mac") -> if (arm) libs.compose.desktop.macos.arm64 else libs.compose.desktop.macos.x64
            os.contains("win") -> libs.compose.desktop.windows.x64
            else -> if (arm) libs.compose.desktop.linux.arm64 else libs.compose.desktop.linux.x64
        }
    )
    implementation(libs.compose.material)
}
