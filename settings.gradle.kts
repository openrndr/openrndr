rootProject.name = "openrndr"

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.4.0"
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