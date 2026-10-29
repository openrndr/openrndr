plugins {
    id("org.openrndr.convention.kotlin-multiplatform-js")
    id("org.openrndr.convention.publish-multiplatform")
}

kotlin {
    sourceSets {
        getByName("webMain") {
            dependencies {
                api(project(":openrndr-draw"))
                implementation(project(":openrndr-dds"))
                implementation(project(":openrndr-utils"))
                implementation(libs.kotlin.js)
                implementation(libs.kotlin.stdlib)
            }
        }
        getByName("webTest") {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlin.coroutines.test)
            }
        }
    }
}

// the tests read a cubemap from the repository, which only works when they run in Node.js
tasks.withType<org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest>().configureEach {
    environment("OPENRNDR_ROOT", rootProject.projectDir.absolutePath)
}
