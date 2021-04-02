package org.openrndr

import mu.KotlinLogging
import org.openrndr.math.Vector2

private val logger = KotlinLogging.logger {}

/**
 * Application interface
 */
@ApplicationDslMarker
actual abstract class Application {
    actual companion object {
        actual fun run(program: Program, configuration: Configuration) {
            TODO("not implemented")
        }

        actual fun runAsync(program: Program, configuration: Configuration) {
            TODO("not implemented")
        }
    }

    actual abstract fun requestDraw()
    actual abstract fun requestFocus()

    actual abstract fun exit()
    actual abstract fun setup()

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
}