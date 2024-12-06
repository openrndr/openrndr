package org.openrndr.convention

import org.jetbrains.dokka.gradle.DokkaMultiModuleTask
import org.jetbrains.dokka.gradle.DokkaTaskPartial
import java.net.URI

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
            localDirectory = sourcesDirectory

            // URL showing where the source code can be accessed through the web browser
            remoteUrl = URI(
                "https://github.com/openrndr/openrndr/blob/master/${moduleName.get()}/src/$name/kotlin"
            ).toURL()

            // Suffix which is used to append the line number to the URL. Use #L for GitHub
            remoteLineSuffix.set("#L")
        }
    }
}

// Since pluginConfiguration doesn't seem to work, manual customization here
tasks.withType<DokkaMultiModuleTask> {
    doLast {
        // Runs for JS, JVM and root. Only for the last one build/dokka/ exists.
        val cssPath = rootDir.resolve("build/dokka/htmlMultiModule/styles/style.css")
        if(cssPath.exists()) {
            val defaultCSS = cssPath.readText()
            val customCSS = rootDir.resolve("dokka/styles/extra.css").readText()
            if(!defaultCSS.contains(customCSS)) {
                cssPath.writeText(defaultCSS + customCSS)
            }

            copy {
                from("dokka/images/logo-icon.svg")
                into("build/dokka/htmlMultiModule/images/")
            }
        }
    }
}
