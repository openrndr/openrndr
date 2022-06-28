plugins {
    kotlin("jvm")
}

dependencies {
    runtimeOnly(libs.bundles.lwjgl.openal) {
        artifact {
            classifier = "natives-linux"
            extension = DependencyArtifact.DEFAULT_TYPE
        }
    }
}
