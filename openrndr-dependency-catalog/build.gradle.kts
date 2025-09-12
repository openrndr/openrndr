plugins {
    `version-catalog`
    `maven-publish`
    signing
}

catalog {
    // declare the aliases, bundles and versions in this block
    versionCatalog {
        from(files("$rootDir/gradle/libs.versions.toml"))
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