package org.openrndr.utils.url

import java.io.File
import java.net.MalformedURLException
import java.net.URL

/**
 * Resolves a given string to either a local file or a URL. If the input cannot be
 * resolved as a URL, it attempts to interpret it as a file path.
 *
 * When the input string starts with "data:", both File and URL components of the returned
 * pair will be null, as data URLs are handled separately and do not correspond to
 * either local files or network URLs.
 *
 * @param fileOrUrl the input string representing either a file path or a URL
 * @return a Pair where the first value is a File object if resolved successfully, or null otherwise,
 *         and the second value is a URL object if resolved successfully, or null otherwise
 */
fun resolveFileOrUrl(fileOrUrl: String): Pair<File?, URL?> {
    return try {
        if (fileOrUrl.startsWith("data:")) {
            Pair(null, null)
        } else {
            Pair(null, URL(fileOrUrl))
        }
    } catch (e: MalformedURLException) {
        Pair(File(fileOrUrl), null)
    }

}

actual fun textFromURL(url: String): String {
    return URL(url).readText()
}