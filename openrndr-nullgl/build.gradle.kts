plugins {
    org.openrndr.convention.`kotlin-jvm`
}
dependencies {
    implementation(project(":openrndr-application"))
    implementation(libs.kotlin.coroutines)
}