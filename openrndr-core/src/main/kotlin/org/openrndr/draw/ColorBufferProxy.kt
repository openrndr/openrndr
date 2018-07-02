package org.openrndr.draw

import org.openrndr.events.Event
import org.openrndr.internal.ColorBufferLoader


class ColorBufferProxy(val url: String, val loader: ColorBufferLoader) {
    class ProxyEvent

    class Events {
        val loaded = Event<ProxyEvent>()
        val unloaded = Event<ProxyEvent>()
    }

    val events = Events()

    enum class State {
        NOT_LOADED,
        QUEUED,
        LOADED
    }

    internal var state = State.NOT_LOADED
    internal var realColorBuffer: ColorBuffer? = null

    var persistent = false
    val colorBuffer: ColorBuffer?
        get() {
            lastTouched = System.currentTimeMillis()
            if (state == State.NOT_LOADED) {
                queue()
            }
            return realColorBuffer
        }

    internal var lastTouched = 0L

    fun queue() {
        touch()
        if (state == State.NOT_LOADED) {
            loader.loadQueue.add(this)
            state = State.QUEUED
        }
    }

    fun touch() {
        lastTouched = System.currentTimeMillis()
    }
}