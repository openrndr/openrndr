plugins {
    id("org.openrndr.convention.kotlin-multiplatform")
    id("org.openrndr.convention.kotlin-multiplatform-js")
    id("org.openrndr.convention.publish-multiplatform")
}

kotlin {
    sourceSets {
        getByName("commonMain") {
            dependencies {
                api(project(":openrndr-math"))
                api(project(":openrndr-event"))
            }
        }
        getByName("webMain") {
            dependencies {
                implementation(libs.kotlin.browser)
            }
        }
    }
}
