plugins {
    kotlin("jvm")
}

val lwjglNatives = "natives-macos"
val lwjglVersion: String by rootProject.extra

dependencies {
    runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-openal:$lwjglVersion:$lwjglNatives")
}