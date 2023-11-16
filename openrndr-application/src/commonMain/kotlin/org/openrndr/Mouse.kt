package org.openrndr

import org.openrndr.events.Event
import org.openrndr.math.Vector2


enum class MouseCursorHideMode {
    HIDE,
    DISABLE
}


/**
 * Mouse event message
 */
data class MouseEvent(val position: Vector2, val rotation: Vector2, val dragDisplacement: Vector2, val type: MouseEventType, val button: MouseButton, val modifiers: Set<KeyModifier>) {
    /**
     * specifies if the propagation of this event is cancelled
     * @see cancelPropagation
     */
    var propagationCancelled: Boolean = false

    /**
     * cancel propagation of this [MouseEvent] message
     *
     * Note that cancellation is only a hint. Event listeners should check if [MouseEvent.propagationCancelled] is set to see
     * if another listener already consumed the event.
     * @see propagationCancelled
     */
    fun cancelPropagation() {
        propagationCancelled = true
    }
}

/**
 * Mouse cursor types
 */
enum class CursorType {
    ARROW_CURSOR,
    IBEAM_CURSOR,
    CROSSHAIR_CURSOR,
    HAND_CURSOR,
    HRESIZE_CURSOR,
    VRESIZE_CURSOR
}

interface MouseEvents {

    val position: Vector2

    /**
     * mouse button down event
     *
     * Emitted from [Application] whenever a mouse button is pressed
     */
    val buttonDown: Event<MouseEvent>

    /**
     * mouse button up event
     *
     * Emitted from [Application] whenever a mouse button is released
     */
    val buttonUp: Event<MouseEvent>

    /**
     * mouse dragged event
     *
     * Emitted from [Application] whenever the mouse is moved while a button is pressed
     */
    val dragged: Event<MouseEvent>

    /**
     * mouse moved event
     *
     * Emitted from [Application] whenever the mouse is moved
     */
    val moved: Event<MouseEvent>
    /**
     * mouse scroll wheel event
     *
     * Emitted from [Application] whenever the mouse scroll wheel is used
     */
    val scrolled: Event<MouseEvent>


    /**
     * mouse entered event
     *
     * Emitted from [Application] whenever the mouse enters the window client area
     */
    val entered: Event<MouseEvent>

    /**
     * mouse exited event
     *
     * Emitted from [Application] whenever the mouse exits the window client area
     */
    val exited: Event<MouseEvent>
}

/**
 * Mouse events
 */
class ApplicationMouse(private val application: () -> Application): MouseEvents {
    /**
     * The current mouse position
     */
    override var position: Vector2
        get() = application().cursorPosition
        set(value) {
            application().cursorPosition = value
        }

    /**
     * specifies if cursor should be visible
     */
    var cursorVisible: Boolean
        get() = application().cursorVisible
        set(value) {
            application().cursorVisible = value
        }

    var cursorHideMode: MouseCursorHideMode
        get() = application().cursorHideMode
        set(value) {
            application().cursorHideMode = value
        }

    /**
     * specifies the active cursor type, default is [CursorType.ARROW_CURSOR]
     */
    var cursorType: CursorType
        get() = application().cursorType
        set(value) {
            application().cursorType = value
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

    @Deprecated("use buttonUp")
    val clicked = buttonUp

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

/**
 * Keeps track of which mouse buttons are currently pressed.
 * Usage: `val mt = MouseTracker(mouse)`, then read `mt.pressedButtons`.
 * Replaces `MouseEvents.pressedButtons`.
 */
class MouseTracker(mouseEvents: MouseEvents) {
    private val mutablePressedButtons = mutableSetOf<MouseButton>()

    /**
     * set containing the names of the currently pressed buttons
     */
    val pressedButtons: Set<MouseButton> = mutablePressedButtons

    init {
        mouseEvents.buttonDown.listen {
            mutablePressedButtons.add(it.button)
        }

        mouseEvents.buttonUp.listen {
            mutablePressedButtons.remove(it.button)
        }
    }
}