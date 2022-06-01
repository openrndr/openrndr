package org.openrndr

import mu.KotlinLogging
import org.openrndr.math.Vector2

private val logger = KotlinLogging.logger {}

var applicationFunc : ((Program, Configuration) -> Application)? = null

/**
 * Application interface
 */
@ApplicationDslMarker
actual abstract class Application {
    actual abstract var program: Program
    actual abstract var configuration: Configuration

    actual fun run(program: Program, configuration: Configuration) {
        error("use Application.runAsync")
    }

    actual suspend fun runAsync(program: Program, configuration: Configuration) {
        val application = applicationFunc?.invoke(program, configuration) ?: error("applicationFunc not set")
        application.setup(program, configuration)
        application.loop()
    }

    actual abstract fun requestDraw()
    actual abstract fun requestFocus()

    actual abstract fun exit()
    actual abstract suspend fun setup(program: Program, configuration: Configuration)

    actual abstract fun loop()
    actual abstract var clipboardContents: String?
    actual abstract var windowTitle: String

    actual abstract var windowPosition: Vector2
    actual abstract var windowSize: Vector2
    actual abstract var cursorPosition: Vector2
    actual abstract var cursorVisible: Boolean
    actual abstract var cursorHideMode: MouseCursorHideMode
    actual abstract var cursorType: CursorType

    actual abstract val seconds: Double

    actual abstract var presentationMode: PresentationMode
    actual abstract var windowContentScale: Double
    actual abstract var windowMultisample: WindowMultisample
    actual abstract var windowResizable: Boolean
}

/**
 * Runs [program] as an application using [configuration].
 */
actual fun application(program: Program, configuration: Configuration) {
    error("use applicationAsync")
}

actual suspend fun applicationAsync(program: Program, configuration: Configuration) {
    val application = applicationFunc?.invoke(program, configuration) ?: error("applicationFunc not set")
    application.runAsync(program, configuration)
}