package org.openrndr

import org.openrndr.math.Vector2
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

var applicationBaseFunc: (() -> ApplicationBase)? = null

fun launch(block: suspend () -> Unit) {
    block.startCoroutine(object : Continuation<Unit> {
        override val context: CoroutineContext get() = EmptyCoroutineContext
        override fun resumeWith(result: Result<Unit>) {
        }
    })
}

/**
 * Application interface
 */
actual abstract class Application {
    actual abstract var program: Program
    actual abstract var configuration: Configuration

    internal actual fun run() {
        launch {
            setup()
            loop()
        }
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
    val applicationBase = applicationBaseFunc?.invoke() ?: error("applicationBaseFunc not set")
    val application = applicationBase.build(program, configuration)
    application.run()
}