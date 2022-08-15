plugins {
    org.openrndr.convention.`kotlin-jvm`
}
dependencies {
    runtimeOnly(libs.bundles.lwjgl.openal) {
        artifact {
            classifier = "natives-macos"
            extension = DependencyArtifact.DEFAULT_TYPE
        }
    }
}