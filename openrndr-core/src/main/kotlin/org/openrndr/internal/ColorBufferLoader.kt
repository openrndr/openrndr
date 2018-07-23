package org.openrndr.internal

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.ColorBufferProxy

val colorBufferLoader by lazy {
    ColorBufferLoader.create()
}

class ColorBufferLoader {
    val loadQueue = mutableListOf<ColorBufferProxy>()
    val unloadQueue = mutableListOf<ColorBufferProxy>()


    fun queue(colorBufferProxy: ColorBufferProxy) {
        synchronized(loadQueue) { loadQueue.add(colorBufferProxy) }
    }

    fun loadFromUrl(url: String): ColorBufferProxy {
        val proxy = ColorBufferProxy(url, this).apply {
            lastTouched = System.currentTimeMillis()
            state = ColorBufferProxy.State.QUEUED
        }
        synchronized(loadQueue) {
            loadQueue.add(proxy)
        }
        return proxy
    }

    companion object {
        fun create(): ColorBufferLoader {
            val loader = ColorBufferLoader()
            val rf = Driver.instance.createResourceThread {
                while (true) {

                    if (!loader.loadQueue.isEmpty()) {
                        val proxy = synchronized(loader.loadQueue) {
                            loader.loadQueue.sortBy { it.lastTouched }
                            loader.loadQueue.removeAt(0)
                        }
                        val cb = ColorBuffer.fromUrl(proxy.url)
                        proxy.realColorBuffer = cb
                        proxy.state = ColorBufferProxy.State.LOADED
                        if (!proxy.persistent) {
                            synchronized(loader.unloadQueue) {
                                loader.unloadQueue.add(proxy)
                            }
                        }
                        proxy.events.loaded.trigger(ColorBufferProxy.ProxyEvent())
                    }


                    synchronized(loader.unloadQueue) {
                        if (!loader.unloadQueue.isEmpty()) {
                            loader.unloadQueue.sortBy { it.lastTouched }
                            val dt = System.currentTimeMillis() - loader.unloadQueue[0].lastTouched
                            if (dt > 5000) {
                                val proxy = loader.unloadQueue.removeAt(0)
                                proxy.realColorBuffer?.destroy()
                                proxy.realColorBuffer = null
                                proxy.state = ColorBufferProxy.State.NOT_LOADED
                                proxy.events.unloaded.trigger(ColorBufferProxy.ProxyEvent())
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