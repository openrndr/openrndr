plugins {
    id("org.openrndr.convention.kotlin-jvm")
    id("org.openrndr.convention.publish-multiplatform")
}
dependencies {
    implementation(project(":openrndr-application"))
    implementation(libs.kotlin.coroutines)
}