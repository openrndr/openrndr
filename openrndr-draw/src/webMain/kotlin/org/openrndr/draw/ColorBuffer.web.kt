package org.openrndr.draw

import io.github.oshai.kotlinlogging.KotlinLogging
import js.buffer.ArrayBufferLike
import js.buffer.ArrayBufferView
import kotlinx.coroutines.CancellationException
import org.openrndr.color.ColorRGBa
import org.openrndr.internal.Driver
import org.openrndr.shape.IntRectangle
import org.openrndr.shape.Rectangle
import org.openrndr.utils.buffer.MPPBuffer
import web.gl.TexImageSource
import web.http.blob
import web.http.fetch
import web.images.*
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.js

private val logger = KotlinLogging.logger {  }

actual abstract class ColorBuffer : Texture, AutoCloseable {
    actual abstract val session: Session?

    /** the width of the [ColorBuffer] in device units */
    actual abstract val width: Int

    /** the height of the [ColorBuffer] in device units */
    actual abstract val height: Int

    /** the content scale of the [ColorBuffer] */
    actual abstract val contentScale: Double

    /**
     * the [ColorFormat] of the image stored in the [ColorBuffer]
     */
    actual abstract val format: ColorFormat

    /**
     * the [ColorType] of the image stored in the [ColorBuffer]
     */
    actual abstract val type: ColorType

    /** the number of mipmap levels */
    actual abstract val levels: Int

    /** the multisampling method used for this [ColorBuffer] */
    actual abstract val multisample: BufferMultisample

    /** the width of the [ColorBuffer] in pixels */
    actual val effectiveWidth: Int
        get() = (width * contentScale).toInt()

    /** the height of the [ColorBuffer] in pixels */
    actual val effectiveHeight: Int
        get() = (height * contentScale).toInt()

    actual val bounds: Rectangle
        get() = Rectangle(0.0, 0.0, width.toDouble(), height.toDouble())

    /** permanently destroy the underlying [ColorBuffer] resources, [ColorBuffer] can not be used after it is destroyed */
    actual abstract fun destroy()

    /** generates mipmaps from the top-level mipmap */
    actual abstract fun generateMipmaps()

    /** the (unitless?) degree of anisotropy to be used in filtering */
    actual abstract var anisotropy: Double

    /**
     * should the v coordinate be flipped because the [ColorBuffer] contents are stored upside-down?
     */
    actual abstract var flipV: Boolean
    actual abstract fun copyTo(
        target: ColorBuffer,
        fromLevel: Int,
        toLevel: Int,
        sourceRectangle: IntRectangle,
        targetRectangle: IntRectangle,
        filter: MagnifyingFilter
    )

    actual abstract fun copyTo(
        target: ColorBuffer,
        fromLevel: Int,
        toLevel: Int,
        filter: MagnifyingFilter
    )

    /**
     * copies contents to a target array texture
     * @param target the color buffer to which contents will be copied
     * @param layer the array layer from which will be copied
     * @param fromLevel the mip-map level from which will be copied
     * @param toLevel the mip-map level of [target] to which will be copied
     */
    actual abstract fun copyTo(
        target: ArrayTexture,
        layer: Int,
        fromLevel: Int,
        toLevel: Int
    )

    abstract fun write(
        source: TexImageSource,
        x: Int = 0,
        y: Int = 0,
        width: Int = this.effectiveWidth,
        height: Int = this.effectiveHeight,
        level: Int = 0
    )

    abstract fun write(
        source: ArrayBufferView<ArrayBufferLike>,
        sourceFormat: ColorFormat,
        sourceType: ColorType,
        x: Int = 0,
        y: Int = 0,
        width: Int = this.effectiveWidth,
        height: Int = this.effectiveHeight,
        level: Int = 0
    )

    abstract fun read(
        target: ArrayBufferView<ArrayBufferLike>,
        x: Int,
        y: Int,
        width: Int = this.effectiveWidth,
        height: Int = this.effectiveHeight,
        level: Int = 0
    )

    actual abstract fun write(
        sourceBuffer: MPPBuffer,
        sourceFormat: ColorFormat,
        sourceType: ColorType,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        level: Int
    )

    actual abstract fun filter(
        filterMin: MinifyingFilter,
        filterMag: MagnifyingFilter
    )

    /** the wrapping mode to use in the horizontal direction */
    actual abstract var wrapU: WrapMode

    /** the wrapping mode to use in the vertical direction */
    actual abstract var wrapV: WrapMode

    /**
     * sets all pixels in the color buffer to [color]
     * @param color the color used for filling
     */
    actual abstract fun fill(color: ColorRGBa, level: Int)

}

@OptIn(ExperimentalWasmJsInterop::class)
fun createImageBitmapOptions(): ImageBitmapOptions = js("({ })")

/**
 * load an image from a url encoded as [String], also accepts base64 encoded data urls
 * Load an image from a file or url encoded as [String], also accepts base64 encoded data urls
 * Usage example:
 * ```
 * program {
 *     val image = loadImage("data/images/cheeta.jpg")
 *     extend {
 *         drawer.image(image)
 *     }
 * }
 * ```
 */
@OptIn(ExperimentalWasmJsInterop::class)
actual suspend fun loadImage(
    url: String,
    formatHint: ImageFileFormat?,
    allowSRGB: Boolean,
    loadMipmaps: Boolean,
    session: Session?
): ColorBuffer {
    val fallback: () -> ColorBuffer = { colorBuffer(1, 1) }

    val imageBitmap: ImageBitmap = try {
        val resp = fetch(url)
        if (!resp.ok) {
            logger.error { "Failed to fetch '$url': HTTP ${resp.status}" }
            return fallback()
        }

        val options = createImageBitmapOptions()
        options.premultiplyAlpha = PremultiplyAlpha.none
        options.imageOrientation = ImageOrientation.flipY

        createImageBitmap(resp.blob(), options)
    } catch (c: CancellationException) {
        throw c
    } catch (e: Exception) {
        logger.error(e) { "Image loading failed for '$url': ${e.message}" }
        return fallback()
    }

    return try {
        val colorType = if (allowSRGB) ColorType.UINT8_SRGB else ColorType.UINT8
        Driver.instance.createColorBufferFromImage(
            imageBitmap,
            1.0,
            ColorFormat.RGBa,
            colorType,
            BufferMultisample.Disabled,
            1,
            session
        )
    } finally {
        runCatching { imageBitmap.close() }
    }
}