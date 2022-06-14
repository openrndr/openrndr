dependencies {
    runtimeOnly(libs.bundles.javacpp.ffmpeg) {
        artifact {
            classifier = "linux-arm64"
            extension = DependencyArtifact.DEFAULT_TYPE
        }
    }
}