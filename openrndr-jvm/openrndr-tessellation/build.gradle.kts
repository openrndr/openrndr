plugins {
    kotlin("multiplatform")
}
val kotlinLanguageVersion: String by rootProject.extra
val kotlinApiVersion: String by rootProject.extra

tasks.withType<JavaCompile> {
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
}

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