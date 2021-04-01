package org.openrndr.events

expect class Event<T>(name: String = "<unnamed-event>", postpone: Boolean = false) {
    val listeners: MutableList<(T) -> Unit>

    fun trigger(message: T)
    fun deliver()

    /**
     * add an event message listener
     * @param listener a function to be invoked when an event message is received
     * @return the [listener] that was passed in
     */
    fun listen(listener: (T) -> Unit): (T) -> Unit
    fun listen(listener: Event<T>): (T) -> Unit

    /**
     * cancel a listener
     */
    fun cancel(listener: (T) -> Unit)
    fun listenOnce(listener: (T) -> Unit)
    fun listenOnce(listener: Event<T>)
}

/**
 * add listener to [Iterable] of [Event] entries
 */
fun <T : Any> Iterable<Event<T>>.listen(listener: (T) -> Unit): (T) -> Unit {
    for (e in this) {
        e.listen(listener)
    }
    return listener
}