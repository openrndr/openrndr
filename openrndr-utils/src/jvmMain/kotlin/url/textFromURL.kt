package org.openrndr.utils.url

import java.io.File
import java.net.MalformedURLException
import java.net.URL


fun resolveFileOrUrl(fileOrUrl: String): Pair<File?, URL?> {
    return try {
        Pair(null, URL(fileOrUrl))
    } catch (e: MalformedURLException) {
        Pair(File(fileOrUrl), null)
    }

}

actual fun textFromURL(url: String): String {
    TODO("Not yet implemented")
}