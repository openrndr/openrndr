plugins {
    org.openrndr.convention.`kotlin-jvm`
    org.openrndr.convention.`publish-jvm`
}
dependencies {
    runtimeOnly(libs.bundles.lwjgl.full) {
        artifact {
            classifier = "natives-linux-arm64"
            extension = DependencyArtifact.DEFAULT_TYPE
        }
    }
}