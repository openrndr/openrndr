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

    @Test
    fun `one shot listeners are listed and removed after a message`() {
        val event = Event<Int>()
        assertEquals(false, event.hasListeners)
        event.listenOnce { }
        assertEquals(1, event.oneShotListeners.size)
        assertEquals(0, event.listeners.size)
        assertEquals(true, event.hasListeners)
        event.trigger(1)
        assertEquals(0, event.oneShotListeners.size)
        assertEquals(false, event.hasListeners)
    }

    @Test
    fun `hasListeners follows listeners`() {
        val event = Event<Int>()
        val ref = event.listen { }
        assertEquals(true, event.hasListeners)
        event.cancel(ref)
        assertEquals(false, event.hasListeners)
    }
}
