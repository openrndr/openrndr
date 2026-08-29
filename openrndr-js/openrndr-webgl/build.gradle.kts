plugins {
    id("org.openrndr.convention.kotlin-multiplatform-js")
    id("org.openrndr.convention.publish-multiplatform")
}

kotlin {
    sourceSets {
        getByName("webMain") {
            dependencies {
                api(project(":openrndr-application"))
                api(project(":openrndr-draw"))
                implementation(project(":openrndr-gl-common"))
                implementation(libs.kotlin.coroutines)
                implementation(libs.kotlin.js)
                implementation(libs.kotlin.browser)
                implementation(libs.kotlin.web)
                implementation(libs.kotlin.stdlib)

            }
        }
        getByName("webTest") {
            dependencies {
                implementation(libs.kotlin.test)
                //implementation("org.jetbrains.kotlin:kotlin-test-junit5:2.4.10")
            }
        }
        getByName("wasmJsMain") {
            dependencies {
                implementation(libs.kotlin.stdlib)
            }
        }
    }
}