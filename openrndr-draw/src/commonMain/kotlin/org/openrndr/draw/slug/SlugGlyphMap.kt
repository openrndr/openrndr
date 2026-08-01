package org.openrndr.draw.slug

import org.openrndr.draw.font.Face

/**
 * A class responsible for managing a mapping between slugs and glyphs for font rendering tasks.
 *
 * This class utilizes a provided `SlugMap` to store and manage slugs, while maintaining an internal
 * cache of glyph-to-slug mappings for quick retrieval and computation.
 *
 * @property slugMap the `SlugMap` instance used to manage and store slugs.
 * @property glyphs a mutable map associating hashed glyph identifiers to their corresponding slugs.
 */
class SlugGlyphMap(val slugMap: SlugMap, val glyphs: MutableMap<Int, Int> = mutableMapOf()) {

    /**
     * Begins a new batching operation for the `SlugMap`.
     *
     * This method prepares the `SlugMap` for batch processing, where the accumulation
     * of shape data occurs in memory rather than being immediately processed and
     * written to GPU textures. Batching helps optimize performance by consolidating
     * multiple write operations into fewer, larger ones. This operation is especially
     * beneficial when processing many glyphs or shapes in sequence at runtime.
     *
     * The batching operation must be ended explicitly using the [endBatch] method
     * to ensure all accumulated data is properly finalized and written.
     *
     * Delegates the batching operation to the `SlugMap` instance.
     *
     * @throws IllegalStateException if the `SlugMap` is already in batching mode.
     * @see endBatch
     */
    fun startBatch() {
        slugMap.startBatch()
    }

    /**
     * Ends the current batching operation for the `SlugMap`.
     *
     * This method finalizes any shape data accumulated during the batching process
     * and ensures that all buffered data is processed and written to the necessary
     * resources, such as GPU textures. It is important to explicitly call this
     * method after starting a batching operation with [startBatch] to complete
     * the batching lifecycle properly.
     *
     * Delegates the batching termination to the `SlugMap` instance.
     *
     * @throws IllegalStateException if there is no active batching operation to end.
     * @see startBatch
     */
    fun endBatch() {
        slugMap.endBatch()
    }

    private fun hash(face: Face, index: Int): Int {
        return face.hashCode() * 31 + index.hashCode()
    }

    /**
     * Retrieves or computes the slug for a glyph at the specified index in the given face.
     *
     * This method checks if a slug for the given glyph index in the specified face already exists
     * in the internal slug map. If not, it computes the slug by retrieving and processing the glyph
     * and then stores it in the cache.
     *
     * @param face the font face from which to retrieve the glyph.
     * @param index the index of the glyph within the specified face.
     * @return the slug value corresponding to the glyph at the provided index in the given face.
     */
    fun getSlugForGlyphIndex(face: Face, index: Int): Int {
        return glyphs.getOrPut(hash(face, index)) {
            val glyph = face.glyphForIndex(index)
            slugMap.addShape(glyph.shape())
        }
    }

    fun getGlyph(face: Face, char: Char): Int = getSlugForGlyphIndex(face, face.glyphForCharacter(char).index)
}
