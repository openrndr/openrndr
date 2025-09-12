plugins {
    `version-catalog`
    `maven-publish`
    signing
}

catalog {
    versionCatalog {
        library("animatable", "org.openrndr:openrndr-animatable:$version")
        library("application", "org.openrndr:openrndr-application:$version")
        library("binpack", "org.openrndr:openrndr-binpack:$version")
        library("color", "org.openrndr:openrndr-color:$version")
        library("dialogs", "org.openrndr:openrndr-dialogs:$version")
        library("draw", "org.openrndr:openrndr-draw:$version")
        library("event", "org.openrndr:openrndr-event:$version")
        library("orextensions", "org.openrndr:openrndr-extensions:$version")
        library("ffmpeg", "org.openrndr:openrndr-ffmpeg:$version")
        library("filter", "org.openrndr:openrndr-filter:$version")
        library("gl3", "org.openrndr:openrndr-gl3:$version")
        library("gl-common", "org.openrndr:openrndr-gl-common:$version")
        library("kartifex", "org.openrndr:openrndr-kartifex:$version")
        library("ktessellation", "org.openrndr:openrndr-ktessellation:$version")
        library("math", "org.openrndr:openrndr-math:$version")
        library("openal", "org.openrndr:openrndr-openal:$version")
        library("shape", "org.openrndr:openrndr-shape:$version")
        library("utils", "org.openrndr:openrndr-utils:$version")
    }
}

group = "org.openrndr"

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["versionCatalog"])
        }
    }
}

signing {
    val isReleaseVersion = !(version.toString()).endsWith("SNAPSHOT")
    setRequired({ isReleaseVersion && gradle.taskGraph.hasTask("publish") })
    sign(publishing.publications)
}
