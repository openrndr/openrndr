package org.openrndr.internal.url

import java.io.IOException
import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandler

class ClassPathHandler : URLStreamHandler {
    /** The classloader to find resources from.  */
    private val classLoader: ClassLoader

    constructor() {
        this.classLoader = javaClass.classLoader
    }

    constructor(classLoader: ClassLoader) {
        this.classLoader = classLoader
    }

    @Throws(IOException::class)
    override fun openConnection(u: URL): URLConnection {
        val resourceUrl = classLoader.getResource(u.path) ?: throw IOException("resource not found: " + u.path)

        return resourceUrl.openConnection()
    }
}