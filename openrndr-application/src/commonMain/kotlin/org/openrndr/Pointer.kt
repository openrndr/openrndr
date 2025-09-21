package org.openrndr

import org.openrndr.events.Event
import org.openrndr.math.Vector2
import kotlin.jvm.JvmRecord

/**
 * The `PointerEvents` interface manages the lifecycle of pointer-related events
 * such as pointer press, release, movement, and cancellation.
 *
 * It provides properties corresponding to specific types of pointer events and
 * a method to deliver these events to their respective listeners.
 */
interface PointerEvents {

    val pointerDown: Event<PointerEvent>

    val pointerUp: Event<PointerEvent>

    val moved: Event<PointerEvent>

    val cancelled: Event<PointerEvent>

    /**
     * Delivers pointer-related events to their respective listeners.
     */
    fun deliver() {
        pointerDown.deliver()
        pointerUp.deliver()
        moved.deliver()
        cancelled.deliver()
    }
}

class Pointers : PointerEvents {
    override val pointerDown: Event<PointerEvent> = Event("pointer-down")
    override val pointerUp: Event<PointerEvent> = Event("pointer-up")
    override val moved: Event<PointerEvent> = Event("pointer-moved")
    override val cancelled: Event<PointerEvent> = Event("pointer-cancelled")
}
@JvmRecord
data class PointerEvent(val pointerID: Long, val position: Vector2, val displacement: Vector2, val pressure: Double)

@JvmRecord
data class Pointer(val pointerID: Long, val position: Vector2, val pressure: Double)

/**
 * Tracks the state of pointer interactions in an application.
 *
 * The `PointerTracker` class listens to pointer events provided by a `PointerEvents` object
 * and maintains a map of active pointers. Each pointer is uniquely identified by its ID and
 * stores information such as its position and pressure.
 *
 * The class subscribes to the following pointer events:
 * - `pointerDown`: Adds a new pointer to the tracker when a pointer is pressed down.
 * - `moved`: Updates the state of an existing pointer when it moves.
 * - `pointerUp`: Removes a pointer from the tracker when it is released.
 *
 * @constructor Creates a `PointerTracker` instance that starts listening to the provided `PointerEvents` object.
 * @param pointerEvents The source of pointer events to track.
 *
 * @property pointers A mutable map of active pointers. The keys represent the unique pointer IDs,
 * and the values are `Pointer` objects containing the state of each pointer.
 */
class PointerTracker(pointerEvents: PointerEvents) {
    val pointers: MutableMap<Long, Pointer> = mutableMapOf()

    init {
        pointerEvents.pointerDown.listen {
            pointers[it.pointerID] = Pointer(it.pointerID, it.position, it.pressure)
        }

        pointerEvents.moved.listen {
            pointers[it.pointerID] = Pointer(it.pointerID, it.position, it.pressure)
        }

        pointerEvents.pointerUp.listen {
            pointers.remove(it.pointerID)
        }
    }
}