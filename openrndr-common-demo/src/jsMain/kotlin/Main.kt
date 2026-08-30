/**
 * Single entry point for the Kotlin/JS module.
 *
 * Kotlin/JS merges all top-level declarations of a source set into a single
 * module namespace, so it is not possible to have multiple top-level
 * `fun main()` declarations like on the JVM. Instead, every demo file
 * exposes a uniquely named function (e.g. `webGLDemo00()`, `webGLDemo01()`)
 * annotated with `@Program`.
 *
 * The `transformDemos` Gradle task scans the default package for those
 * functions and generates both `resources/index.html` (a page listing the
 * available applications) and `GeneratedApplications.kt` (a `runApplication` dispatch
 * function). The application to run is selected via the `application` query
 * parameter, e.g. `index.html?application=DemoCommon01`.
 */
private fun selectedApplicationName(): String? =
    js("new URLSearchParams(window.location.search).get('application')") as? String

fun main() {
    val programName = selectedApplicationName()
    runApplication(programName)
}
