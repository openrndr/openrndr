import org.openrndr.application

/**
 * Single entry point for the Kotlin/JS module.
 *
 * Kotlin/JS merges all top-level declarations of a source set into a single
 * module namespace, so it is not possible to have multiple top-level
 * `fun main()` declarations like on the JVM. Instead, every demo file
 * exposes a uniquely named function (e.g. `webGLDemo00()`, `webGLDemo01()`)
 * annotated with `@Program`.
 *
 * The `generateProgramIndex` Gradle task scans the default package for those
 * functions and generates both `resources/index.html` (a page listing the
 * available programs) and `GeneratedPrograms.kt` (a `runProgram` dispatch
 * function). The program to run is selected via the `program` query
 * parameter, e.g. `index.html?program=webGLDemo01`.
 */
private fun selectedProgramName(): String? =
    js("new URLSearchParams(window.location.search).get('program')") as? String

fun main() {
    val programName = selectedProgramName()
    application {
        configure {
            width = 720
            height = 720
        }
        runProgram(programName)
    }
}
