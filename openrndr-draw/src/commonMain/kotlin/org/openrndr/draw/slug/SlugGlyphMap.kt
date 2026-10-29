package org.openrndr.draw.slug

import org.openrndr.draw.boundary.BoundaryGlyphMap
import org.openrndr.draw.font.Face

/**
 * Caches the slugs of glyphs in a [SlugMap], each glyph of a face is added to [slugMap] once.
 *
 * @property slugMap the `SlugMap` instance used to manage and store slugs.
 */
class SlugGlyphMap(val slugMap: SlugMap, glyphs: MutableMap<Int, Int> = mutableMapOf()) :
    BoundaryGlyphMap(slugMap, glyphs) {

    /**
     * Returns the slug of the glyph at [index] in [face], adding the glyph to [slugMap] when it is not in it yet
     */
    fun getSlugForGlyphIndex(face: Face, index: Int): Int = getBoundaryForGlyphIndex(face, index)
}
