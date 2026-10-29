package org.openrndr.draw.font.internal

import org.openrndr.draw.font.Face

/**
 * A caching implementation of [FontDriver] that wraps another driver to avoid loading the same face repeatedly.
 *
 * `CachingFontDriver` keeps one base face per `fileOrUrl`. Every call to [loadFace] returns a new face obtained
 * through [Face.withSize] on the base face, which shares the font data with it. Each returned face is owned by
 * the caller and can be closed independently; the shared font data is freed once the base face and all faces
 * derived from it are closed.
 *
 * @property driver The underlying [FontDriver] used to load faces that are not in the cache.
 */
class CachingFontDriver(val driver: FontDriver) : FontDriver {

    private val faces = mutableMapOf<String, Face>()

    override fun loadFace(fileOrUrl: String): Face {
        val base = faces.getOrPut(fileOrUrl) { driver.loadFace(fileOrUrl) }
        return base.withSize(FontDriver.DEFAULT_SIZE_IN_POINTS)
    }

    override suspend fun loadFaceSuspend(fileOrUrl: String): Face {
        val base = faces[fileOrUrl] ?: driver.loadFaceSuspend(fileOrUrl).also { faces[fileOrUrl] = it }
        return base.withSize(FontDriver.DEFAULT_SIZE_IN_POINTS)
    }

    /**
     * Closes and removes all cached base faces. Faces previously returned by [loadFace] remain valid.
     */
    fun clear() {
        faces.values.forEach { it.close() }
        faces.clear()
    }
}
