package org.openrndr

import org.openrndr.events.Event
import org.openrndr.math.Vector2
import kotlin.jvm.JvmRecord


interface PointerEvents {

    val pointerDown: Event<PointerEvent>

    val pointerUp: Event<PointerEvent>

    val moved: Event<PointerEvent>

    val cancelled: Event<PointerEvent>

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

data class PointerEvent(val pointerID: Long, val position: Vector2, val displacement: Vector2, val pressure: Double)

@JvmRecord
data class Pointer(val pointerID: Long, val position: Vector2, val pressure: Double)

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