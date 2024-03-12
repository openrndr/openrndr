plugins {
    org.openrndr.convention.`kotlin-jvm`
    org.openrndr.convention.`publish-jvm`
}
dependencies {
    implementation(project(":openrndr-application"))
    implementation(libs.kotlin.coroutines)
}