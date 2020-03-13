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

class Mouse(val application: Application) {

    /**
     * The current mouse position
     */
    var position: Vector2
        get() = application.cursorPosition
        set(value) {
            application.cursorPosition = value
        }

    var cursorVisible: Boolean
        get() = application.cursorVisible
        set(value) {
            application.cursorVisible = value
        }

    val buttonDown = Event<MouseEvent>().postpone(true)
    val buttonUp = Event<MouseEvent>().postpone(true)
    val dragged = Event<MouseEvent>().postpone(true)
    val moved = Event<MouseEvent>().postpone(true)
    val scrolled = Event<MouseEvent>().postpone(true)
    val clicked = Event<MouseEvent>().postpone(true)
    val entered = Event<MouseEvent>().postpone(true)
    val left = Event<MouseEvent>().postpone(true)
}
