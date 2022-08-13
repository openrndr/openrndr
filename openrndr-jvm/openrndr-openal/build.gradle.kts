plugins {
    org.openrndr.convention.`kotlin-multiplatform`
}

kotlin {
    jvm {
        testRuns["test"].executionTask.configure {
            exclude("**/*.class")
        }
    }
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(project(":openrndr-math"))
                implementation(libs.bundles.lwjgl.openal)
                implementation(libs.kotlin.logging)
            }
        }
    }
}
