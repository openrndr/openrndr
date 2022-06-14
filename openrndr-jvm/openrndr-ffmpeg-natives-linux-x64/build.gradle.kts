dependencies {
    runtimeOnly(libs.bundles.javacpp.ffmpeg) {
        artifact {
            classifier = "linux-x86_64"
            extension = DependencyArtifact.DEFAULT_TYPE
        }
    }
}