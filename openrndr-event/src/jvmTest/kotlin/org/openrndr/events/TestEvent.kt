package org.openrndr.events

import kotlin.test.Test
import kotlin.test.assertEquals

class TestSegment {
    @Test
    fun `event should have 1 listener`() {
        val event = Event<Int>()
        val listener = { _: Int -> }
        val ref = event.listen(listener)
        assertEquals(1, event.listeners.size)
    }

    @Test
    fun `event there should be 0 listeners after cancelling`() {
        val event = Event<Int>()
        val listener = { _: Int -> }
        val ref = event.listen(listener)
        event.cancel(ref)
        assertEquals(0, event.listeners.size)
    }

    @Test
    fun `event should have 3 listeners`() {
        val event = Event<Int>()
        val listener = { _: Int -> }

        fun listener2(e: Int) {
            e
        }

        event.listen(listener)
        event.listen {}
        event.listen(::listener2)
        assertEquals(3, event.listeners.size)
    }
}
