plugins {
    kotlin("multiplatform")
}

kotlin {
    js(IR) {
        browser()
        nodejs()
    }

    sourceSets {
        @Suppress("UNUSED_VARIABLE")
        val jsMain by getting {
            dependencies {
                api(project(":openrndr-application"))
                api(project(":openrndr-draw"))
            }

        }
        @Suppress("UNUSED_VARIABLE")
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}