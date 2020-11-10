package org.openrndr.events

import mu.KotlinLogging
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList

private val logger = KotlinLogging.logger {}

private const val ignoreExceptionProperty = "org.openrndr.events.ignoreExceptions"
private val ignoreExceptions by lazy { System.getProperties().containsKey(ignoreExceptionProperty) }

/**
 * an event class
 * @param name a name for the event, this is is used for logging and debugging purposes only, default is "<unnamed-event>"
 * @param postpone should message delivery for this event be postponed, default is false
 */
class Event<T>(val name: String = "<unnamed-event>", var postpone: Boolean = false) {
    private var lastTriggered = Instant.ofEpochMilli(0L)

    /**
     * number of times this event is triggered
     */
    var triggerCount = 0L
        private set

    private val messages = mutableListOf<T>()
    val listeners: MutableList<(T) -> Unit> = CopyOnWriteArrayList()

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
    fun trigger(message: T) {
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
    fun deliver() {
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


    @Deprecated("Switching away from JavaRX", replaceWith = ReplaceWith("listen"))
    fun subscribe(listener: (T) -> Unit): (T) -> Unit = listen(listener)

    @Deprecated("Switching away from JavaRX", replaceWith = ReplaceWith("listen"))
    fun onNext(event: T) {
        trigger(event)
    }

    /**
     * add an event message listener
     * @param listener a function to be invoked when an event message is received
     * @return the [listener] that was passed in
     */
    fun listen(listener: (T) -> Unit): (T) -> Unit {
        listeners.add(listener)
        return listener
    }

    fun listen(listener: Event<T>): (T) -> Unit {
        val listenFunction = { m: T -> listener.trigger(m); Unit }
        listeners.add(listenFunction)
        return listenFunction
    }

    /**
     * cancel a listener
     */
    fun cancel(listener: (T) -> Unit) {
        listeners.remove(listener)
    }

    fun listenOnce(listener: (T) -> Unit) {
        oneShotListeners.add(listener)
    }

    fun listenOnce(listener: Event<T>) {
        oneShotListeners.add({ v1 -> listener.trigger(v1) })
    }
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