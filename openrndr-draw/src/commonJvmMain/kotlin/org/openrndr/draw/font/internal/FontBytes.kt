package org.openrndr.draw.font.internal

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.net.URI
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.Base64
import org.openrndr.draw.font.webfont.sfntFontBytes

private val logger = KotlinLogging.logger { }

/**
 * Directory in which fonts downloaded over http(s) are cached.
 *
 * Can be overridden with the `org.openrndr.fontcache.dir` system property. Defaults to the platform cache directory:
 * `~/Library/Caches/org.openrndr/fonts` on macOS, `%LOCALAPPDATA%\openrndr\cache\fonts` on Windows and
 * `$XDG_CACHE_HOME/openrndr/fonts` (or `~/.cache/openrndr/fonts`) elsewhere.
 *
 * Cached fonts do not expire; delete the directory to force fonts to be downloaded again.
 */
val fontCacheDirectory: File by lazy {
    System.getProperty("org.openrndr.fontcache.dir")?.let { return@lazy File(it) }
    val home = System.getProperty("user.home")
    val os = System.getProperty("os.name").lowercase()
    when {
        os.contains("mac") -> File(home, "Library/Caches/org.openrndr/fonts")
        os.contains("win") -> File(System.getenv("LOCALAPPDATA") ?: "$home\\AppData\\Local", "openrndr\\cache\\fonts")
        else -> File(System.getenv("XDG_CACHE_HOME") ?: "$home/.cache", "openrndr/fonts")
    }
}

/**
 * Read font bytes from a file or url.
 *
 * Fonts loaded from http(s) urls are cached on disk in [cacheDirectory], so they are only downloaded once.
 * Set the `org.openrndr.fontcache` system property to `false` to disable the disk cache. `data:` urls are decoded,
 * base64 or percent-encoded. WOFF and WOFF2 fonts are decoded to TrueType or OpenType fonts.
 */
fun fontBytesFromFileOrUrl(fileOrUrl: String, cacheDirectory: File = fontCacheDirectory): ByteArray =
    sfntFontBytes(readFontBytes(fileOrUrl, cacheDirectory))

/**
 * The bytes of the `data:` url [url]: `data:[<mediatype>][;base64],<data>`
 */
fun bytesOfDataUrl(url: String): ByteArray {
    require(url.startsWith("data:", ignoreCase = true)) { "not a data url" }
    val comma = url.indexOf(',')
    require(comma >= 0) { "data url without data" }
    val header = url.substring(5, comma)
    val data = url.substring(comma + 1)
    return if (header.endsWith(";base64", ignoreCase = true)) {
        // whitespace and percent-encoded characters can occur in base64 data urls
        Base64.getDecoder().decode(percentDecode(data).toString(Charsets.ISO_8859_1).filterNot { it.isWhitespace() })
    } else {
        percentDecode(data)
    }
}

private fun percentDecode(text: String): ByteArray {
    val out = java.io.ByteArrayOutputStream(text.length)
    var i = 0
    while (i < text.length) {
        val c = text[i]
        if (c == '%' && i + 2 < text.length) {
            val hex = text.substring(i + 1, i + 3).toIntOrNull(16)
            if (hex != null) {
                out.write(hex)
                i += 3
                continue
            }
        }
        if (c.code < 0x80) out.write(c.code) else out.write(c.toString().toByteArray(Charsets.UTF_8))
        i++
    }
    return out.toByteArray()
}

private fun readFontBytes(fileOrUrl: String, cacheDirectory: File): ByteArray {
    if (fileOrUrl.startsWith("data:", ignoreCase = true)) {
        return bytesOfDataUrl(fileOrUrl)
    }
    val isHttp = fileOrUrl.startsWith("http://") || fileOrUrl.startsWith("https://")
    if (!isHttp) {
        return if (fileOrUrl.startsWith("file:") || fileOrUrl.startsWith("jar:")) {
            @Suppress("DEPRECATION")
            URL(fileOrUrl).readBytes()
        } else {
            File(fileOrUrl).readBytes()
        }
    }

    val url = URI(fileOrUrl).toURL()
    if (System.getProperty("org.openrndr.fontcache") == "false") {
        return url.readBytes()
    }

    val digest = MessageDigest.getInstance("SHA-256").digest(fileOrUrl.toByteArray())
    val name = digest.joinToString("") { "%02x".format(it) }
    val extension = url.path.substringAfterLast('/').substringAfterLast('.', "").take(8)
    val cached = File(cacheDirectory, if (extension.isEmpty()) name else "$name.$extension")

    if (cached.isFile) {
        try {
            return cached.readBytes()
        } catch (e: Exception) {
            logger.warn(e) { "failed to read cached font '${cached}', downloading again" }
        }
    }

    val bytes = url.readBytes()
    var temp: File? = null
    try {
        cacheDirectory.mkdirs()
        temp = File.createTempFile(name, ".tmp", cacheDirectory)
        temp.writeBytes(bytes)
        Files.move(temp.toPath(), cached.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        logger.info { "cached font '$fileOrUrl' in '$cached'" }
    } catch (e: Exception) {
        temp?.delete()
        logger.warn(e) { "failed to cache font '$fileOrUrl' in '$cacheDirectory'" }
    }
    return bytes
}
