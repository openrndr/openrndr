@file:JvmName("ImageDriverJVM")

package org.openrndr.internal

import org.openrndr.draw.ImageFileDetails
import kotlin.jvm.JvmName


/**
 * ImageDriver is responsible for probing images
 * @since 0.4.3
 */
interface ImageDriver {
    /**
     * Probe an image located at [fileOrUrl]
     * @param fileOrUrl a string encoded file location or url
     *
     * @since 0.4.3
     */
    fun probeImage(fileOrUrl: String): ImageFileDetails?

    companion object {
        var driver: ImageDriver? = null
        val instance: ImageDriver
            get() {
                return driver ?: error("ImageDriver not initialized")
            }
    }
}

