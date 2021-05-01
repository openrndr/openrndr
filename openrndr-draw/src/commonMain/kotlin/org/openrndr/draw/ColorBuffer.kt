package org.openrndr.draw

import org.openrndr.internal.Driver
import org.openrndr.shape.IntRectangle
import org.openrndr.shape.Rectangle
import org.openrndr.utils.buffer.MPPBuffer


/**
 * Buffer multisample options
 */
sealed class BufferMultisample {
    /**
     * Disable multisampling
     */
    object Disabled : BufferMultisample() {
        override fun toString(): String {
            return "Disabled()"
        }
    }

    /**
     * Enable multisampling with a given [sampleCount]
     */
    data class SampleCount(val sampleCount: Int) : BufferMultisample()
}


/**
 * Texture wrapping mode
 */
enum class WrapMode {
    CLAMP_TO_EDGE,
    REPEAT,
    MIRRORED_REPEAT
}

/**
 * Texture filters used for minification
 */
enum class MinifyingFilter {
    NEAREST,
    LINEAR,
    NEAREST_MIPMAP_NEAREST,
    LINEAR_MIPMAP_NEAREST,
    NEAREST_MIPMAP_LINEAR,
    LINEAR_MIPMAP_LINEAR
}

/**
 * Texture filters for magnification
 */
enum class MagnifyingFilter {
    /** nearest neighbour, blocky */
    NEAREST,
    LINEAR
}


expect abstract class ColorBuffer {
    abstract val session: Session?

    /** the width of the [ColorBuffer] in device units */
    abstract val width: Int

    /** the height of the [ColorBuffer] in device units */
    abstract val height: Int

    /** the content scale of the [ColorBuffer] */
    abstract val contentScale: Double

    /**
     * the [ColorFormat] of the image stored in the [ColorBuffer]
     */
    abstract val format: ColorFormat

    /**
     * the [ColorType] of the image stored in the [ColorBuffer]
     */
    abstract val type: ColorType

    /** the number of mipmap levels */
    abstract val levels: Int

    /** the multisampling method used for this [ColorBuffer] */
    abstract val multisample: BufferMultisample

    /** the width of the [ColorBuffer] in pixels */
    val effectiveWidth: Int

    /** the height of the [ColorBuffer] in pixels */
    val effectiveHeight: Int

    val bounds: Rectangle

    /** permanently destroy the underlying [ColorBuffer] resources, [ColorBuffer] can not be used after it is destroyed */
    abstract fun destroy()

    /** bind the colorbuffer to a texture unit, internal API */
    abstract fun bind(unit: Int)
    /**
     * write the contents from [sourceBuffer] to the [ColorBuffer], potentially with format and type conversions
     *
     * The [sourceBuffer] should be allocated using [ByteBuffer.allocateDirect] and have an amount of remaining bytes
     * that matches with the dimensions, [sourceFormat] and [sourceType].
     * @param sourceBuffer a [ByteBuffer] holding raw image data
     * @param sourceFormat the [ColorFormat] that is used for the image data stored in [sourceBuffer], default is [ColorBuffer.format]
     * @param sourceType the [ColorType] that is used for the image data stored in [sourceBuffer], default is [ColorBuffer.type]
     * @param level the mipmap-level of [ColorBuffer] to write to
     */


    /** generates mipmaps from the top-level mipmap */
    abstract fun generateMipmaps()


    abstract fun write(sourceBuffer: MPPBuffer, sourceFormat: ColorFormat, sourceType: ColorType, x:Int = 0, y:Int = 0, width:Int, height:Int, level:Int = 0)

    /** the (unitless?) degree of anisotropy to be used in filtering */
    abstract var anisotropy: Double

    /**
     * should the v coordinate be flipped because the [ColorBuffer] contents are stored upside-down?
     */
    abstract var flipV: Boolean


    abstract fun filter(filterMin: MinifyingFilter, filterMag: MagnifyingFilter)


    /*
    abstract fun copyTo(
        target: ColorBuffer,
        fromLevel: Int = 0,
        toLevel: Int = 0,
        sourceRectangle: IntRectangle = IntRectangle(
            0,
            0,
            this.effectiveWidth / (1 shl fromLevel),
            this.effectiveHeight / (1 shl fromLevel)
        ),
        targetRectangle: IntRectangle = IntRectangle(
            0,
            0,
            sourceRectangle.width,
            sourceRectangle.height
        ),
        filter: MagnifyingFilter = MagnifyingFilter.NEAREST
    )
     */
    // TODO restore default arguments when https://youtrack.jetbrains.com/issue/KT-45542 is fixed
    abstract fun copyTo(
        target: ColorBuffer,
        fromLevel: Int = 0,
        toLevel: Int = 0,
        sourceRectangle: IntRectangle,
        targetRectangle: IntRectangle,
        filter: MagnifyingFilter = MagnifyingFilter.NEAREST
    )


    /**
     * copies contents to a target array texture
     * @param target the color buffer to which contents will be copied
     * @param layer the array layer from which will be copied
     * @param fromLevel the mip-map level from which will be copied
     * @param toLevel the mip-map level of [target] to which will be copied
     */
    abstract fun copyTo(target: ArrayTexture, layer: Int, fromLevel: Int = 0, toLevel: Int = 0)
}


/**
 * create a [ColorBuffer]
 * @param width the width in device units
 * @param height the height in device units
 * @param contentScale content scale used for denoting hi-dpi content
 * @param format the color format
 * @param type the color type
 * @param multisample multisampling mode to use
 * @param format the color format
 * @param levels the number of mip-map levels
 * @param session the [Session] that should track this color buffer
 */
fun colorBuffer(
    width: Int,
    height: Int,
    contentScale: Double = 1.0,
    format: ColorFormat = ColorFormat.RGBa,
    type: ColorType = ColorType.UINT8,
    multisample: BufferMultisample = BufferMultisample.Disabled,
    levels: Int = 1,
    session: Session? = Session.active
): ColorBuffer {
    return Driver.instance.createColorBuffer(width, height, contentScale, format, type, multisample, levels, session)
}



/**
 * load an image from a file or url encoded as [String], also accepts base64 encoded data urls
 */
expect fun loadImage(fileOrUrl: String, formatHint: ImageFileFormat? = null, session: Session? = Session.active): ColorBuffer

expect suspend fun loadImageSuspend(fileOrUrl: String, formatHint: ImageFileFormat? = null, session: Session? = Session.active): ColorBuffer