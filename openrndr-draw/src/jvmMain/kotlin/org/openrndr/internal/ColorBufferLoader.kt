package org.openrndr.internal

import io.github.oshai.kotlinlogging.KotlinLogging
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.ColorBufferProxy
import java.io.IOException
import java.util.WeakHashMap

private val logger = KotlinLogging.logger {}

val colorBufferLoader by lazy {
    ColorBufferLoader.create()
}

class ColorBufferLoader {
    private val loadQueue = ArrayDeque<ColorBufferProxy>()
    private val unloadQueue = ArrayDeque<ColorBufferProxy>()

    private val urlItems = WeakHashMap<String, ColorBufferProxy>()

    fun queue(colorBufferProxy: ColorBufferProxy) {
        synchronized(loadQueue) { loadQueue.add(colorBufferProxy) }
        synchronized(unloadQueue) { unloadQueue.remove(colorBufferProxy) }
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

    fun cancel(proxy: ColorBufferProxy) {
        synchronized(loadQueue) {
            loadQueue.remove(proxy)
        }
        synchronized(unloadQueue) {
            if (proxy.state == ColorBufferProxy.State.LOADED) {
                unloadQueue.add(proxy)
            }
        }
    }

    companion object {
        fun create(): ColorBufferLoader {
            val loader = ColorBufferLoader()
            Driver.instance.createResourceThread {
                logger.debug { "ColorBufferLoader thread started" }
                while (true) {
                    if (loader.loadQueue.isNotEmpty()) {
                        val proxy = synchronized(loader.loadQueue) {
                            loader.loadQueue.forEach { it.lastTouchedShadow = it.lastTouched }
                            val best = loader.loadQueue.minWith(compareBy({ it.priority }, { -it.lastTouchedShadow }))
                            loader.loadQueue.remove(best)
                            best
                        }
                        try {
                            val cb = ColorBuffer.fromUrl(proxy.url)
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

                    synchronized(loader.unloadQueue) {
                        if (loader.unloadQueue.isNotEmpty()) {
                            val proxy = loader.unloadQueue.minWith(compareBy { it.lastTouched })
                            val dt = System.currentTimeMillis() - proxy.lastTouched
                            if (dt > 5000) {
                                loader.unloadQueue.remove(proxy)
                                proxy.colorBuffer?.destroy()
                                proxy.colorBuffer = null
                                proxy.state = ColorBufferProxy.State.NOT_LOADED
                                proxy.events.unloaded.trigger(ColorBufferProxy.ProxyEvent())
                                logger.debug { "unloaded $proxy" }
                            }
                        }
                    }
                    Driver.instance.clear(0.0, 0.0, 0.0, 1.0)
                    Thread.sleep(5)
                }
            }
            return loader
        }
    }
}