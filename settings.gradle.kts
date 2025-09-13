rootProject.name = "openrndr"

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}
includeBuild("build-logic")
include(
    listOf(
        "openrndr-application",
        "openrndr-common-demo",
        "openrndr-core",
        "openrndr-demos",
        "openrndr-draw",
        "openrndr-gl-common",
        "openrndr-math",
        "openrndr-color",
        "openrndr-shape",
        "openrndr-event",
        "openrndr-binpack",
        "openrndr-filter",
        "openrndr-platform",

        "openrndr-animatable",
        "openrndr-jvm:openrndr-dialogs",

        "openrndr-jvm:openrndr-gl3",
        "openrndr-jvm:openrndr-openal",


        "openrndr-jvm:openrndr-ffmpeg",
        "openrndr-jvm:openrndr-fontdriver-stb",

        "openrndr-js:openrndr-webgl",
        "openrndr-js:openrndr-webgl-demo",
        "openrndr-extensions",
        "openrndr-nullgl",
        "openrndr-utils",
        "openrndr-dds",
        "openrndr-kartifex",
        "openrndr-ktessellation",

        "openrndr-dependency-catalog",
        "openrndr-module-catalog"
    )
)