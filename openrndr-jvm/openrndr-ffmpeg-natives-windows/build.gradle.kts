plugins {
    org.openrndr.convention.`kotlin-jvm`
}
dependencies {
    runtimeOnly(libs.bundles.javacpp) {
        artifact {
            classifier = "windows-x86_64"
            extension = DependencyArtifact.DEFAULT_TYPE
        }
    }
    runtimeOnly(libs.bundles.javacpp.ffmpeg) {
        artifact {
            classifier = "windows-x86_64-gpl"
            extension = DependencyArtifact.DEFAULT_TYPE
        }
    }
}