package org.openrndr.draw

import mu.KotlinLogging
import org.openrndr.events.Event
import org.openrndr.internal.ColorBufferLoader

private val logger = KotlinLogging.logger {}

data class ColorBufferProxy(val url: String, val loader: ColorBufferLoader, val persistent: Boolean) {
    class ProxyEvent

    class Events {
        val loaded = Event<ProxyEvent>()
        val unloaded = Event<ProxyEvent>()
        val retry = Event<ProxyEvent>()
        val error = Event<ProxyEvent>()
    }

    val events = Events()

    enum class State {
        NOT_LOADED,
        QUEUED,
        LOADED,
        RETRY,
        ERROR
    }

    internal var realState = State.NOT_LOADED
    internal var realColorBuffer: ColorBuffer? = null

    val state get() = realState

    val colorBuffer: ColorBuffer?
        get() {
            lastTouched = System.currentTimeMillis()
            if (realState == State.NOT_LOADED) {
                queue()
            }
            return realColorBuffer
        }

    internal var lastTouched = 0L
    internal var lastTouchedShadow = 0L

    fun queue() {
        touch()
        if (realState == State.NOT_LOADED) {
            loader.queue(this)
            realState = State.QUEUED
        }
    }

    fun touch() {
        lastTouched = System.currentTimeMillis()
    }

    fun retry() {
        if (realState == State.RETRY) {
            logger.debug {
                "retry requested"
            }
            realState = State.NOT_LOADED
        } else {
            logger.warn {
                "proxy is not in retry"
            }
        }
    }
}