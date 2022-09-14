package org.openrndr.draw

import mu.KotlinLogging
import org.openrndr.events.Event
import org.openrndr.internal.ColorBufferLoader
import org.openrndr.internal.colorBufferLoader
import java.io.File
import java.net.MalformedURLException
import java.net.URL

private val logger = KotlinLogging.logger {}

data class ColorBufferProxy(val url: String, val loader: ColorBufferLoader, val persistent: Boolean) {
    class ProxyEvent

    class Events {
        val loaded = Event<ProxyEvent>("proxy-loaded", postpone = false)
        val unloaded = Event<ProxyEvent>("proxy-unloaded", postpone = false)
        val retry = Event<ProxyEvent>("proxy-retry", postpone = false)
        val error = Event<ProxyEvent>("proxy-error", postpone = false)
    }

    val events = Events()

    enum class State {
        NOT_LOADED,
        QUEUED,
        LOADED,
        RETRY,
        ERROR
    }

    /**
     * The proxy priority index, proxies with lower priority index will be loaded first
     */
    var priority = 100

    var colorBuffer: ColorBuffer? = null
        get() {
            lastTouched = System.currentTimeMillis()
            if (state == State.NOT_LOADED) {
                queue()
            }
            return field
        }
        internal set

    var state = State.NOT_LOADED
        internal set

    internal var lastTouched = 0L
    internal var lastTouchedShadow = 0L

    fun cancel() {
        touch()
        if (state == State.QUEUED) {
            loader.cancel(this)
            state = State.NOT_LOADED
            logger.debug {
                "canceled $this"
            }
        } else {
            logger.warn {
                "proxy is not queued, so it cannot be canceled: $this"
            }
        }
    }

    fun queue() {
        touch()
        if (state == State.NOT_LOADED) {
            loader.queue(this)
            state = State.QUEUED
        }
    }

    fun touch() {
        lastTouched = System.currentTimeMillis()
    }

    fun retry() {
        if (state == State.RETRY) {
            logger.debug {
                "retry requested"
            }
            state = State.NOT_LOADED
        } else {
            logger.warn {
                "proxy is not in retry"
            }
        }
    }
}

fun imageProxy(file: File, @Suppress("UNUSED_PARAMETER") queue: Boolean = true, persistent: Boolean = false): ColorBufferProxy {
    return imageProxy(file.toString(), persistent)
}

fun imageProxy(fileOrUrl: String, queue: Boolean = true, persistent: Boolean = false): ColorBufferProxy {
    return try {
        if (!fileOrUrl.startsWith("data:")) {
            URL(fileOrUrl)
            colorBufferLoader.loadFromUrl(fileOrUrl, queue, persistent)
        } else {
            error("data scheme not supported")
        }
    } catch (e: MalformedURLException) {
        val file = File(fileOrUrl)
        val url = file.toURI().toURL()
        colorBufferLoader.loadFromUrl(url.toString(), queue, persistent)
    }
}