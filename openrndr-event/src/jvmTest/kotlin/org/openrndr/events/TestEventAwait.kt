package org.openrndr.events

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.milliseconds

class TestEventAwait {
    @Test
    fun awaitResumesWithTheMessage() = runBlocking {
        val event = Event<Int>()
        val result = async(start = CoroutineStart.UNDISPATCHED) { event.await() }
        event.trigger(42)
        assertEquals(42, result.await())
        assertFalse(event.hasListeners)
    }

    @Test
    fun awaitChoiceRemovesTheListenersOfTheOtherEvents() = runBlocking {
        val a = Event<String>()
        val b = Event<String>()
        val c = Event<String>()
        repeat(3) {
            val result = async(start = CoroutineStart.UNDISPATCHED) { awaitChoice(a, b, c) }
            b.trigger("b")
            assertEquals("b", result.await())
        }
        // the losing events do not collect listeners
        assertFalse(a.hasListeners)
        assertFalse(c.hasListeners)
    }

    @Test
    fun awaitChoiceWithIndex() = runBlocking {
        val events = List(3) { Event<Unit>() }
        val result = async(start = CoroutineStart.UNDISPATCHED) { awaitChoiceWithIndex(*events.toTypedArray()) }
        events[2].trigger(Unit)
        assertEquals(2, result.await().index)
    }

    @Test
    fun awaitChoiceOfOptionsWithDifferentMessageTypes() = runBlocking {
        val selected = Event<Int>()
        val submitted = Event<Unit>()
        val first = async(start = CoroutineStart.UNDISPATCHED) { awaitChoice(selected.option { it }, submitted.option { null }) }
        selected.trigger(7)
        assertEquals(7, first.await())
        val second = async(start = CoroutineStart.UNDISPATCHED) { awaitChoice(selected.option { it }, submitted.option { null }) }
        submitted.trigger(Unit)
        assertNull(second.await())
        assertFalse(selected.hasListeners)
        assertFalse(submitted.hasListeners)
    }

    @Test
    fun cancellationRemovesTheListeners() = runBlocking {
        val a = Event<Int>()
        val b = Event<Int>()
        val waiting = async(start = CoroutineStart.UNDISPATCHED) { awaitChoice(a, b) }
        waiting.cancel()
        yield()
        assertFalse(a.hasListeners)
        assertFalse(b.hasListeners)
    }

    @Test
    fun timeouts() = runBlocking {
        val event = Event<String?>()
        assertEquals("timeout", event.await(20.milliseconds) { "timeout" })
        assertFalse(event.hasListeners)

        // a null message is not a timeout
        val result = async(start = CoroutineStart.UNDISPATCHED) { event.await(1000.milliseconds) { "timeout" } }
        event.trigger(null)
        assertNull(result.await())

        val a = Event<Int>()
        val b = Event<Int>()
        assertNull(awaitChoiceWithIndex(20.milliseconds, a, b))
        assertEquals(-1, awaitChoice(20.milliseconds, a, b) { -1 })
        assertFalse(a.hasListeners)
        assertFalse(b.hasListeners)
    }

    @Test
    fun eventsTriggeredOnOtherThreadsResumeOnce() = runBlocking {
        repeat(200) {
            val a = Event<Int>()
            val b = Event<Int>()
            val result = async(start = CoroutineStart.UNDISPATCHED) { awaitChoice(a, b) }
            val threads = listOf(Thread { a.trigger(1) }, Thread { b.trigger(2) })
            threads.forEach { it.start() }
            threads.forEach { it.join() }
            val value = result.await()
            assert(value == 1 || value == 2)
            assertFalse(a.hasListeners)
            assertFalse(b.hasListeners)
        }
    }
}
