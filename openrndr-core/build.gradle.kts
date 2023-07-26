import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

plugins {
    org.openrndr.convention.`kotlin-jvm`
}
dependencies {
    api(project(":openrndr-color"))
    api(project(":openrndr-draw"))
    api(project(":openrndr-math"))
    api(project(":openrndr-shape"))
    api(project(":openrndr-event"))
    api(project(":openrndr-animatable"))
    implementation(project(":openrndr-application"))
    testRuntimeOnly(project(":openrndr-nullgl"))
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.kotest.runner)
    testImplementation(libs.kluent)
}

tasks {
    test {
        if (DefaultNativePlatform.getCurrentOperatingSystem().isMacOsX) {
            allJvmArgs = allJvmArgs + "-XstartOnFirstThread"
        }
        useJUnitPlatform()
    }
}