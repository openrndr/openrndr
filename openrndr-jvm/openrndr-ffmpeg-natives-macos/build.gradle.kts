dependencies {
    runtimeOnly(libs.bundles.javacpp.ffmpeg) {
        artifact {
            classifier = "macosx-x86_64"
            extension = DependencyArtifact.DEFAULT_TYPE
        }
    }
}