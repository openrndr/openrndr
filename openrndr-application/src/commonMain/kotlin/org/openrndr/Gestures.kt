package org.openrndr

import org.openrndr.events.Event
import org.openrndr.math.Vector2
import kotlin.jvm.JvmRecord


/**
 * Represents an event triggered by a pinch gesture, typically used in touch interfaces.
 *
 * @property scale The scale factor associated with the pinch gesture. A value greater than 1.0
 * indicates a zoom-in gesture, while a value less than 1.0 indicates a zoom-out gesture.
 */
@JvmRecord
data class PinchEvent(val scale:Double)


/**
 * Interface defining the contract for gesture-related events such as pinch gestures.
 * Provides events to handle the start, update, and end of a pinch gesture.
 * This is typically used in touch input scenarios for interactive applications.
 */
interface GestureEvents {

    /**
     * Event triggered to indicate the beginning of a pinch gesture.
     *
     * Listeners can register to this event to react and handle the start of a pinch gesture.
     */
    val pinchStarted: Event<PinchEvent>

    /**
     * Event triggered to indicate an update in the pinch gesture.
     *
     * This event is dispatched when a pinch gesture, typically involving two or more touch pointers,
     * changes in scale. The associated [PinchEvent] provides details about the updated scale factor
     * of the gesture.
     *
     * Listeners can register to this event to react and handle real-time updates to the pinch gesture,
     * such as zooming in or out based on the gesture's scale.
     */
    val pinchUpdated: Event<PinchEvent>

    /**
     * Event triggered to indicate the completion of a pinch gesture.
     **
     * Listeners can register to this event to perform cleanup or handle any final actions
     * required upon the completion of a pinch gesture.
     */
    val pinchEnded: Event<PinchEvent>

    /**
     * Delivers gesture-related events to their respective listeners.
     */
    fun deliver() {
        pinchStarted.deliver()
        pinchUpdated.deliver()
        pinchEnded.deliver()
    }
}

class Gestures: GestureEvents {
    override val pinchStarted: Event<PinchEvent> = Event("pinch-started", postpone = true)
    override val pinchUpdated: Event<PinchEvent> = Event("pinch-updated", postpone = true)
    override val pinchEnded: Event<PinchEvent> = Event("pinch-ended", postpone = true)
}

