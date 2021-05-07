plugins {
    kotlin("multiplatform")
}

val lwjglNatives = "natives-linux"
val lwjglVersion: String by rootProject.extra

val kotlinApiVersion: String by rootProject.extra

kotlin {
    jvm {

    }
    sourceSets {
        val jvmMain by getting {
            dependencies {
                runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:$lwjglNatives")
                runtimeOnly("org.lwjgl:lwjgl-glfw:$lwjglVersion:$lwjglNatives")
                runtimeOnly("org.lwjgl:lwjgl-jemalloc:$lwjglVersion:$lwjglNatives")
                runtimeOnly("org.lwjgl:lwjgl-opengl:$lwjglVersion:$lwjglNatives")
                runtimeOnly("org.lwjgl:lwjgl-stb:$lwjglVersion:$lwjglNatives")
                runtimeOnly("org.lwjgl:lwjgl-tinyexr:$lwjglVersion:$lwjglNatives")
            }
        }
    }
}
