plugins {
    kotlin("jvm")
}

val lwjglNatives = "natives-macos-arm64"
val lwjglVersion: String by rootProject.extra

dependencies {
    runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-openal:$lwjglVersion:$lwjglNatives")
}