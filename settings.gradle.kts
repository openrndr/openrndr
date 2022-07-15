rootProject.name = "openrndr"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("kotlinApi", "1.6")
            version("kotlinLanguage", "1.6")
            version("kotlin", "1.6.21")
            version("jvmTarget", "1.8")
            version("kotlinxCoroutines", "1.6.4")
            version("kotlinLogging", "2.1.23")
            version("kotlinxSerialization", "1.3.2")
            version("lwjgl", "3.3.1")
            version("javacpp", "1.5.7")
            // ffmpeg version suffix should match javacpp version
            version("ffmpeg", "5.0-1.5.7")
            version("spek", "2.0.18")
            version("kluent", "1.68")
            version("jsoup", "1.14.3")
            version("kotest", "5.2.3")
            version("junitJupiter", "5.8.2")
            version("slf4j", "1.7.36")

            library("kotlin-logging", "io.github.microutils", "kotlin-logging").versionRef("kotlinLogging")
            library("kotlin-coroutines", "org.jetbrains.kotlinx", "kotlinx-coroutines-core")
                .versionRef("kotlinxCoroutines")
            library("kotlin-serialization-json", "org.jetbrains.kotlinx", "kotlinx-serialization-json")
                .versionRef("kotlinxSerialization")
            library("kotlin-serialization-core", "org.jetbrains.kotlinx", "kotlinx-serialization-core")
                .versionRef("kotlinxSerialization")
            library("kotlin-stdlib", "org.jetbrains.kotlin", "kotlin-stdlib").versionRef("kotlin")
            library("kotlin-test", "org.jetbrains.kotlin", "kotlin-test").versionRef("kotlin")
            library("kotlin-reflect", "org.jetbrains.kotlin", "kotlin-reflect").versionRef("kotlin")
            library("kotlin-gradlePlugin", "org.jetbrains.kotlin", "kotlin-gradle-plugin").versionRef("kotlin")

            library("lwjgl-core", "org.lwjgl", "lwjgl").versionRef("lwjgl")
            library("lwjgl-glfw", "org.lwjgl", "lwjgl-glfw").versionRef("lwjgl")
            library("lwjgl-jemalloc", "org.lwjgl", "lwjgl-jemalloc").versionRef("lwjgl")
            library("lwjgl-opengl", "org.lwjgl", "lwjgl-opengl").versionRef("lwjgl")
            library("lwjgl-stb", "org.lwjgl", "lwjgl-stb").versionRef("lwjgl")
            library("lwjgl-tinyexr", "org.lwjgl", "lwjgl-tinyexr").versionRef("lwjgl")

            library("lwjgl-nfd", "org.lwjgl", "lwjgl-nfd").versionRef("lwjgl")
            library("lwjgl-openal", "org.lwjgl", "lwjgl-openal").versionRef("lwjgl")
            library("lwjgl-egl", "org.lwjgl", "lwjgl-egl").versionRef("lwjgl")

            bundle("lwjgl-openal", listOf("lwjgl-core", "lwjgl-openal"))
            bundle(
                "lwjgl-full", listOf(
                    "lwjgl-core",
                    "lwjgl-glfw",
                    "lwjgl-jemalloc",
                    "lwjgl-opengl",
                    "lwjgl-stb",
                    "lwjgl-tinyexr",
                    "lwjgl-nfd"
                )
            )

            library("javacpp", "org.bytedeco", "javacpp").versionRef("javacpp")
            library("ffmpeg", "org.bytedeco", "ffmpeg").versionRef("ffmpeg")

            bundle("javacpp.ffmpeg", listOf("javacpp", "ffmpeg"))

            library("jsoup", "org.jsoup", "jsoup").versionRef("jsoup")

            library("spek-dsl", "org.spekframework.spek2", "spek-dsl-jvm").versionRef("spek")
            library("spek-junit5", "org.spekframework.spek2", "spek-runner-junit5").versionRef("spek")

            library("jupiter-api", "org.junit.jupiter", "junit-jupiter-api").versionRef("junitJupiter")
            library("jupiter-engine", "org.junit.jupiter", "junit-jupiter-engine").versionRef("junitJupiter")

            bundle("jupiter", listOf("jupiter-api", "jupiter-engine"))

            library("kotest", "io.kotest", "kotest-assertions-core").versionRef("kotest")
            library("kluent", "org.amshove.kluent", "kluent").versionRef("kluent")
            library("slf4j-simple", "org.slf4j", "slf4j-simple").versionRef("slf4j")
        }
    }
}

include(
    listOf(
        "openrndr-application",
        "openrndr-core",
        "openrndr-demos",
        "openrndr-draw",
        "openrndr-math",
        "openrndr-color",
        "openrndr-shape",
        "openrndr-event",
        "openrndr-binpack",
        "openrndr-filter",
        "openrndr-svg",
        "openrndr-animatable",
        "openrndr-jvm:openrndr-dialogs",
        "openrndr-jvm:openrndr-ffmpeg",
        "openrndr-jvm:openrndr-ffmpeg-natives-windows",
        "openrndr-jvm:openrndr-ffmpeg-natives-macos",
        "openrndr-jvm:openrndr-ffmpeg-natives-macos-arm64",
        "openrndr-jvm:openrndr-ffmpeg-natives-linux-x64",
        "openrndr-jvm:openrndr-ffmpeg-natives-linux-arm64",
        "openrndr-jvm:openrndr-gl3",
        "openrndr-jvm:openrndr-gl3-natives-linux-x64",
        "openrndr-jvm:openrndr-gl3-natives-linux-arm64",
        "openrndr-jvm:openrndr-gl3-natives-macos",
        "openrndr-jvm:openrndr-gl3-natives-macos-arm64",
        "openrndr-jvm:openrndr-gl3-natives-windows",
        "openrndr-jvm:openrndr-openal",
        "openrndr-jvm:openrndr-openal-natives-linux-x64",
        "openrndr-jvm:openrndr-openal-natives-linux-arm64",
        "openrndr-jvm:openrndr-openal-natives-macos",
        "openrndr-jvm:openrndr-openal-natives-macos-arm64",
        "openrndr-jvm:openrndr-openal-natives-windows",
        "openrndr-js:openrndr-webgl",
        "openrndr-extensions",
        "openrndr-nullgl",
        "openrndr-utils",
        "openrndr-dds",
        "openrndr-kartifex",
        "openrndr-ktessellation"
    )
)