package org.openrndr.convention

plugins {
    `maven-publish` apply true
    id("maven-publish")
    id("org.openrndr.convention.dokka")
    signing
    java
}

group = "org.openrndr"

tasks {
    @Suppress("UNUSED_VARIABLE")
    val javadoc by getting(Javadoc::class) {
        options {
            this as StandardJavadocDocletOptions
            addBooleanOption("Xdoclint:none", true)
        }
    }
}

java {
    withJavadocJar()
    withSourcesJar()
}

val isReleaseVersion = !(version.toString()).endsWith("SNAPSHOT")

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = "org.openrndr"
            artifactId = project.name
            description = project.name
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
    setRequired({ isReleaseVersion && gradle.taskGraph.hasTask("publish") })
    sign(publishing.publications)
}
