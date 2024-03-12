plugins {
    org.openrndr.convention.`kotlin-jvm`
    org.openrndr.convention.`publish-jvm`
}
dependencies {
    runtimeOnly(libs.bundles.javacpp) {
        artifact {
            classifier = "linux-arm64"
            extension = DependencyArtifact.DEFAULT_TYPE
        }
    }

    runtimeOnly(libs.bundles.javacpp.ffmpeg) {
        artifact {
            classifier = "linux-arm64-gpl"
            extension = DependencyArtifact.DEFAULT_TYPE
        }
    }
}