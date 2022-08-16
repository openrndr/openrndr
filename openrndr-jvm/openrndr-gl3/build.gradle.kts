import org.openrndr.convention.addNativeRuntimeOnly
import org.openrndr.convention.currentOperatingSystemName
import org.openrndr.convention.mapToLwjglTargetName
import org.openrndr.convention.openrndrJvmNativeVariants

plugins {
    org.openrndr.convention.`kotlin-multiplatform-jvm-natives`
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
                implementation(project(":openrndr-draw"))
                implementation(project(":openrndr-shape"))
                implementation(project(":openrndr-binpack"))
                implementation(project(":openrndr-dds"))
                implementation(libs.kotlin.coroutines)
                implementation(libs.lwjgl.core)
                implementation(libs.lwjgl.glfw)
                implementation(libs.lwjgl.jemalloc)
                implementation(libs.lwjgl.opengl)
                implementation(libs.lwjgl.stb)
                implementation(libs.lwjgl.tinyexr)
                implementation(libs.lwjgl.openal)
                implementation(libs.lwjgl.egl)
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jvmTest by getting {
            dependencies {
                runtimeOnly(project(":openrndr-jvm:openrndr-gl3", "jvmRuntimeElements"))
                runtimeOnly(project(":openrndr-jvm:openrndr-gl3", "natives-${nativeClassifier}RuntimeElements"))
                runtimeOnly(libs.slf4j.simple)
                implementation(libs.kluent)
                implementation(libs.spek.dsl)
            }
        }

        for (nativeVariant in openrndrJvmNativeVariants) {
            getByName(nativeVariant.targetName + "Main") {
                dependencies {
                    addNativeRuntimeOnly(libs.bundles.lwjgl.full, nativeVariant.mapToLwjglTargetName())
                }
//                dependsOn(jvmMain)
            }
        }
    }
}

