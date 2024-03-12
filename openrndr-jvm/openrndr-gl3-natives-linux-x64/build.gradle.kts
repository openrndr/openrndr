plugins {
    org.openrndr.convention.`kotlin-jvm`
    org.openrndr.convention.`publish-jvm`
}
dependencies {
    runtimeOnly(libs.bundles.lwjgl.full) {
        artifact {
            classifier = "natives-linux"
            extension = DependencyArtifact.DEFAULT_TYPE
        }
    }
}