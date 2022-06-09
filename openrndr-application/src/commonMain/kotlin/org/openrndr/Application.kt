@file:JvmName("ApplicationJVM")
package org.openrndr

import org.openrndr.math.Vector2
import kotlin.jvm.JvmName

/**
 * PresentationMode describes modes of frame presentation
 */
enum class PresentationMode {
    /**
     * automatic presentation mode, frames are presented at highest rate possible
     */
    AUTOMATIC,

    /**
     * manual presentation mode, presentation only takes place after requesting redraw
     */
    MANUAL,
}

@DslMarker
annotation class ApplicationDslMarker

/**
 * Application interface
 */
@ApplicationDslMarker
expect abstract class Application {
    abstract var program: Program
    abstract var configuration: Configuration

    internal fun run(program: Program, configuration: Configuration)
    internal suspend fun runAsync(program: Program, configuration: Configuration)

    abstract fun requestDraw()
    abstract fun requestFocus()

    abstract fun exit()
    abstract suspend fun setup(program: Program = Program(), configuration: Configuration = Configuration())

    abstract fun loop()
    abstract var clipboardContents: String?
    abstract var windowTitle: String

    abstract var windowPosition: Vector2
    abstract var windowSize: Vector2
    abstract var windowContentScale: Double
    abstract var windowMultisample: WindowMultisample
    abstract var windowResizable: Boolean

    abstract var cursorPosition: Vector2
    abstract var cursorVisible: Boolean
    abstract var cursorHideMode: MouseCursorHideMode
    abstract var cursorType: CursorType

    abstract val seconds: Double

    abstract var presentationMode: PresentationMode
}

expect fun application(program: Program, configuration: Configuration = Configuration())
expect suspend fun applicationAsync(program: Program, configuration: Configuration = Configuration())