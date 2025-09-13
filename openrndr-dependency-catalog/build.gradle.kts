plugins {
    `version-catalog`
    `maven-publish`
    signing
}

catalog {
    versionCatalog {
        from(files("$rootDir/gradle/libs.versions.toml"))
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