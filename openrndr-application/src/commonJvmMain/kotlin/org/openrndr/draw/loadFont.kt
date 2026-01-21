package org.openrndr.draw

import org.openrndr.Program
import org.openrndr.draw.font.Face
import org.openrndr.draw.font.fontHeightScaler

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
    return loadFontImageMap(fileOrUrl, size, characterSet, contentScale, fontScaler)
}
