package org.openrndr.draw.font.internal

import org.openrndr.draw.font.Face

/**
 * FontDriver interface
 */
interface FontDriver {

    fun loadFace(fileOrUrl: String): Face

    companion object {
        var driver: FontDriver? = null
        val instance: FontDriver
            get() {
                return driver ?: error("FontDriver not initialized")
            }
    }
}