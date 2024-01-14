@file:JvmName("EventJVM")
package org.openrndr.events


import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList

private val logger = KotlinLogging.logger {}

private const val ignoreExceptionProperty = "org.openrndr.events.ignoreExceptions"
private val ignoreExceptions by lazy { System.getProperties().containsKey(ignoreExceptionProperty) }

/**
 * an event class
 * @param name a name for the event, this is used for logging and debugging purposes only, default is "<unnamed-event>"
 * @param postpone should message delivery for this event be postponed, default is false
 */
actual class Event<T> actual constructor(val name: String, var postpone: Boolean) {
    private var lastTriggered = Instant.ofEpochMilli(0L)

    /**
     * number of times this event is triggered
     */
    var triggerCount = 0L
        private set

    private val messages = mutableListOf<T>()
    actual val listeners: MutableList<(T) -> Unit> = CopyOnWriteArrayList()

    private val oneShotListeners = CopyOnWriteArrayList<(T) -> Unit>()


    val timeSinceLastTrigger: Duration
        get() = Duration.between(lastTriggered, Instant.now())

    private fun internalTrigger() {
        triggerCount++
        lastTriggered = Instant.now()
    }


    @Deprecated("use the postpone property directly")
    fun postpone(postpone: Boolean): Event<T> {
        this.postpone = postpone
        return this
    }

    /**
     * trigger the event
     *
     * When [Event.postpone] is false messages will be delivered immediately to the listeners.
     * However, when [Event.postpone] is true messages will not be delivered until [Event.deliver] is invoked.
     *
     * @param message the message to be delivered to the listeners
     */
    actual fun trigger(message: T) {
        internalTrigger()
        if (!postpone) {
            this.listeners.forEach { listener ->
                try {
                    listener(message)
                } catch (e: Throwable) {
                    logger.error { "Exception thrown in listener ('${name}'): ${e.javaClass.simpleName}; '${e.message}'" }
                    if (!ignoreExceptions)
                        throw e
                    else {
                        e.printStackTrace()
                    }
                }
            }
            this.oneShotListeners.forEach { listener ->
                try {
                    listener(message)
                } catch (e: Throwable) {
                    logger.error { "Exception thrown in one-shot listener ('${name}'): ${e.javaClass.simpleName}; '${e.message}'" }
                    if (!ignoreExceptions)
                        throw e
                    else {
                        e.printStackTrace()
                    }
                }
            }
            this.oneShotListeners.clear()
        } else {
            if (this.listeners.size > 0 || this.oneShotListeners.size > 0) {
                synchronized(messages) {
                    messages.add(message)
                }
            }
        }
    }

    /**
     * deliver postponed event messages
     *
     * Invoking this method will only deliver messages when [Event.postpone] is true, otherwise it will silently do
     * nothing.
     */
    actual fun deliver() {
        if (postpone) {
            synchronized(messages) {
                if (messages.size > 0) {
                    val copy = mutableListOf<T>()
                    copy.addAll(messages)
                    messages.clear()

                    copy.forEach { m ->
                        this.listeners.forEach { l ->
                            try {
                                l(m)
                            } catch (e: Exception) {
                                logger.error { "Exception thrown in listener ('${name}'): ${e.javaClass.simpleName}; '${e.message}'" }
                                if (!ignoreExceptions)
                                    throw e
                                else {
                                    e.printStackTrace()
                                }
                            }
                        }
                        this.oneShotListeners.forEach { l ->
                            try {
                                l(m)
                            } catch (e: Exception) {
                                logger.error { "Exception thrown in one-shot listener ('${name}'): ${e.javaClass.simpleName}; '${e.message}'" }
                                if (!ignoreExceptions)
                                    throw e
                                else {
                                    e.printStackTrace()
                                }
                            }
                        }
                        this.oneShotListeners.clear()
                    }
                }
            }
        }
    }
    actual fun listen(listener: (T) -> Unit): (T) -> Unit {
        listeners.add(listener)
        return listener
    }

    actual fun listen(listener: Event<T>): (T) -> Unit {
        val listenFunction = { m: T -> listener.trigger(m) }
        listeners.add(listenFunction)
        return listenFunction
    }
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
