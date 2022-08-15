plugins {
    org.openrndr.convention.`kotlin-jvm`
}
dependencies {
    runtimeOnly(libs.bundles.lwjgl.openal) {
        artifact {
            classifier = "natives-linux-arm64"
            extension = DependencyArtifact.DEFAULT_TYPE
        }
    }
}