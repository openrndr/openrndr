@file:JvmName("ApplicationJVM")

package org.openrndr

import org.openrndr.math.IntVector2
import org.openrndr.math.Vector2
import kotlin.jvm.JvmName
import kotlin.jvm.JvmRecord

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

@JvmRecord
data class WindowConfiguration(
    val title: String = "OPENRNDR",
    val position: IntVector2? = null,
    val width: Int = 640,
    val height: Int = 480,
    val resizable: Boolean = false,
    val closable: Boolean = true,
    val alwaysOnTop: Boolean = false,
    val hideDecorations: Boolean = false,
    val transparent: Boolean = false,
    val multisample: WindowMultisample = WindowMultisample.Disabled,
    val fullscreen: Fullscreen = Fullscreen.DISABLED,
    val hideMouseCursor: Boolean = false,
    val relativeMouseCoordinates: Boolean = false,
    val utilityWindow: Boolean = false,
    val display: Display? = null,
    val unfocusBehaviour: UnfocusBehaviour = UnfocusBehaviour.NORMAL
)

/**
 * Application interface
 */
expect abstract class Application {
    abstract var program: Program
    abstract var configuration: Configuration

    internal fun run()

    abstract fun requestDraw()
    abstract fun requestFocus()

    abstract fun exit()
    abstract suspend fun setup()

    abstract fun loop()
    abstract var clipboardContents: String?
    abstract var windowTitle: String

    abstract var windowPosition: Vector2
    abstract var windowSize: Vector2
    abstract var windowContentScale: Double
    abstract var windowMultisample: WindowMultisample
    abstract var windowResizable: Boolean

    abstract var windowHitTest: ((Vector2) -> Hit)?

    abstract fun windowClose()
    abstract fun windowMinimize()
    abstract fun windowMaximize()
    abstract fun windowFullscreen(mode: Fullscreen)

    abstract var cursorPosition: Vector2
    abstract var cursorVisible: Boolean
    abstract var cursorHideMode: MouseCursorHideMode
    abstract var cursorType: CursorType


    abstract val seconds: Double

    abstract var presentationMode: PresentationMode
}

/**
 * Configures and initializes an application using the specified program and optional configuration settings.
 *
 * @param program the program instance that defines the application's core behavior, lifecycle, and rendering logic.
 * @param configuration an optional configuration object that defines customization options for the application,
 * such as preferred window dimensions, debug settings, and display properties. Defaults to a basic configuration.
 */
expect fun application(program: Program, configuration: Configuration = Configuration())