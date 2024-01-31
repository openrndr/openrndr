plugins {
    org.openrndr.convention.`kotlin-jvm`
    java
}
dependencies {
    runtimeOnly(libs.bundles.lwjgl.full) {
        artifact {
            classifier = "natives-macos-arm64"
            extension = DependencyArtifact.DEFAULT_TYPE
        }
    }
}