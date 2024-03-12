plugins {
    org.openrndr.convention.`kotlin-multiplatform`
    org.openrndr.convention.`publish-multiplatform`
}

kotlin {
    sourceSets {
        @Suppress("UNUSED_VARIABLE")
        val commonMain by getting {
            dependencies {
                api(project(":openrndr-math"))
                api(project(":openrndr-color"))
                api(project(":openrndr-shape"))
                api(project(":openrndr-event"))
                implementation(project(":openrndr-utils"))
                implementation(project(":openrndr-platform"))
                implementation(libs.kotlin.coroutines)
            }
        }
    }
}