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
        @Suppress("UNUSED_VARIABLE")
        val jvmMain by getting {
            dependencies {
                implementation(project(":openrndr-math"))
                implementation(libs.bundles.lwjgl.openal)
            }
        }
    }
}
