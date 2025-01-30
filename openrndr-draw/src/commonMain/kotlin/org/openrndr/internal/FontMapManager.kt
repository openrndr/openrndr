package org.openrndr.internal

import org.openrndr.draw.FontImageMap

/**
 * Abstract class responsible for managing the creation of font image maps from various sources.
 */
abstract class FontMapManager {
    /**
     * Generates a `FontImageMap` from a font located at the specified URL, with the given font size,
     * character set, and optional content scaling factor.
     *
     * @param url The URL of the font file to be used for rendering.
     * @param size The font size to be used in rendering the characters in the font image map.
     * @param characterSet The set of characters to include in the generated font image map.
     * @param contentScale The scaling factor for rendering the font. Defaults to `1.0`.
     * @return A `FontImageMap` containing the pre-rendered characters, their metrics, and texture data.
     */
    abstract fun fontMapFromUrl(url:String, size:Double, characterSet: Set<Char>, contentScale:Double=1.0): FontImageMap
}