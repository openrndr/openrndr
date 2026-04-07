package org.openrndr.draw

import org.openrndr.Program

/**
 * Load a [FontImageMap]
 * @param fileOrUrl a string containing a path to a font file or a url
 * @param sizeInPoints the size at which to rasterize the glyphs in the font
 * @param characterSet the characters to use when rasterizing the glyphs
 * @param contentScale the content scale at which to rasterize the glyphs
 * @param fontScaler a function that determines the scale of rasterization based on [sizeInPoints]
 */
fun Program.loadFont(
    fileOrUrl: String,
    sizeInPoints: Double,
    characterSet: Set<Char> = defaultFontmapCharacterSet,
    contentScale: Double = this.drawer.context.contentScale,
): FontImageMap {
    return loadFontImageMap(fileOrUrl, sizeInPoints, characterSet, contentScale)
}
