plugins {
    kotlin("jvm")
}

dependencies {
    runtimeOnly(libs.bundles.lwjgl.full) {
        artifact {
            classifier = "natives-linux"
            extension = DependencyArtifact.DEFAULT_TYPE
        }
    }
}
