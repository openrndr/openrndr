package org.openrndr

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
    /** Use the system default */
    object SystemDefault : WindowMultisample()
    /** Disable multisampling */
    object Disabled : WindowMultisample()
    /** Use the specified sample count */
    data class SampleCount(val count: Int) : WindowMultisample()
}

class Configuration {

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
    var maximumWidth: Int = Int.MAX_VALUE/8

    /**
     * The maximum window height
     */
    var maximumHeight: Int = Int.MAX_VALUE/8


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
     * Should the window be made fullscreen?
     */
    var fullscreen:Fullscreen = Fullscreen.DISABLED

    /**
     * Should the window be made visible before calling setup?
     */
    var showBeforeSetup = true

    /**
     * Should the cursor be hidden?
     */
    var hideCursor = false

    /**
     * The window position. The window will be placed in the center of the primary screen when set to null
     */
    var position: IntVector2? = null

    /**
     * The window and drawing behaviour on window unfocus
     **/
    var unfocusBehaviour = UnfocusBehaviour.NORMAL

    /**
     * Should the created window be resizable?
     */
    var windowResizable: Boolean = false

    /**
     * Should the application be run in headless mode?
     */
    var headless: Boolean = false

    /**
     * Should the window render target use multisampling?
     */
    var multisample: WindowMultisample = WindowMultisample.Disabled

}

/**
 * Simple configuration builder
 */
fun configuration(builder: Configuration.() -> Unit): Configuration {
    return Configuration().apply(builder)
}
