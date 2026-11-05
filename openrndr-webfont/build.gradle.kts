plugins {
    id("org.openrndr.convention.kotlin-multiplatform")
    id("org.openrndr.convention.kotlin-multiplatform-js")
    id("org.openrndr.convention.publish-multiplatform")
}

// decoders of WOFF and WOFF2 fonts, kept apart from openrndr-draw so that web bundles only hold them when a font driver
// needs them
kotlin {
    sourceSets {
        getByName("commonTest") {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}
