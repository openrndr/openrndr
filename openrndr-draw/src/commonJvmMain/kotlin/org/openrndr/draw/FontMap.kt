@file:JvmName("FontMapJVM")

package org.openrndr.draw

import io.github.oshai.kotlinlogging.KotlinLogging
import org.openrndr.draw.font.Face
import org.openrndr.draw.font.fontHeightScaler
import org.openrndr.draw.font.loadFace
import java.io.File
import java.net.MalformedURLException
import java.net.URI

private val logger = KotlinLogging.logger {}

private val scaleCache = mutableMapOf<Pair<String, (Face) -> Double>, Double>()

/**
 * Loads a font image map from a given file or URL. This function creates a `FontImageMap`
 * instance from the specified font resource, applying the given properties such as size,
 * character set, content scale, and a font scaling method.
 *
 * @param fileOrUrl The path to a local font file or a URL pointing to a font resource.
 * @param size The desired size of the font to be loaded.
 * @param characterSet A set of characters to include in the font image map. Defaults to `defaultFontmapCharacterSet`.
 *                     If a space character is not explicitly included, it will be added automatically.
 * @param contentScale A scaling factor for adjusting the pixel density of the font image map. Defaults to `1.0`.
 * @param fontScaler A function defining how the font is scaled within the image map. Defaults to `::fontHeightScaler`.
 * @return A `FontImageMap` that represents the pre-rendered character textures for the specified font.
 *         Throws an exception if the file does not exist, is not a `.ttf` or `.otf` file, or if the URL is invalid.
 */
fun loadFontImageMap(
    fileOrUrl: String,
    size: Double,
    characterSet: Set<Char> = defaultFontmapCharacterSet,
    contentScale: Double = 1.0,
    fontScaler: (Face) -> Double = ::fontHeightScaler
): FontImageMap {
    val activeSet = if (characterSet.contains(' ')) characterSet else (characterSet + ' ')

    val scale = scaleCache.getOrPut(fileOrUrl to fontScaler) { loadFace(fileOrUrl).use { fontScaler(it) } } * size

    return if (isValidUrl(fileOrUrl)) {
        FontImageMap.fromUrl(fileOrUrl, scale, activeSet, contentScale)
    } else {
        val file = File(fileOrUrl)
        require(file.exists()) {
            "failed to load font: file '${file.absolutePath}' does not exist."
        }
        require(file.extension.lowercase() in setOf("ttf", "otf")) {
            "failed to load font: file '${file.absolutePath}' is not a .ttf or .otf file"
        }
        FontImageMap.fromFile(fileOrUrl, scale, activeSet, contentScale)
    }
}

private fun isValidUrl(url: String): Boolean {
    return try {
        URI(url).toURL()
        true
    } catch (_: MalformedURLException) {
        false
    } catch(_: IllegalArgumentException) {
        false
    }
}


actual val defaultFontMap by lazy {
    val defaultFontPath = File("data/fonts/default.otf")
    if (defaultFontPath.isFile) {
        logger.info { "loading default font from ${defaultFontPath.absolutePath}" }
        loadFontImageMap(defaultFontPath.path, 16.0, contentScale = RenderTarget.active.contentScale)
    } else {
        logger.warn { "default font ${defaultFontPath.absolutePath} not found" }
        null
    }
}
