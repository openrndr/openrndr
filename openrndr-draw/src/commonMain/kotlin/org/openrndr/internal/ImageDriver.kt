@file:JvmName("ImageDriverJVM")

package org.openrndr.internal

import org.openrndr.draw.ImageFileDetails
import org.openrndr.draw.ImageFileFormat
import org.openrndr.utils.buffer.MPPBuffer
import kotlin.jvm.JvmName

class ImageSaveContext

/**
 * Represents a configuration interface for saving images.
 * Classes implementing this interface define specific strategies or settings
 * for saving image files, which may include parameters like file format,
 * compression level, resolution, or output path.
 */
sealed interface ImageSaveConfiguration

fun ImageSaveContext.jpeg(configuration: JpegImageSaveConfiguration.() -> Unit): JpegImageSaveConfiguration =
    JpegImageSaveConfiguration().apply(configuration)

class JpegImageSaveConfiguration(var quality: Int = 95) : ImageSaveConfiguration

fun ImageSaveContext.exr(configuration: ExrImageSaveConfiguration.() -> Unit): ExrImageSaveConfiguration =
    ExrImageSaveConfiguration().apply(configuration)

/**
 * Configuration class for saving images in the EXR format.
 *
 * This class provides settings specific to the EXR (OpenEXR) file format,
 * allowing customization of how EXR images are saved. It supports defining
 * a compression level to control the quality and size of the saved image.
 *
 * @property compression An integer representing the compression level to be applied when saving the EXR image.
 *                        The valid range and specific levels may depend on the EXR implementation.
 */
class ExrImageSaveConfiguration(var compression: Int = 0) : ImageSaveConfiguration

/**
 * Configuration settings for saving images in the PNG format.
 */
class PngImageSaveConfiguration() : ImageSaveConfiguration

/**
 * Configuration settings for saving images in the HDR format.
 */
class HdrImageSaveConfiguration() : ImageSaveConfiguration

/**
 * Configuration settings for saving images in the DDS format.
 */
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

    /**
     * Saves the provided image data to the specified filename using the given configuration.
     *
     * @param imageData The image data to be saved.
     * @param filename The name of the output file where the image will be saved.
     * @param configuration The configuration specifying the settings for saving the image.
     */
    fun saveImage(imageData: ImageData, filename: String, configuration: ImageSaveConfiguration)

    /**
     * Saves the provided image data to the specified filename using a custom save configuration.
     *
     * @param imageData The image data to be saved.
     * @param filename The name of the output file where the image will be saved.
     * @param saveContext A lambda defining the custom save configuration within the context of [ImageSaveContext].
     */
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

    /**
     * Loads a cubemap image from a file or URL. A cubemap is a texture typically used in 3D rendering,
     * consisting of six images representing the sides of a cube.
     *
     * @param fileOrUrl A string specifying the file location or URL of the cubemap image.
     * @param formatHint An optional hint for the image file format. This can help the loader
     *                   interpret the data correctly if the format cannot be inferred automatically.
     * @return A [CubemapImageData] instance representing the loaded cubemap image. The caller is
     *         responsible for managing the lifecycle of the returned object and closing it when no longer needed.
     */
    fun loadCubemapImage(fileOrUrl: String, formatHint: ImageFileFormat?): CubemapImageData

    /**
     * Loads a cubemap image from the provided buffer. The cubemap is typically a texture with six
     * faces corresponding to the sides of a cube, used in 3D rendering.
     *
     * @param buffer an [MPPBuffer] containing the image data for the cubemap.
     * @param name an optional name for the image, typically used for error reporting and debugging.
     * @param formatHint an optional hint for the file format of the image data in the buffer.
     * @return a [CubemapImageData] instance representing the loaded cubemap image. The caller is responsible
     * for managing the lifecycle of the returned object and closing it when no longer needed.
     */
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

