plugins {
    org.openrndr.convention.`kotlin-jvm`
}
dependencies {
    runtimeOnly(libs.bundles.javacpp.ffmpeg) {
        artifact {
            classifier = "windows-x86_64"
            extension = DependencyArtifact.DEFAULT_TYPE
        }
    }
}