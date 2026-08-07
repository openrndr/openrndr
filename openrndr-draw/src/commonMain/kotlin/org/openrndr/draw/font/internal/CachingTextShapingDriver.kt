package org.openrndr.draw.font.internal

import org.openrndr.draw.font.Face
import org.openrndr.utils.collections.LRUCache

/**
 * A caching implementation of [TextShapingDriver] that wraps another driver to optimize text shaping performance.
 *
 * `CachingTextShapingDriver` maintains an in-memory LRU cache to store the results of text shaping operations.
 * This minimizes redundant computations by reusing previously shaped results for identical inputs,
 * thereby improving efficiency in scenarios where the same text is shaped multiple times.
 *
 * @property driver The underlying [TextShapingDriver] used to perform text shaping operations when no cache hit occurs.
 */
class CachingTextShapingDriver(val driver: TextShapingDriver) : TextShapingDriver {

    data class CacheKey(val faceHash: Long, val text: String, val features: List<ShapeFeature>, val direction: Direction?, val script: Script?, val language: String?)

    val lru = LRUCache<CacheKey, List<ShapeResult>>()
    override fun shape(
        face: Face,
        text: String,
        features: List<ShapeFeature>,
        direction: Direction?,
        script: Script?,
        language: String?
    ): List<ShapeResult> {

        val key = CacheKey(face.hashCode().toLong(), text, features, direction, script, language)
        return lru.getOrSet(key, false) {
            driver.shape(face, text, features, direction, script, language)
        }
    }

    override fun querySubstitutionFeatures(
        face: Face,
        script: Script
    ): List<String> {
        return driver.querySubstitutionFeatures(face, script)
    }

    override fun queryPositionFeatures(
        face: Face,
        script: Script
    ): List<String> {
        return driver.queryPositionFeatures(face, script)
    }
}