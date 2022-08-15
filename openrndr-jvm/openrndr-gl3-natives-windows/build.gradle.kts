plugins {
    org.openrndr.convention.`kotlin-jvm`
}
dependencies {
    runtimeOnly(libs.bundles.lwjgl.full) {
        artifact {
            classifier = "natives-windows"
            extension = DependencyArtifact.DEFAULT_TYPE
        }
    }
}