plugins {
    org.openrndr.convention.`kotlin-jvm`
}
dependencies {
    api(project(":openrndr-color"))
    api(project(":openrndr-draw"))
    api(project(":openrndr-math"))
    api(project(":openrndr-shape"))
    api(project(":openrndr-event"))
    api(project(":openrndr-animatable"))
    implementation(project(":openrndr-application"))
    testRuntimeOnly(project(":openrndr-nullgl"))
    testImplementation(libs.kluent)
    testImplementation(libs.spek.dsl)
    testRuntimeOnly(libs.spek.junit5)
}

tasks {
    test {
        useJUnitPlatform {
            includeEngines("spek2")
        }
    }
}