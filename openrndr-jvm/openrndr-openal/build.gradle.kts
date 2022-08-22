plugins {
    org.openrndr.convention.`kotlin-multiplatform`
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
    }
}
