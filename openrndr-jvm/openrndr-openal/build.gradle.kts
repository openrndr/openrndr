import org.openrndr.convention.addNativeRuntimeOnly
import org.openrndr.convention.mapToLwjglTargetName
import org.openrndr.convention.openrndrJvmNativeVariants

plugins {
    org.openrndr.convention.`kotlin-multiplatform-jvm-natives`
}

kotlin {
    sourceSets {
        @Suppress("UNUSED_VARIABLE")
        val jvmMain by getting {
            dependencies {
                implementation(project(":openrndr-math"))
                implementation(libs.bundles.lwjgl.openal)
            }
        }
        for (nativeVariant in openrndrJvmNativeVariants) {
            getByName(nativeVariant.targetName + "Main") {
                dependencies {
                    addNativeRuntimeOnly(libs.bundles.lwjgl.openal, nativeVariant.mapToLwjglTargetName())
                }
            }
        }
    }
}