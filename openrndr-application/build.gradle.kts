plugins {
    id("org.openrndr.convention.kotlin-multiplatform")
    id("org.openrndr.convention.kotlin-multiplatform-js")
    id("org.openrndr.convention.publish-multiplatform")
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":openrndr-math"))
                api(project(":openrndr-draw"))
                api(project(":openrndr-animatable"))
                api(project(":openrndr-platform"))
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(libs.kotlin.coroutines)
            }
        }
    }
}