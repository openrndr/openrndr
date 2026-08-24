plugins {
    id("org.openrndr.convention.kotlin-multiplatform")
    id("org.openrndr.convention.kotlin-multiplatform-js")
    id("org.openrndr.convention.publish-multiplatform")
}

kotlin {
    sourceSets {
        val commonMain = getByName("commonMain") {
            dependencies {
                api(project(":openrndr-math"))
                api(project(":openrndr-shape"))
            }
        }
    }
}