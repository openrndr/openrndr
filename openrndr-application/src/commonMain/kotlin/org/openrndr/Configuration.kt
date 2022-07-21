package org.openrndr

import org.openrndr.draw.BufferMultisample
import org.openrndr.math.IntVector2

/**
 *
 */
enum class UnfocusBehaviour {
    /** Continue as usual **/
    NORMAL,

    /** Throttle drawing **/
    THROTTLE
}

/**
 * Fullscreen mode
 */
enum class Fullscreen {
    /** Do not use fullscreen */
    DISABLED,

    /** Use current display mode for fullscreen */
    CURRENT_DISPLAY_MODE,

    /** Use given width and height to set a display mode */
    SET_DISPLAY_MODE
}

sealed class WindowMultisample {
    fun bufferEquivalent() : BufferMultisample = when {
        this === SystemDefault -> error("Cannot resolve SystemDefault to BufferMultisample")
        this === Disabled -> BufferMultisample.Disabled
        this is SampleCount -> BufferMultisample.SampleCount(this.count)
        else -> error("Cannot resolve $this to BufferMultisample")
    }

    /** Use the system default */
    object SystemDefault : WindowMultisample()

    /** Disable multisampling */
    object Disabled : WindowMultisample()

    /** Use the specified sample count */
    data class SampleCount(val count: Int) : WindowMultisample()
}

class Configuration {

    var canvasId = "openrndr-canvas"

    /**
     * The preferred window width
     */
    var width: Int = 640

    /**
     * The preferred window height
     */
    var height: Int = 480

    /**
     * The minimum window width
     */
    var minimumWidth: Int = 128

    /**
     * The minimum window height
     */
    var minimumHeight: Int = 128


    /**
     * The maximum window width
     */
    var maximumWidth: Int = Int.MAX_VALUE / 8

    /**
     * The maximum window height
     */
    var maximumHeight: Int = Int.MAX_VALUE / 8


    /**
     * The window title
     */
    var title: String = "OPENRNDR"

    /**
     * Should debug mode be used?
     */
    var debug: Boolean = false
    var trace: Boolean = false

    /**
     * Should window decorations be hidden?
     */
    var hideWindowDecorations = false

    /**
     * The display on which to create the window.
     * All currently detected [Display]s can be found in the `displays` list inside [ApplicationBuilder].
     * Defaults to `null` which means to use your primary monitor.
     */
    var display: Display? = null

    /**
     * Should the window be made fullscreen?
     */
    var fullscreen: Fullscreen = Fullscreen.DISABLED

    /**
     * Should the window be made visible before calling setup?
     */
    var showBeforeSetup = true

    /**
     * Should the cursor be hidden?
     */
    var hideCursor = false

    /**
     * If the cursor is hidden in what way should it be hidden?
     */
    var cursorHideMode = MouseCursorHideMode.HIDE

    /**
     * The window position. The window will be placed in the center of the primary screen when set to null
     */
    var position: IntVector2? = null

    /**
     * The window and drawing behaviour on window unfocus
     **/
    var unfocusBehaviour = UnfocusBehaviour.NORMAL


    /**
     * Should the window be always on top (floating)
     */
    var windowAlwaysOnTop: Boolean = false

    /**
     * Should the created window be resizable?
     */
    var windowResizable: Boolean = false

    /**
     * Should the window icon be set to the openrndr pink icon?
     */
    var windowSetIcon: Boolean = true

    /**
     * Should the window be transparent
     */
    var windowTransparent : Boolean = false

    /**
     * Should the window render target use multisampling?
     */
    var multisample: WindowMultisample = WindowMultisample.Disabled

    /**
     * Should the program wait for vertical retrace?
     */
    var vsync = true

    var maxContentScale = 10.0

}

@Deprecated(
    "Use buildConfiguration instead", level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("buildConfiguration(builder)")
)
fun configuration(builder: Configuration.() -> Unit): Configuration = buildConfiguration(builder)

/**
 * Convenience function for building a new [Configuration].
 * @return the built [Configuration]
 */
fun buildConfiguration(builder: Configuration.() -> Unit): Configuration {
    return Configuration().apply(builder)
}
