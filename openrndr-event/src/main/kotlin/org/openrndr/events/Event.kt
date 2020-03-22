package org.openrndr.events

import mu.KotlinLogging
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList

private val logger = KotlinLogging.logger {}

private const val ignoreExceptionProperty = "org.openrndr.events.ignoreExceptions"
private val ignoreExceptions by lazy { System.getProperties().containsKey(ignoreExceptionProperty) }

class Event<T> {
    private var lastTriggered = Instant.ofEpochMilli(0L)
    private var triggerCount: Long = 0
    private var name = "<nameless-event>"
    private var signature: Array<Class<*>> = emptyArray()

    private var filter: ((T) -> Boolean)? = null


    private val messages = mutableListOf<Message>()
    val listeners: MutableList<(T) -> Unit> = CopyOnWriteArrayList<(T) -> Unit>()

    private val oneShotListeners = CopyOnWriteArrayList<(T) -> Unit>()

    private var postpone = false

    constructor()

    constructor(name: String) {
        name(name)
    }

    fun name(name: String): Event<T> {
        this.name = name
        return this
    }

    fun sinceLastTrigger(): Duration {
        return Duration.between(lastTriggered, Instant.now())
    }

    fun triggerCount(): Long {
        return triggerCount
    }

    protected fun internalTrigger() {
        triggerCount++
        lastTriggered = Instant.now()
    }

    fun name(): String {
        return name
    }

    fun signature(): Array<Class<*>> {
        return signature
    }

    @FunctionalInterface
    interface Filter<T1> {
        fun filter(v1: T1): Boolean
    }

    @FunctionalInterface
    interface Map<T1, R1> {
        fun map(v1: T1): R1
    }

    private inner class Message internal constructor(internal var v1: T)


    fun postpone(postpone: Boolean): Event<T> {
        this.postpone = postpone
        return this
    }

    fun trigger(v1: T): Event<T> {
        internalTrigger()

        if (filter == null || filter!!.invoke(v1)) {
            if (!postpone) {
                this.listeners.forEach { l ->
                    try {
                        l(v1)
                    } catch(e : Throwable) {
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
                        l(v1)
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
                        messages.add(Message(v1))
                    }
                }
            }
        }
        return this
    }

    fun deliver() {
        if (postpone) {
            synchronized(messages) {

                if (messages.size > 0) {
                    val copy = ArrayList<Message>()
                    copy.addAll(messages)
                    messages.clear()

                    copy.forEach { m ->
                        this.listeners.forEach { l ->
                            try {
                                l(m.v1)
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
                                l(m.v1)
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

    fun filter(filter: (T) -> Boolean): Event<T> {
        val filtered = Event<T>()
        filtered.filter = filter

        listen(filtered)
        return filtered
    }


    @Deprecated("Switching away from JavaRX", replaceWith = ReplaceWith("listen"))
    fun subscribe(listener: (T) -> Unit): (T) -> Unit = listen(listener)


    @Deprecated("Switching away from JavaRX", replaceWith = ReplaceWith("listen"))
    fun onNext(event : T) {
        trigger(event)
    }


    fun listen(listener: (T) -> Unit): (T) -> Unit {
        listeners.add(listener)
        return listener
    }

    fun listen(listener: Event<T>): (T) -> Unit {
        val listenFunction = { m: T -> listener.trigger(m); Unit }
        listeners.add(listenFunction)
        return listenFunction
    }

    fun cancel(listener: (T) -> Unit) {
        listeners.remove(listener)
    }

    fun listenOnce(listener: (T) -> Unit) {
        oneShotListeners.add(listener)
    }

    fun listenOnce(listener: Event<T>) {
        oneShotListeners.add({ v1 -> listener.trigger(v1) })
    }

    fun signature(t1: Class<T>): Event<T> {
        signature = arrayOf(t1)
        return this
    }
}
