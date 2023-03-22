plugins {
    org.openrndr.convention.`kotlin-jvm`
}
dependencies {
    runtimeOnly(libs.bundles.javacpp) {
        artifact {
            classifier = "linux-x86_64"
            extension = DependencyArtifact.DEFAULT_TYPE
        }
    }
    runtimeOnly(libs.bundles.javacpp.ffmpeg) {
        artifact {
            classifier = "linux-x86_64"
            extension = DependencyArtifact.DEFAULT_TYPE
        }
    }
}