plugins {
    kotlin("jvm")
}

dependencies {
    runtimeOnly(libs.bundles.lwjgl.openal) {
        artifact {
            classifier = "natives-macos-arm64"
            extension = DependencyArtifact.DEFAULT_TYPE
        }
    }
}