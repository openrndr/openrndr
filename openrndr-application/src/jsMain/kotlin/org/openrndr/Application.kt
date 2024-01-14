package org.openrndr

import io.github.oshai.kotlinlogging.KotlinLogging
import org.openrndr.math.Vector2

private val logger = KotlinLogging.logger {}

var applicationBaseFunc: (() -> ApplicationBase)? = null

/**
 * Application interface
 */
actual abstract class Application {
    actual abstract var program: Program
    actual abstract var configuration: Configuration

    internal actual fun run() {
        throw NotImplementedError("Synchronous application is unsupported, use Application.runAsync()")
    }

    internal actual suspend fun runAsync() {
        setup()
        loop()
    }

    actual abstract fun requestDraw()
    actual abstract fun requestFocus()

    actual abstract fun exit()
    actual abstract suspend fun setup()

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
    actual abstract val pointers: List<Pointer>
}

/**
 * Runs [program] as a synchronous application with the given [configuration].
 * @see application
 */
actual fun application(program: Program, configuration: Configuration){
    throw NotImplementedError("Synchronous application is unsupported, use applicationAsync()")
}

/**
 * Runs [program] as an asynchronous application with the given [configuration].
 * @see applicationAsync
 */
actual suspend fun applicationAsync(program: Program, configuration: Configuration) {
    val applicationBase = applicationBaseFunc?.invoke() ?: error("applicationBaseFunc not set")
    val application = applicationBase.build(program, configuration)
    application.runAsync()
}