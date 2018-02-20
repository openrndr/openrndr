package org.openrndr.events

import java.time.Duration
import java.time.Instant
import java.util.ArrayList

class Event<T> {
    internal var lastTriggered = Instant.ofEpochMilli(0L)
    internal var triggerCount: Long = 0
    internal var name = "<nameless-event>"
    internal var signature: Array<Class<*>> = emptyArray()

    private var filter: ((T)->Boolean)? = null

    private val messages = ArrayList<Message>()
    private val listeners = ArrayList< (T)->Unit>()

    private val oneShotListeners = ArrayList<(T)->Unit>()

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

            //logger.trace("event {} triggered with values ({}), {} receivers", name, v1, this.listeners.size)

            if (!postpone) {
                this.listeners.forEach { l -> l(v1) }
                this.oneShotListeners.forEach { l ->
                    try {
                        l(v1)
                    } catch (e: Exception) {
                        e.printStackTrace()
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
                messages.forEach { m ->
                    this.listeners.forEach { l ->
                        try {
                            l(m.v1)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    this.oneShotListeners.forEach { l ->
                        try {
                            l(m.v1)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    this.oneShotListeners.clear()
                }
                messages.clear()
            }
        }

    }

    fun filter(filter: (T)->Boolean): Event<T> {
        val filtered = Event<T>()
        filtered.filter = filter

        listen(filtered)
        return filtered
    }

    fun listen(listener: (T)->Unit): Event<T> {
        listeners.add(listener)
        return this
    }

    fun listen(listener: Event<T>): Event<T> {
        listeners.add({ m -> listener.trigger(m) })
        return this
    }

    fun listenOnce(listener: (T)->Unit): Event<T> {
        oneShotListeners.add(listener)
        return this
    }

    fun listenOnce(listener: Event<T>): Event<T> {
        oneShotListeners.add({ v1 -> listener.trigger(v1) })
        return this
    }

    fun signature(t1: Class<T>): Event<T> {
        signature = arrayOf(t1)
        return this
    }
}
