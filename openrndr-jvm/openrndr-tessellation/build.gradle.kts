plugins {
    kotlin("multiplatform")
}
val kotlinLanguageVersion: String by rootProject.extra
val kotlinApiVersion: String by rootProject.extra

kotlin {
    jvm {
        withJava()
        sourceSets {
            val jvmMain by getting {
                dependencies {
                    implementation(project(":openrndr-math"))
                }
            }
        }
    }
}