package org.openrndr.convention

plugins {
    id("org.jetbrains.dokka")
}

repositories {
    mavenCentral()
}

dokka {
    pluginsConfiguration.html {
        customStyleSheets.from("dokka/styles/extra.css")
        customAssets.from("dokka/images/logo-icon.svg")
    }
    dokkaSourceSets.configureEach {
        skipDeprecated.set(true)

        val sourcesDirectory = try {
            file("src/$name/kotlin", PathValidation.EXISTS)
        } catch (_: InvalidUserDataException) {
            return@configureEach
        }

        // Specifies the location of the project source code on the Web.
        // If provided, Dokka generates "source" links for each declaration.
        sourceLink {
            // Unix based directory relative path to the root of the project (where you execute gradle respectively).
            localDirectory = sourcesDirectory

            // URL showing where the source code can be accessed through the web browser
            remoteUrl("https://github.com/openrndr/openrndr/blob/master/${moduleName.get()}/src/$name/kotlin")

            // Suffix which is used to append the line number to the URL. Use #L for GitHub
            remoteLineSuffix.set("#L")
        }
    }
}
