package org.openrndr.events

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.coroutines.resume
import kotlin.time.Duration

/**
 * An [event] paired with a [transform] that maps its message to a common result type [R], so that events with
 * different message types can be raced in one [awaitChoice]
 */
class ChoiceOption<T, R>(val event: Event<T>, val transform: (T) -> R)

/**
 * Creates a [ChoiceOption] for this event that maps its message through [transform]
 */
fun <T, R> Event<T>.option(transform: (T) -> R): ChoiceOption<T, R> = ChoiceOption(this, transform)

/** A one-shot listener on an event, kept with its event so it can be removed again */
private class Registration<T, R>(val option: ChoiceOption<T, R>, onMessage: (R) -> Unit) {
    val listener: (T) -> Unit = { message -> onMessage(option.transform(message)) }
    fun register() {
        option.event.listenOnce(listener)
    }

    fun unregister() = option.event.cancel(listener)
}

/**
 * Suspends until the first of [options] triggers, resuming with its transformed message. The events may have
 * different message types, see [option].
 *
 * When the first event triggers, or the coroutine is cancelled, the listeners on all events are removed, events
 * that trigger later are not seen.
 */
@OptIn(ExperimentalAtomicApi::class)
suspend fun <R> awaitChoice(vararg options: ChoiceOption<*, R>): R = suspendCancellableCoroutine { continuation ->
    // events can trigger on other threads, the first one resumes the coroutine
    val resumed = AtomicBoolean(false)
    lateinit var registrations: List<Registration<*, R>>
    fun unregisterAll() = registrations.forEach { it.unregister() }
    registrations = options.map { option ->
        @Suppress("UNCHECKED_CAST")
        Registration(option as ChoiceOption<Any?, R>) { result ->
            if (resumed.compareAndSet(expectedValue = false, newValue = true)) {
                unregisterAll()
                continuation.resume(result)
            }
        }
    }
    continuation.invokeOnCancellation { unregisterAll() }
    for (registration in registrations) {
        registration.register()
    }
    // an event that triggered while the listeners were registered leaves the later listeners registered
    if (resumed.load()) {
        unregisterAll()
    }
}

/**
 * Suspends until the first of [events] triggers, resuming with its message. When the first event triggers, or the
 * coroutine is cancelled, the listeners on all events are removed, events that trigger later are not seen.
 */
suspend fun <T> awaitChoice(vararg events: Event<T>): T =
    awaitChoice(*Array(events.size) { events[it].option { message -> message } })

/**
 * Suspends until the first of [events] triggers, resuming with its index in [events] and its message.
 */
suspend fun <T> awaitChoiceWithIndex(vararg events: Event<T>): IndexedValue<T> =
    awaitChoice(*Array(events.size) { index -> events[index].option { message -> IndexedValue(index, message) } })

/**
 * Suspends until this event triggers once, resuming with its message. The listener is removed when the coroutine is
 * cancelled.
 */
suspend fun <T> Event<T>.await(): T = awaitChoice(this)

/** A message, also one that is null, wrapped so that a timeout can be told apart from it */
private class Message<T>(val value: T)

/**
 * Suspends until this event triggers once, resuming with its message, or with the result of [onTimeout] when
 * [timeout] passes first. A message that is null is returned as such, not mistaken for a timeout.
 */
suspend fun <T> Event<T>.await(timeout: Duration, onTimeout: () -> T): T {
    val message = withTimeoutOrNull(timeout) { Message(await()) }
    return if (message != null) message.value else onTimeout()
}

/**
 * Suspends until the first of [events] triggers, resuming with its message, or with the result of [onTimeout] when
 * [timeout] passes first
 */
suspend fun <T> awaitChoice(timeout: Duration, vararg events: Event<T>, onTimeout: () -> T): T {
    val message = withTimeoutOrNull(timeout) { Message(awaitChoice(*events)) }
    return if (message != null) message.value else onTimeout()
}

/**
 * Suspends until the first of [events] triggers, resuming with its index in [events] and its message, or with null
 * when [timeout] passes first
 */
suspend fun <T> awaitChoiceWithIndex(timeout: Duration, vararg events: Event<T>): IndexedValue<T>? =
    withTimeoutOrNull(timeout) { awaitChoiceWithIndex(*events) }
