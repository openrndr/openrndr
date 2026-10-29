package org.openrndr.draw

import org.openrndr.Program

/**
 * Load a [FontImageMap]
 * @param fileOrUrl a url of a font file, loaded with a synchronous request, faces loaded before with
 * [org.openrndr.draw.font.loadFaceSuspend] are not requested again
 * @param sizeInPoints the size at which to rasterize the glyphs in the font
 * @param characterSet the characters to use when rasterizing the glyphs
 * @param contentScale the content scale at which to rasterize the glyphs
 */
fun Program.loadFont(
    fileOrUrl: String,
    sizeInPoints: Double,
    characterSet: Set<Char> = defaultFontmapCharacterSet,
    contentScale: Double = this.drawer.context.contentScale,
): FontImageMap {
    return FontImageMap.fromUrl(fileOrUrl, sizeInPoints, characterSet, contentScale)
}
