package org.openrndr

import mu.KotlinLogging
import org.openrndr.events.Event
import org.openrndr.math.Vector2

private val logger = KotlinLogging.logger {}

class MouseEvent(val position: Vector2, val rotation: Vector2, val dragDisplacement: Vector2, val type: MouseEventType, val button: MouseButton, val modifiers: Set<KeyModifier>, var propagationCancelled: Boolean = false) {
    fun cancelPropagation() {
        logger.debug { "Cancelling propagation of $type event" }
        propagationCancelled = true
    }
}

class Mouse(val application: ()->Application) {

    /**
     * The current mouse position
     */
    var position: Vector2
        get() = application().cursorPosition
        set(value) {
            application().cursorPosition = value
        }

    var cursorVisible: Boolean
        get() = application().cursorVisible
        set(value) {
            application().cursorVisible = value
        }

    val buttonDown = Event<MouseEvent>("mouse-button-down").postpone(true)
    val buttonUp = Event<MouseEvent>("mouse-button-up").postpone(true)
    val dragged = Event<MouseEvent>("mouse-dragged").postpone(true)
    val moved = Event<MouseEvent>("mouse-moved").postpone(true)
    val scrolled = Event<MouseEvent>("mouse-scrolled").postpone(true)

    val clicked = buttonUp
    val entered = Event<MouseEvent>("mouse-entered").postpone(true)
    val exited = Event<MouseEvent>("mouse-exited").postpone(true)

    var pressedButtons = mutableSetOf<MouseButton>()
}
