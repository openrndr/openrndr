@file:JvmName("ImageDriverJVM")

package org.openrndr.internal

import org.openrndr.draw.ImageFileDetails
import org.openrndr.draw.ImageFileFormat
import org.openrndr.utils.buffer.MPPBuffer
import kotlin.jvm.JvmName

class ImageSaveContext

sealed interface ImageSaveConfiguration

fun ImageSaveContext.jpeg(configuration: JpegImageSaveConfiguration.() -> Unit): JpegImageSaveConfiguration =
    JpegImageSaveConfiguration().apply(configuration)

class JpegImageSaveConfiguration(var quality: Int = 95) : ImageSaveConfiguration

class ExrImageSaveConfiguration() : ImageSaveConfiguration

class PngImageSaveConfiguration() : ImageSaveConfiguration

class HdrImageSaveConfiguration() : ImageSaveConfiguration

class DdsImageSaveConfiguration() : ImageSaveConfiguration

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
    fun loadImage(fileOrUrl: String, formatHint: ImageFileFormat?, allowSRGB: Boolean): ImageData

    /**
     * Load an image located at [fileOrUrl]
     * @param buffer an [MPPBuffer] holding the image data
     * @param name a name that is used in error reporting only
     * @param formatHint a hint for the file format used in [buffer]
     * @return an [ImageData] instance that the user must close after use
     * @since 0.4.5
     */
    fun loadImage(buffer: MPPBuffer, name: String?, formatHint: ImageFileFormat?, allowSRGB: Boolean): ImageData

    /**
     * Save an image to [filename]
     * @param imageData an [ImageData] instance holding the pixel data to be saved
     * @param filename the filename to save the image to
     * @param formatHint the format to use
     * @since 0.4.5
     */
    fun saveImage(imageData: ImageData, filename: String, formatHint: ImageFileFormat?) {
        val assumedFormat = formatHint ?: ImageFileFormat.PNG
        val config = when (assumedFormat) {
            ImageFileFormat.JPG -> JpegImageSaveConfiguration()
            ImageFileFormat.EXR -> ExrImageSaveConfiguration()
            ImageFileFormat.PNG -> PngImageSaveConfiguration()
            ImageFileFormat.DDS -> DdsImageSaveConfiguration()
            ImageFileFormat.HDR -> HdrImageSaveConfiguration()
        }
        return saveImage(imageData, filename, config)
    }

    fun saveImage(imageData: ImageData, filename: String, configuration: ImageSaveConfiguration)

    fun saveImage(imageData: ImageData, filename: String, saveContext: ImageSaveContext.() -> ImageSaveConfiguration) {
        val context = ImageSaveContext()
        val config = context.saveContext()
    }

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

