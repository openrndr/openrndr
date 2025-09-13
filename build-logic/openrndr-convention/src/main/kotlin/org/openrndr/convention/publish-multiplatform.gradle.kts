package org.openrndr.convention



plugins {
    `maven-publish` apply false
    id("maven-publish")
    id("org.openrndr.convention.dokka")
    signing
}

group = "org.openrndr"

publishing {
    publications {

        val fjdj = tasks.register<Jar>("fakeJavaDocJar") {
            archiveClassifier.set("javadoc")
        }
        matching { it.name == "jvm" }.forEach { p ->
            p as MavenPublication
            p.artifact(fjdj)
        }
        all {
            this as MavenPublication
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
                    connection.set("scm:git:git@github.com:openrndr/orx.git")
                    developerConnection.set("scm:git:ssh://github.com/openrndr/openrndr.git")
                    url.set("https://github.com/openrndr/openrndr")
                }
            }
        }
    }
}

val isReleaseVersion = !(version.toString()).endsWith("SNAPSHOT")

signing {
    setRequired({ isReleaseVersion && gradle.taskGraph.hasTask("publish") })
    sign(publishing.publications)
}
