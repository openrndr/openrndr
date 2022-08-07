plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
            exclude("**/*.class")
        }
    }
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(project(":openrndr-core"))
                implementation(project(":openrndr-math"))
                implementation(libs.bundles.lwjgl.openal)
                implementation(libs.kotlin.logging)
            }
        }
    }
}
