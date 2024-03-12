plugins {
    org.openrndr.convention.`kotlin-jvm`
    org.openrndr.convention.`publish-jvm`
}
dependencies {
    runtimeOnly(libs.bundles.lwjgl.openal) {
        artifact {
            classifier = "natives-windows"
            extension = "jar"
        }
    }
}