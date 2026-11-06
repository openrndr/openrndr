package org.openrndr.draw.font.internal

import org.openrndr.draw.font.Face

/**
 * A caching implementation of [FontDriver] that wraps another driver to avoid loading the same face repeatedly.
 *
 * `CachingFontDriver` keeps one base face per `fileOrUrl`, `data:` urls are kept by their SHA-256 digest, so the
 * cache does not hold the font data in them. Every call to [loadFace] returns a new face obtained
 * through [Face.withSize] on the base face, which shares the font data with it. Each returned face is owned by
 * the caller and can be closed independently; the shared font data is freed once the base face and all faces
 * derived from it are closed.
 *
 * @property driver The underlying [FontDriver] used to load faces that are not in the cache.
 */
class CachingFontDriver(val driver: FontDriver) : FontDriver {

    private val faces = mutableMapOf<String, Face>()

    override fun loadFace(fileOrUrl: String): Face {
        val base = faces.getOrPut(cacheKey(fileOrUrl)) { driver.loadFace(fileOrUrl) }
        return base.withSize(FontDriver.DEFAULT_SIZE_IN_POINTS)
    }

    override suspend fun loadFaceSuspend(fileOrUrl: String): Face {
        val key = cacheKey(fileOrUrl)
        val base = faces[key] ?: driver.loadFaceSuspend(fileOrUrl).also { faces[key] = it }
        return base.withSize(FontDriver.DEFAULT_SIZE_IN_POINTS)
    }

    /**
     * Closes and removes all cached base faces. Faces previously returned by [loadFace] remain valid.
     */
    /** the number of cached base faces */
    val size: Int get() = faces.size

    fun clear() {
        faces.values.forEach { it.close() }
        faces.clear()
    }
}

/**
 * The key of the face of [fileOrUrl] in the cache: `data:` urls by the SHA-256 digest of their UTF-8 bytes, other
 * files and urls as they are
 */
internal fun cacheKey(fileOrUrl: String): String =
    if (fileOrUrl.startsWith("data:", ignoreCase = true)) "data:sha-256:" + sha256Hex(fileOrUrl.encodeToByteArray())
    else fileOrUrl
