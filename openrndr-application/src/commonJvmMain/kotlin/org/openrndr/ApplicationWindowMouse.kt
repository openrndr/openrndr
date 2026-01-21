package org.openrndr

import org.openrndr.events.Event
import org.openrndr.math.Vector2

class ApplicationWindowMouse(private val applicationWindow: () -> ApplicationWindow): MouseEvents {
    /**
     * The current mouse position
     */
    override var position: Vector2
        get() = applicationWindow().cursorPosition
        set(value) {
            applicationWindow().cursorPosition = value
        }

    /**
     * specifies if cursor should be visible
     */
    var cursorVisible: Boolean
        get() = applicationWindow().cursorVisible
        set(value) {
            applicationWindow().cursorVisible = value
        }

    var cursorHideMode: MouseCursorHideMode
        get() = applicationWindow().cursorHideMode
        set(value) {
            applicationWindow().cursorHideMode = value
        }

    /**
     * specifies the active cursor type, default is [CursorType.ARROW_CURSOR]
     */
    var cursorType: CursorType
        get() = applicationWindow().cursorType
        set(value) {
            applicationWindow().cursorType = value
        }

    /**
     * mouse button down event
     *
     * Emitted from [Application] whenever a mouse button is pressed
     */
    override val buttonDown = Event<MouseEvent>("mouse-button-down", postpone = true)

    /**
     * mouse button up event
     *
     * Emitted from [Application] whenever a mouse button is released
     */
    override val buttonUp = Event<MouseEvent>("mouse-button-up", postpone = true)

    /**
     * mouse dragged event
     *
     * Emitted from [Application] whenever the mouse is moved while a button is pressed
     */
    override val dragged = Event<MouseEvent>("mouse-dragged", postpone = true)

    /**
     * mouse moved event
     *
     * Emitted from [Application] whenever the mouse is moved
     */
    override val moved = Event<MouseEvent>("mouse-moved", postpone = true)
    /**
     * mouse scroll wheel event
     *
     * Emitted from [Application] whenever the mouse scroll wheel is used
     */
    override val scrolled = Event<MouseEvent>("mouse-scrolled", postpone = true)


    /**
     * mouse entered event
     *
     * Emitted from [Application] whenever the mouse enters the window client area
     */
    override val entered = Event<MouseEvent>("mouse-entered", postpone = true)

    /**
     * mouse exited event
     *
     * Emitted from [Application] whenever the mouse exits the window client area
     */
    override val exited = Event<MouseEvent>("mouse-exited", postpone = true)
}
