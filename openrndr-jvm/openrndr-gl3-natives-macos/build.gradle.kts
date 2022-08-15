plugins {
    org.openrndr.convention.`kotlin-jvm`
}
dependencies {
    runtimeOnly(libs.bundles.lwjgl.full) {
        artifact {
            classifier = "natives-macos"
            extension = DependencyArtifact.DEFAULT_TYPE
        }
    }
}