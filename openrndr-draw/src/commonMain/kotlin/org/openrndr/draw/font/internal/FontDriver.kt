package org.openrndr.draw.font.internal

import org.openrndr.draw.font.Face

/**
 * FontDriver interface
 */
interface FontDriver {

    /**
     * Load a face at [DEFAULT_SIZE_IN_POINTS], use [Face.withSize] to obtain the face at other sizes
     */
    fun loadFace(fileOrUrl: String): Face

    companion object {
        const val DEFAULT_SIZE_IN_POINTS = 16.0

        var driver: FontDriver? = null
        val instance: FontDriver
            get() {
                return driver ?: error("FontDriver not initialized")
            }
    }
}