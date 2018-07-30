package org.openrndr.internal

import mu.KotlinLogging
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.ColorBufferProxy
import java.io.IOException

private val logger = KotlinLogging.logger {}

val colorBufferLoader by lazy {
    ColorBufferLoader.create()
}

class ColorBufferLoader {
    private val loadQueue = mutableListOf<ColorBufferProxy>()
    private val unloadQueue = mutableListOf<ColorBufferProxy>()

    private val urlItems = mutableMapOf<String, ColorBufferProxy>()

    fun queue(colorBufferProxy: ColorBufferProxy) {
        synchronized(loadQueue) { loadQueue.add(colorBufferProxy) }
    }

    fun loadFromUrl(url: String, persistent: Boolean = false): ColorBufferProxy {
        val proxy = urlItems.getOrPut(url) {
            ColorBufferProxy(url, this, persistent).apply {
                lastTouched = System.currentTimeMillis()
                realState = ColorBufferProxy.State.QUEUED
            }
        }

        synchronized(loadQueue) {
            loadQueue.add(proxy)
        }
        return proxy
    }

    companion object {
        fun create(): ColorBufferLoader {
            val loader = ColorBufferLoader()
            Driver.instance.createResourceThread {
                logger.debug { "ColorBufferLoader thread started" }
                while (true) {
                    if (!loader.loadQueue.isEmpty()) {
                        val proxy = synchronized(loader.loadQueue) {
                            loader.loadQueue.forEach { it.lastTouchedShadow = it.lastTouched }
                            loader.loadQueue.sortBy { it.lastTouchedShadow }
                            loader.loadQueue.removeAt(0)
                        }
                        try {
                            val cb = ColorBuffer.fromUrl(proxy.url)
                            proxy.realColorBuffer = cb
                            proxy.realState = ColorBufferProxy.State.LOADED
                            if (!proxy.persistent) {
                                synchronized(loader.unloadQueue) {
                                    loader.unloadQueue.add(proxy)
                                }
                            }
                            logger.debug { "successfully loaded $proxy" }
                            proxy.events.loaded.trigger(ColorBufferProxy.ProxyEvent())
                        } catch (e: IOException) {
                            logger.error { e.stackTrace }
                            proxy.realState = ColorBufferProxy.State.RETRY
                            proxy.events.retry.trigger(ColorBufferProxy.ProxyEvent())
                        } catch (e: Exception) {
                            logger.error { "unexpected exception while loading $proxy" }
                            logger.error { e.stackTrace }
                            proxy.realState = ColorBufferProxy.State.ERROR
                            proxy.events.error.trigger(ColorBufferProxy.ProxyEvent())
                            // maybe come to a grinding halt here?
                        }
                    }

                    synchronized(loader.unloadQueue) {
                        if (!loader.unloadQueue.isEmpty()) {
                            //loader.unloadQueue.sortBy { it.lastTouched }
                            val dt = System.currentTimeMillis() - loader.unloadQueue[0].lastTouched
                            if (dt > 5000) {
                                val proxy = loader.unloadQueue.removeAt(0)
                                proxy.realColorBuffer?.destroy()
                                proxy.realColorBuffer = null
                                proxy.realState = ColorBufferProxy.State.NOT_LOADED
                                proxy.events.unloaded.trigger(ColorBufferProxy.ProxyEvent())
                                logger.debug { "unloaded $proxy" }
                            }
                        }
                    }

                    Driver.instance.clear(ColorRGBa.BLACK)
                    Thread.sleep(5)
                }
            }
            return loader
        }
    }
}