package org.openrndr.draw

import org.openrndr.Program
import org.openrndr.draw.font.Face
import org.openrndr.draw.font.fontHeightScaler
import org.openrndr.draw.font.loadFace
import java.io.File
import java.net.MalformedURLException
import java.net.URL
import java.util.*

/**
 * Load a [FontImageMap]
 * @param fileOrUrl a string containing a path to a font file or a url
 * @param size the size at which to rasterize the glyphs in the font
 * @param characterSet the characters to use when rasterizing the glyphs
 * @param contentScale the content scale at which to rasterize the glyphs
 * @param fontScaler a function that determines the scale of rasterization based on [size]
 */
fun Program.loadFont(
    fileOrUrl: String,
    size: Double,
    characterSet: Set<Char> = defaultFontmapCharacterSet,
    contentScale: Double = this.drawer.context.contentScale,
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
        require(file.extension.lowercase(Locale.getDefault()) in setOf("ttf", "otf")) {
            "failed to load font: file '${file.absolutePath}' is not a .ttf or .otf file"
        }
        FontImageMap.fromFile(fileOrUrl, scale, activeSet, contentScale)
    }
}
