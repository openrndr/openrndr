package org.openrndr.internal

import io.github.oshai.kotlinlogging.KotlinLogging
import org.openrndr.draw.ColorBufferProxy
import org.openrndr.draw.loadImage
import java.io.IOException
import java.util.WeakHashMap

private val logger = KotlinLogging.logger {}

/** The number of milliseconds after which proxies that are not persistent and not touched are unloaded */
private const val UNLOAD_DELAY = 5000L

val colorBufferLoader by lazy {
    ColorBufferLoader.create()
}

/**
 * Manages loading and unloading of `ColorBufferProxy` objects, enabling asynchronous
 * fetching of color buffer resources from URLs or other sources. This class also
 * handles caching and resource cleanup for loaded buffers to optimize usage.
 */
class ColorBufferLoader {
    private val loadQueue = ArrayDeque<ColorBufferProxy>()
    private val unloadQueue = ArrayDeque<ColorBufferProxy>()

    private val urlItems = WeakHashMap<String, ColorBufferProxy>()

    /** The loader thread waits on this until a proxy is queued or an unload is due */
    private val signal = Object()

    private fun wake() {
        synchronized(signal) { signal.notifyAll() }
    }

    fun queue(colorBufferProxy: ColorBufferProxy) {
        synchronized(loadQueue) { loadQueue.add(colorBufferProxy) }
        synchronized(unloadQueue) { unloadQueue.remove(colorBufferProxy) }
        wake()
    }

    fun loadFromUrl(url: String, queue: Boolean = true, persistent: Boolean = false): ColorBufferProxy =
        urlItems.getOrPut(url) {
            val proxy = ColorBufferProxy(url, this, persistent).apply {
                lastTouched = System.currentTimeMillis()
                state = if (queue) {
                    ColorBufferProxy.State.QUEUED
                } else {
                    ColorBufferProxy.State.NOT_LOADED
                }
            }
            if (queue) {
                queue(proxy)
            }
            proxy
        }

    /**
     * Cancels the loading or manages the unloading of a specified ColorBufferProxy instance.
     *
     * If the proxy is in the load queue, it is removed. If the proxy's state is LOADED, it is
     * added to the unload queue for further processing.
     *
     * @param proxy the ColorBufferProxy instance to be canceled. If it is queued for loading,
     * it will be removed from the load queue. If it is loaded, it will be scheduled for unloading.
     */
    fun cancel(proxy: ColorBufferProxy) {
        synchronized(loadQueue) {
            loadQueue.remove(proxy)
        }
        synchronized(unloadQueue) {
            if (proxy.state == ColorBufferProxy.State.LOADED) {
                unloadQueue.add(proxy)
            }
        }
        wake()
    }

    /**
     * Unloads the proxies that were not touched for [UNLOAD_DELAY] milliseconds
     * @return the number of milliseconds until the next unload is due, null when nothing is waiting to be unloaded
     */
    private fun unloadUntouched(): Long? {
        synchronized(unloadQueue) {
            while (unloadQueue.isNotEmpty()) {
                val proxy = unloadQueue.minWith(compareBy { it.lastTouched })
                val remaining = proxy.lastTouched + UNLOAD_DELAY - System.currentTimeMillis()
                if (remaining >= 0) {
                    return remaining + 1
                }
                unloadQueue.remove(proxy)
                proxy.colorBuffer?.destroy()
                proxy.colorBuffer = null
                proxy.state = ColorBufferProxy.State.NOT_LOADED
                proxy.events.unloaded.trigger(ColorBufferProxy.ProxyEvent())
                logger.debug { "unloaded $proxy" }
            }
            return null
        }
    }

    companion object {
        fun create(): ColorBufferLoader {
            val loader = ColorBufferLoader()
            Driver.instance.createResourceThread {
                logger.debug { "ColorBufferLoader thread started" }
                while (true) {
                    val proxy = synchronized(loader.loadQueue) {
                        if (loader.loadQueue.isEmpty()) {
                            null
                        } else {
                            loader.loadQueue.forEach { it.lastTouchedShadow = it.lastTouched }
                            val best = loader.loadQueue.minWith(compareBy({ it.priority }, { -it.lastTouchedShadow }))
                            loader.loadQueue.remove(best)
                            best
                        }
                    }
                    if (proxy != null) {
                        try {
                            val cb = loadImage(proxy.url)//ColorBuffer.fromUrl(proxy.url)
                            // the upload is complete before other contexts use the color buffer
                            Driver.instance.finish()
                            proxy.colorBuffer = cb
                            proxy.state = ColorBufferProxy.State.LOADED
                            if (!proxy.persistent) {
                                synchronized(loader.unloadQueue) {
                                    loader.unloadQueue.add(proxy)
                                }
                            }
                            logger.debug { "successfully loaded $proxy" }
                            proxy.events.loaded.trigger(ColorBufferProxy.ProxyEvent())
                        } catch (e: IOException) {
                            logger.error { e.stackTrace }
                            proxy.state = ColorBufferProxy.State.RETRY
                            proxy.events.retry.trigger(ColorBufferProxy.ProxyEvent())
                        } catch (e: Exception) {
                            logger.error { "unexpected exception while loading $proxy" }
                            logger.error { e.stackTrace }
                            proxy.state = ColorBufferProxy.State.ERROR
                            proxy.events.error.trigger(ColorBufferProxy.ProxyEvent())
                            // maybe come to a grinding halt here?
                        }
                    }

                    val untilUnload = loader.unloadUntouched()

                    // wait for a proxy to be queued or for the next unload, instead of polling
                    synchronized(loader.signal) {
                        val idle = synchronized(loader.loadQueue) { loader.loadQueue.isEmpty() }
                        if (idle) {
                            loader.signal.wait(untilUnload ?: 0L)
                        }
                    }
                }
            }
            return loader
        }
    }
}