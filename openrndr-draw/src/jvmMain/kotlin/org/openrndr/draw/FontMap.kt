@file:JvmName("FontMapJVM")

package org.openrndr.draw

import io.github.oshai.kotlinlogging.KotlinLogging
import org.openrndr.draw.font.Face
import org.openrndr.draw.font.fontHeightScaler
import org.openrndr.draw.font.loadFace
import java.io.File
import java.net.MalformedURLException
import java.net.URL

private val logger = KotlinLogging.logger {}

fun loadFont(
    fileOrUrl: String,
    size: Double,
    characterSet: Set<Char> = defaultFontmapCharacterSet,
    contentScale: Double = 1.0,
    fontScaler: (Face) -> Double = ::fontHeightScaler
): FontImageMap {
    val activeSet = if (characterSet.contains(' ')) characterSet else (characterSet + ' ')
    val font = loadFace(fileOrUrl)
    val scale = fontScaler(font) * size
    font.close()

    return try {
        URL(fileOrUrl)
        FontImageMap.fromUrl(fileOrUrl, scale, activeSet, contentScale)
    } catch (e: MalformedURLException) {
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

actual val defaultFontMap by lazy {
    val defaultFontPath = File("data/fonts/default.otf")
    if (defaultFontPath.isFile) {
        logger.info { "loading default font from ${defaultFontPath.absolutePath}" }
        loadFont(defaultFontPath.path, 16.0, contentScale = RenderTarget.active.contentScale)
    } else {
        logger.warn { "default font ${defaultFontPath.absolutePath} not found" }
        null
    }
}
