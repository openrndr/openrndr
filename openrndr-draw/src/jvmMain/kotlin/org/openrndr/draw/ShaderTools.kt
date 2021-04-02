package org.openrndr.draw

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL


fun codeFromStream(stream: InputStream): String {
    BufferedReader(InputStreamReader(stream)).use {
        return it.readText()
    }
}

fun codeFromURL(url: URL): String {
    url.openStream().use {
        return codeFromStream(it)
    }
}

fun codeFromURL(url: String): String {
    return codeFromURL(URL(url))
}


