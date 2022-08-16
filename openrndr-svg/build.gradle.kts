import org.openrndr.convention.currentOperatingSystemName

plugins {
    org.openrndr.convention.`kotlin-multiplatform`
}

val nativeClassifier = when (currentOperatingSystemName) {
    "linux" -> "linux-x64"
    "macos", "windows" -> currentOperatingSystemName
    else -> throw IllegalStateException("Unknown OS: $currentOperatingSystemName")
}

kotlin {
    sourceSets {
        @Suppress("UNUSED_VARIABLE")
        val jvmMain by getting {
            dependencies {
                implementation(project(":openrndr-application"))
                implementation(project(":openrndr-math"))
                implementation(project(":openrndr-color"))
                implementation(libs.jsoup)
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jvmTest by getting {
            dependencies {
                runtimeOnly(project(":openrndr-jvm:openrndr-gl3", "jvmRuntimeElements"))
                runtimeOnly(project(":openrndr-jvm:openrndr-gl3", "natives-${nativeClassifier}RuntimeElements"))
                runtimeOnly(libs.slf4j.simple)
                runtimeOnly(project(":openrndr-nullgl"))
            }
        }
    }
}