plugins {
    id("org.openrndr.convention.kotlin-multiplatform")
}

val generatedProgramsDir = layout.buildDirectory.dir("generated/openrndr/programs/jsMain/kotlin")
val generatedResourcesDir = layout.buildDirectory.dir("generated/openrndr/programs/jsMain/resources")

kotlin {
    js {
        browser {
            commonWebpackConfig {
                outputFileName = "openrndr-program.js"
                cssSupport {
                    enabled.set(true)
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        getByName("jsMain") {
            kotlin.srcDir(generatedProgramsDir)
            resources.srcDir(generatedResourcesDir)
            dependencies {
                implementation(project(":openrndr-application"))
                implementation(project(":openrndr-draw"))
                implementation(project(":openrndr-gl-common"))
                implementation(project(":openrndr-js:openrndr-webgl"))
                implementation(libs.kotlin.coroutines)
                implementation(libs.kotlin.logging)
            }
        }
    }
}

/**
 * Scans the `.kt` files in the default package of `src/jsMain/kotlin` for functions annotated
 * with [org.openrndr.annotations.Program] and generates:
 * - `index.html`: a page listing the available programs, each of which can be selected and
 *   executed. It is generated into `build/generated/openrndr/programs/jsMain/resources` (added
 *   to the `jsMain` source set as a resources directory).
 * - a Kotlin source file (added to the `jsMain` source set) exposing the list of programs and a
 *   dispatch function used by `Main.kt` to run the selected program. It is generated into
 *   `build/generated/openrndr/programs/jsMain/kotlin`, following the standard Gradle convention
 *   of keeping generated sources under the `build` directory.
 */
val generateProgramIndex by tasks.registering {
    group = "openrndr"
    description = "Scans @Program annotated functions and generates the program index (index.html)"

    val kotlinSourceDir = layout.projectDirectory.dir("src/jsMain/kotlin")
    val resourcesDir = generatedResourcesDir
    val outputDir = generatedProgramsDir

    inputs.dir(kotlinSourceDir)
    outputs.dir(outputDir)
    outputs.dir(resourcesDir)

    doLast {
        data class ProgramEntry(val title: String, val functionName: String)

        val programRegex = Regex(
            """@Program\(\s*"([^"]*)"\s*\)\s*\r?\n\s*fun\s+\w+(?:<[^>]*>)?\.(\w+)\s*\("""
        )

        val programs = mutableListOf<ProgramEntry>()
        kotlinSourceDir.asFile.listFiles { file -> file.isFile && file.extension == "kt" }
            ?.sortedBy { it.name }
            ?.forEach { file ->
                val text = file.readText()
                val isDefaultPackage = text.lineSequence().none { it.trim().startsWith("package ") }
                if (isDefaultPackage) {
                    programRegex.findAll(text).forEach { match ->
                        programs.add(ProgramEntry(match.groupValues[1], match.groupValues[2]))
                    }
                }
            }

        val listItems = programs.joinToString("\n") { program ->
            "            <li><button class=\"program-button\" data-program=\"${program.functionName}\">${program.title} (${program.functionName})</button></li>"
        }

        val html = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <title>openrndr-webgl-demo</title>
                <meta name="viewport" content="width=device-width,initial-scale=1.0,user-scalable=no">
                <style>
                    body, html {
                        margin: 0;
                        width: 100%;
                        height: 100%;
                        overflow: hidden;
                        font-family: sans-serif;
                    }
                    #openrndr-canvas {
                        display: block;
                        width: 100%;
                        height: 100%;
                    }
                    #program-list {
                        position: absolute;
                        top: 0;
                        left: 0;
                        z-index: 1;
                        background: rgba(255, 255, 255, 0.85);
                        padding: 8px;
                        margin: 0;
                        list-style: none;
                    }
                    .program-button {
                        display: block;
                        width: 100%;
                        margin: 2px 0;
                    }
                </style>
            </head>
            <body>
            <ul id="program-list">
${listItems}
            </ul>
            <canvas id="openrndr-canvas"></canvas>
            <script src="openrndr-program.js"></script>
            <script>
                document.querySelectorAll(".program-button").forEach(function (button) {
                    button.addEventListener("click", function () {
                        var url = new URL(window.location.href);
                        url.searchParams.set("program", button.getAttribute("data-program"));
                        window.location.href = url.toString();
                    });
                });
            </script>
            </body>
            </html>
        """.trimIndent()

        val resourcesOutputDir = resourcesDir.get().asFile
        resourcesOutputDir.mkdirs()
        resourcesOutputDir.resolve("index.html").writeText(html + "\n")

        val whenBranches = programs.joinToString("\n") { program ->
            "                \"${program.functionName}\" -> ${program.functionName}()"
        }
        val defaultProgram = programs.firstOrNull()?.functionName

        val kotlinSource = """
            // Generated by the generateProgramIndex task. Do not edit manually.
            import org.openrndr.ApplicationBuilder

            /**
             * The names of all functions annotated with `@Program` found in the default package.
             */
            val availablePrograms: List<String> = listOf(${programs.joinToString(", ") { "\"${it.functionName}\"" }})

            /**
             * Runs the program identified by [name], falling back to the first available program
             * when [name] is `null` or unknown.
             */
            fun ApplicationBuilder.runProgram(name: String?) {
                when (name) {
${whenBranches}
                    else -> ${defaultProgram?.let { "$it()" } ?: "error(\"no @Program annotated functions were found\")"}
                }
            }
        """.trimIndent()

        val outputFile = outputDir.get().asFile
        outputFile.mkdirs()
        outputFile.resolve("GeneratedPrograms.kt").writeText(kotlinSource + "\n")
    }
}

tasks.named("compileKotlinJs") {
    dependsOn(generateProgramIndex)
}

tasks.named("jsProcessResources") {
    dependsOn(generateProgramIndex)
}