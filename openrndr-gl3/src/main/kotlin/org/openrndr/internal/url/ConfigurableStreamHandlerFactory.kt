package org.openrndr.internal.url

import java.net.URLStreamHandler
import java.net.URLStreamHandlerFactory
import java.util.HashMap

class ConfigurableStreamHandlerFactory(protocol: String, urlHandler: URLStreamHandler) : URLStreamHandlerFactory {
    private val protocolHandlers: MutableMap<String, URLStreamHandler>

    init {
        protocolHandlers = HashMap()
        addHandler(protocol, urlHandler)
    }

    fun addHandler(protocol: String, urlHandler: URLStreamHandler) {
        protocolHandlers.put(protocol, urlHandler)
    }

    override fun createURLStreamHandler(protocol: String): URLStreamHandler {
        protocolHandlers[protocol]?.let {
            return it
        }
            throw RuntimeException("no stream handler for $protocol")


    }
}