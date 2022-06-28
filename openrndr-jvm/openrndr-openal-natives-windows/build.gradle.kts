plugins {
    kotlin("jvm")
}

dependencies {
    runtimeOnly(libs.bundles.lwjgl.openal) {
        artifact {
            classifier = "natives-windows"
            extension = "jar"
        }
    }
}
