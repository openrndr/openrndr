package org.openrndr.events

import io.github.oshai.kotlinlogging.KotlinLogging


private val logger = KotlinLogging.logger {}

actual class Event<T> actual constructor(val name: String, val postpone: Boolean) {
    actual val listeners: MutableList<(T) -> Unit> = mutableListOf()
    private val oneShotListeners: MutableList<(T) -> Unit> = mutableListOf()
    private val messages = mutableListOf<T>()

    actual fun trigger(message: T) {
        messages.add(message)
        deliver()
    }

    actual fun deliver() {
        run {
            if (messages.size > 0) {
                val copy = mutableListOf<T>()
                copy.addAll(messages)
                messages.clear()

                copy.forEach { m ->
                    this.listeners.forEach { l ->
                        try {
                            l(m)
                        } catch (e: Exception) {
                            logger.error { "Exception thrown in listener ('${name}'): ${e::class.simpleName}; '${e.message}'" }
                            throw e
                        }
                    }
                    this.oneShotListeners.forEach { l ->
                        try {
                            l(m)
                        } catch (e: Exception) {
                            logger.error { "Exception thrown in one-shot listener ('${name}'): ${e::class.simpleName}; '${e.message}'" }
                            throw e
                        }
                    }
                    this.oneShotListeners.clear()
                }
            }
        }
    }

    /**
     * add an event message listener
     * @param listener a function to be invoked when an event message is received
     * @return the [listener] that was passed in
     */
    actual fun listen(listener: (T) -> Unit): (T) -> Unit {
        listeners.add(listener)
        return listener
    }

    actual fun listen(listener: Event<T>): (T) -> Unit {
        val listenFunction = { m: T -> listener.trigger(m) }
        listeners.add(listenFunction)
        return listenFunction
    }

    /**
     * cancel a listener
     */
    actual fun cancel(listener: (T) -> Unit) {
        listeners.remove(listener)
    }

    actual fun listenOnce(listener: (T) -> Unit) {
        oneShotListeners.add(listener)
    }

    actual fun listenOnce(listener: Event<T>) {
        oneShotListeners.add { v1 -> listener.trigger(v1) }
    }

}
