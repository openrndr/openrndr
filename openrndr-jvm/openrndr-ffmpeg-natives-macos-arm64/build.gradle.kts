plugins {
    org.openrndr.convention.`kotlin-jvm`
}
dependencies {
    runtimeOnly(libs.bundles.javacpp) {
        artifact {
            classifier = "macosx-arm64"
            extension = DependencyArtifact.DEFAULT_TYPE
        }
    }
    runtimeOnly(libs.bundles.javacpp.ffmpeg) {
        artifact {
            classifier = "macosx-arm64-gpl"
            extension = DependencyArtifact.DEFAULT_TYPE
        }
    }
}