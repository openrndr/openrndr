plugins {
    `version-catalog`
    `maven-publish`
    signing
}

catalog {
    versionCatalog {
        library("animatable", "org.openrndr:openrndr-animatable:$version")
        library("application-core", "org.openrndr:openrndr-application:$version")
        library("application-glfw", "org.openrndr:openrndr-application-glfw:$version")
        library("application-egl", "org.openrndr:openrndr-application-egl:$version")

        library("binpack", "org.openrndr:openrndr-binpack:$version")
        library("color", "org.openrndr:openrndr-color:$version")
        library("dialogs", "org.openrndr:openrndr-dialogs:$version")
        library("draw", "org.openrndr:openrndr-draw:$version")
        library("event", "org.openrndr:openrndr-event:$version")
        // we can't name this 'extensions' because it is a keyword in Gradle
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

        bundle("basic", listOf("application-core", "draw", "utils"))
        bundle("runtime-glfw", listOf("application-glfw"))
    }
}

group = "org.openrndr"

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "org.openrndr"
            artifactId = project.name
            description = project.name
            from(components["versionCatalog"])
            pom {
                name.set(project.name)
                description.set(project.name)
                url.set("https://openrndr.org")
                developers {
                    developer {
                        id.set("edwinjakobs")
                        name.set("Edwin Jakobs")
                        email.set("edwin@openrndr.org")
                    }
                }

                licenses {
                    license {
                        name.set("BSD-2-Clause")
                        url.set("https://github.com/openrndr/openrndr/blob/master/LICENSE")
                        distribution.set("repo")
                    }
                }

                scm {
                    connection.set("scm:git:git@github.com:openrndr/openrndr.git")
                    developerConnection.set("scm:git:ssh://github.com/openrndr/openrndr.git")
                    url.set("https://github.com/openrndr/openrndr")
                }
            }
        }
    }
}

signing {
    val isReleaseVersion = !(version.toString()).endsWith("SNAPSHOT")
    setRequired({ isReleaseVersion && gradle.taskGraph.hasTask("publish") })
    sign(publishing.publications)
}
