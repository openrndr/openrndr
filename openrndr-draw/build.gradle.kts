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
                api(project(":openrndr-color"))
                api(project(":openrndr-shape"))
                api(project(":openrndr-event"))
                implementation(project(":openrndr-utils"))
                implementation(project(":openrndr-platform"))
                implementation(libs.kotlin.coroutines)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotest.assertions)
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(libs.kotlin.js)
                implementation(libs.kotlin.browser)
            }
        }
    }
}