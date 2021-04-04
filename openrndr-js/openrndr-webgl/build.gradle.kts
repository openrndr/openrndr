plugins {
    kotlin("multiplatform")
}

val kotlinxSerializationVersion:    String by rootProject.extra
val kotestVersion:                  String by rootProject.extra
val junitJupiterVersion:            String by rootProject.extra

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