package org.openrndr.convention

import org.jetbrains.dokka.gradle.DokkaMultiModuleTask
import org.jetbrains.dokka.gradle.DokkaTaskPartial
import java.net.URL

plugins {
    id("org.jetbrains.dokka")
}

repositories {
    mavenCentral()
}

tasks.withType<DokkaTaskPartial> {
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
            localDirectory.set(sourcesDirectory)
            // URL showing where the source code can be accessed through the web browser
            remoteUrl.set(
                URL("https://github.com/openrndr/openrndr/blob/master/${moduleName.get()}/src/$name/kotlin")
            )
            // Suffix which is used to append the line number to the URL. Use #L for GitHub
            remoteLineSuffix.set("#L")
        }
    }
}

// Since pluginConfiguration doesn't seem to work, manual customization here
tasks.withType<DokkaMultiModuleTask> {
    doLast {
        val customCSS = file("dokka/styles/extra.css").readText()
        val cssPath = file("build/dokka/htmlMultiModule/styles/style.css")
        val defaultCSS = cssPath.readText()
        if(!defaultCSS.contains(customCSS)) {
            cssPath.writeText(defaultCSS + customCSS)
        }

        copy {
            from("dokka/images/logo-icon.svg")
            into("build/dokka/htmlMultiModule/images/")
        }
    }
}