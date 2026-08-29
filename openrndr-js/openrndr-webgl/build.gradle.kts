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
            }
        }
        getByName("wasmJsMain") {
            dependencies {
                implementation(libs.kotlin.stdlib)
            }
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest>().configureEach {
    failOnNoDiscoveredTests = false
}

tasks.named("jsNodeTest") {
    enabled = false
}

tasks.named("wasmJsNodeTest") {
    enabled = false
}


tasks.named("wasmJsBrowserTest") {
    enabled = false
}
