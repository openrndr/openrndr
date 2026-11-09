package org.openrndr.draw.boundary

import org.openrndr.draw.font.Face

/**
 * Caches the boundaries of glyphs in a [BoundaryMap], each glyph of a face is added to [map] once.
 *
 * @property map the map the boundaries of glyphs are added to
 * @property glyphs the boundary indices of glyphs, by a hash of their face and glyph index
 */
open class BoundaryGlyphMap(val map: BoundaryMap, val glyphs: MutableMap<Int, Int> = mutableMapOf()) {

    /**
     * Starts a batch in [map], glyphs added until [endBatch] is called are uploaded to the GPU at once
     */
    fun startBatch() {
        map.startBatch()
    }

    /**
     * Ends the batch started by [startBatch] and uploads the added glyphs
     */
    fun endBatch() {
        map.endBatch()
    }

    /**
     * Increased by [clear], boundary indices returned in an earlier generation are no longer valid
     */
    var generation = 0
        private set

    /**
     * The number of glyphs in this map
     */
    val glyphCount: Int get() = glyphs.size

    /**
     * Removes all glyphs and clears [map], the textures are kept. Increases [generation], boundary indices returned
     * before are no longer valid and draw commands prepared with them must be prepared again.
     */
    fun clear() {
        map.clear()
        glyphs.clear()
        generation++
    }

    private fun hash(face: Face, index: Int): Int {
        return face.hashCode() * 31 + index.hashCode()
    }

    /**
     * Returns the boundary index of the glyph at [index] in [face], adding the glyph to [map] when it is not in it yet
     */
    fun getBoundaryForGlyphIndex(face: Face, index: Int): Int {
        return glyphs.getOrPut(hash(face, index)) {
            val glyph = face.glyphForIndex(index)
            map.addShape(glyph.shape())
        }
    }

    /**
     * Returns the boundary index of the glyph for [char] in [face]
     */
    fun getGlyph(face: Face, char: Char): Int = getBoundaryForGlyphIndex(face, face.glyphForCharacter(char).index)
}
