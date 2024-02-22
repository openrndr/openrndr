@file:JvmName("ImageDriverJVM")

package org.openrndr.internal

import org.openrndr.draw.ImageFileDetails
import org.openrndr.draw.ImageFileFormat
import org.openrndr.utils.buffer.MPPBuffer
import kotlin.jvm.JvmName


/**
 * ImageDriver is responsible for probing, loading and saving images
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

    /**
     * Probe an image from [buffer]
     * @since 0.4.5
     */
    fun probeImage(buffer: MPPBuffer, formatHint: ImageFileFormat?): ImageFileDetails?


    /**
     * Load an image located at [fileOrUrl]
     * @param fileOrUrl a string encoded file location or url
     * @param formatHint a hint for the file format
     * @return an [ImageData] instance that the user must close after use
     * @since 0.4.5
     */
    fun loadImage(fileOrUrl: String, formatHint: ImageFileFormat?): ImageData

    /**
     * Load an image located at [fileOrUrl]
     * @param buffer an [MPPBuffer] holding the image data
     * @param name a name that is used in error reporting only
     * @param formatHint a hint for the file format used in [buffer]
     * @return an [ImageData] instance that the user must close after use
     * @since 0.4.5
     */
    fun loadImage(buffer: MPPBuffer, name: String?, formatHint: ImageFileFormat?): ImageData

    /**
     * Save an image to [filename]
     * @param imageData an [ImageData] instance holding the pixel data to be saved
     * @param filename the filename to save the image to
     * @param formatHint the format to use
     * @since 0.4.5
     */
    fun saveImage(imageData: ImageData, filename: String, formatHint: ImageFileFormat?)

    /**
     * Convert an image to a data-url
     * @param imageData an [ImageData] instance holding the pixel data to be converted
     * @param formatHint the format to use in the generated data-url
     * @since 0.4.5
     */
    fun imageToDataUrl(imageData: ImageData, formatHint: ImageFileFormat?): String

    fun loadCubemapImage(fileOrUrl: String, formatHint: ImageFileFormat?): CubemapImageData

    fun loadCubemapImage(buffer: MPPBuffer, name: String?, formatHint: ImageFileFormat?): CubemapImageData


    companion object {
        var driver: ImageDriver? = null

        /**
         * The instance singleton
         */
        val instance: ImageDriver
            get() {
                return driver ?: error("ImageDriver not initialized")
            }
    }
}

