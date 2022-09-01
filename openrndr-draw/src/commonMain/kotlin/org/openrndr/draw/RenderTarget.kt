package org.openrndr.draw

import org.openrndr.color.ColorRGBa
import org.openrndr.internal.Driver
import org.openrndr.shape.IntRectangle
import org.openrndr.draw.colorBuffer as _colorBuffer
import org.openrndr.draw.depthBuffer as _depthBuffer

/**
 * Color attachment for [RenderTarget].
 * @param index the binding index for [RenderTarget]
 * @param name an optional name for the binding, defaults to `null`
 */
sealed class ColorAttachment(val index: Int, val name: String?)
class ColorBufferAttachment(
    index: Int,
    name: String?,
    val colorBuffer: ColorBuffer,
    val level: Int
) : ColorAttachment(index, name)

class ArrayTextureAttachment(
    index: Int,
    name: String?,
    val arrayTexture: ArrayTexture,
    val layer: Int,
    val level: Int
) : ColorAttachment(index, name)

class LayeredArrayTextureAttachment(
    index: Int,
    name: String?,
    val arrayTexture: ArrayTexture,
    val level: Int
) : ColorAttachment(index, name)

class VolumeTextureAttachment(
    index: Int,
    name: String?,
    val volumeTexture: VolumeTexture,
    val layer: Int,
    val level: Int
) : ColorAttachment(index, name)

class LayeredVolumeTextureAttachment(
    index: Int,
    name: String?,
    val volumeTexture: VolumeTexture,
    val level: Int
) : ColorAttachment(index, name)

class ArrayCubemapAttachment(
    index: Int,
    name: String?,
    val arrayCubemap: ArrayCubemap,
    val side: CubemapSide,
    val layer: Int,
    val level: Int
) : ColorAttachment(index, name)

class LayeredArrayCubemapAttachment(
    index: Int,
    name: String?,
    val arrayCubemap: ArrayCubemap,
    val level: Int
) : ColorAttachment(index, name)

class CubemapAttachment(
    index: Int,
    name: String?,
    val cubemap: Cubemap,
    val side: CubemapSide,
    val level: Int
) : ColorAttachment(index, name)

class LayeredCubemapAttachment(
    index: Int,
    name: String?,
    val cubemap: Cubemap,
    val level: Int
) : ColorAttachment(index, name)

interface RenderTarget {
    /** [Session] in which this render target is created. */
    val session: Session?

    /** Width in display units. */
    val width: Int

    /** Height in display units. */
    val height: Int

    /** Content scaling factor. */
    val contentScale: Double

    /** Multisampling mode. */
    val multisample: BufferMultisample

    /** Effective width in pixels. */
    //@Deprecated("use pixelWidth", replaceWith = ReplaceWith("pixelWidth"))
    val effectiveWidth: Int get() = (width * contentScale).toInt()

    /** Effective width in pixels. */
    val pixelWidth: Int get() = (width * contentScale).toInt()

    /** Effective height in pixels. */
    //@Deprecated("use pixelHeight", replaceWith = ReplaceWith("pixelHeight"))
    val effectiveHeight: Int get() = (height * contentScale).toInt()

    /** Effective height in pixels. */
    val pixelHeight: Int get() = (height * contentScale).toInt()

    /** List of [ColorAttachment]s. */
    val colorAttachments: List<ColorAttachment>

    /** Find the index of a [ColorAttachment] by name. */
    fun colorAttachmentIndexByName(name: String): Int? {
        return colorAttachments.find { it.name == name }?.index
    }

    /** Find [ColorAttachment] by name. */
    fun colorAttachmentByName(name: String): ColorAttachment? {
        return colorAttachments.find { it.name == name }
    }

    val depthBuffer: DepthBuffer?

    companion object {
        /**
         * Points to the [RenderTarget] that is currently in use. This is either an
         * instance of ProgramRenderTarget, which represents the window's surface or to
         * a user created RenderTarget that is made active using [Drawer.withTarget] or similar.
         */
        val active: RenderTarget
            get() = Driver.instance.activeRenderTarget
    }

    fun attach(colorBuffer: ColorBuffer, level: Int = 0, name: String? = null)
    fun attach(depthBuffer: DepthBuffer)

    fun attach(arrayTexture: ArrayTexture, layer: Int, level: Int = 0, name: String? = null)
    fun attach(arrayCubemap: ArrayCubemap, side: CubemapSide, layer: Int, level: Int = 0, name: String? = null)
    fun attach(cubemap: Cubemap, side: CubemapSide, level: Int = 0, name: String? = null)
    fun attach(volumeTexture: VolumeTexture, layer: Int, level: Int = 0, name: String? = null)

    fun attachLayered(arrayTexture: ArrayTexture, level: Int = 0, name: String? = null)
    fun attachLayered(arrayCubemap: ArrayCubemap, level: Int = 0, name: String? = null)
    fun attachLayered(cubemap: Cubemap, level: Int = 0, name: String? = null)
    fun attachLayered(volumeTexture: VolumeTexture, level: Int = 0, name: String? = null)

    /** Detach all color attachments. */
    fun detachColorAttachments()

    @Deprecated("detachColorBuffer is deprecated, use detachColorAttachments", replaceWith = ReplaceWith("detachColorAttachments"))
    fun detachColorBuffers()

    /** Detach the [depthBuffer]. */
    fun detachDepthBuffer()

    /**
     * Destroy the [RenderTarget].
     *
     * In order to fully destroy the RenderTarget, you should
     *   1. Destroy the color attachments and the depth attachment
     *   2. Call [detachColorAttachments] and [detachDepthBuffer]
     *   3. Call [destroy]
     */
    fun destroy()

    /** Get a color attachment as a [ColorBuffer]. */
    fun colorBuffer(index: Int): ColorBuffer

    /**
     * Clear a color attachment.
     * @param index the index of the attachment to clear
     * @param color the [ColorRGBa] to use for clearing
     */
    fun clearColor(index: Int, color: ColorRGBa)

    /**
     * Clear the depth attachment.
     * @param depth the value to use for clearing the depth attachment, defaults to `1.0`
     * @param stencil the value to use for clearing the stencil attachment, defaults to `0`
     */
    fun clearDepth(depth: Double = 1.0, stencil: Int = 0)

    /**
     * Set the [BlendMode] for a [ColorAttachment].
     * @param index the attachment index
     */
    fun blendMode(index: Int, blendMode: BlendMode)

    /** Binds the [RenderTarget] to the active target. */
    fun bind()

    /** Unbinds the [RenderTarget] from the active target. */
    fun unbind()

    /** Indicates if this [RenderTarget] has a [DepthBuffer]. */
    val hasDepthBuffer: Boolean

    /** Indicates if this [RenderTarget] has at least one [ColorAttachment]. */
    val hasColorAttachments: Boolean

    fun resolveTo(to: RenderTarget) {
        require(this.width == to.width && this.height == to.height)
        require(colorAttachments.size == to.colorAttachments.size)
        require((to.depthBuffer == null) == (depthBuffer == null))
        for (i in colorAttachments.indices) {
            when (val a = colorAttachments[i]) {
                is ColorBufferAttachment -> {
                    // TODO remove sourceRectangle and targetRectangle arguments when https://youtrack.jetbrains.com/issue/KT-45542 is fixed
                    val sourceRectangle = IntRectangle(
                    0,
                    0,
                    this.effectiveWidth / (1 shl 0),
                    this.effectiveHeight / (1 shl 0)
                    )
                    val targetRectangle = IntRectangle(0,0, sourceRectangle.width, sourceRectangle.height)
                    a.colorBuffer.copyTo((to.colorAttachments[i] as ColorBufferAttachment).colorBuffer, sourceRectangle = sourceRectangle, targetRectangle = targetRectangle)
                }
                else -> {

                }


            }
        }
        depthBuffer?.resolveTo((to.depthBuffer!!))
    }
}

@Suppress("unused")
class RenderTargetBuilder(private val renderTarget: RenderTarget) {

    @Deprecated("you should not use this", replaceWith = ReplaceWith("colorBuffer()"), level = DeprecationLevel.ERROR)
    @Suppress("UNUSED_PARAMETER")
    fun colorBuffer(width: Int, height: Int, contentScale: Double = 1.0, format: ColorFormat = ColorFormat.RGBa, type: ColorType = ColorType.UINT8, multisample: BufferMultisample): Nothing {
        throw IllegalStateException("use colorBuffer without width and height arguments")
    }

    /**
     * attach an existing [ColorBuffer] to the [RenderTarget]
     * @param colorBuffer an existing [ColorBuffer]
     * @param level the [ColorBuffer]'s mipmap-level to attach, default is 0
     */
    fun colorBuffer(colorBuffer: ColorBuffer, level: Int = 0) {
        renderTarget.attach(colorBuffer, level)
    }

    /**
     * attach an existing [ColorBuffer] to the [RenderTarget] and give it a name
     * @param name the name for the attachment
     * @param colorBuffer an existing [ColorBuffer]
     * @param level the [ColorBuffer]'s mipmap-level to attach, default is 0
     */
    fun colorBuffer(name: String, colorBuffer: ColorBuffer, level: Int = 0) {
        if (colorBuffer.multisample == renderTarget.multisample) {
            renderTarget.attach(colorBuffer, level, name)
        } else {
            throw IllegalArgumentException("${colorBuffer.multisample} != ${renderTarget.multisample}")
        }
    }

    /**
     * create a new [ColorBuffer] and create a named attachment to the [RenderTarget]
     * @param name the name for the attachment
     * @param format the [ColorFormat] for the [ColorBuffer], default is [ColorFormat.RGBa]
     * @param type the [ColorType] for the [ColorBuffer], default is [ColorType.UINT8]
     */
    fun colorBuffer(name: String, format: ColorFormat = ColorFormat.RGBa, type: ColorType = ColorType.UINT8) {
        val cb = _colorBuffer(renderTarget.width, renderTarget.height, renderTarget.contentScale, format, type, renderTarget.multisample)
        renderTarget.attach(cb, 0, name)
    }

    /**
     * create a new [ColorBuffer] and create a nameless attachment to the [RenderTarget]
     * @param format the [ColorFormat] for the [ColorBuffer], default is [ColorFormat.RGBa]
     * @param type the [ColorType] for the [ColorBuffer], default is [ColorType.UINT8]
     */
    fun colorBuffer(format: ColorFormat = ColorFormat.RGBa, type: ColorType = ColorType.UINT8) {
        val cb = _colorBuffer(renderTarget.width, renderTarget.height, renderTarget.contentScale, format, type, renderTarget.multisample)
        renderTarget.attach(cb)
    }

    /**
     * attach an existing [ArrayTexture] with a named binding to [RenderTarget]
     * @param name the name for the attachment
     * @param arrayTexture the [ArrayTexture] to attach
     * @param layer the layer of the [ArrayTexture] to attach
     * @param level mipmap level of the [ArrayTexture] to attach, default is 0
     */
    fun arrayTexture(name: String, arrayTexture: ArrayTexture, layer: Int, level: Int = 0) {
        renderTarget.attach(arrayTexture, layer, level, name)
    }

    /**
     * attach an existing [ArrayTexture] with a named binding to [RenderTarget]
     * @param arrayTexture the [ArrayTexture] to attach
     * @param layer the layer of the [ArrayTexture] to attach
     * @param level mipmap level of the [ArrayTexture] to attach, default is 0
     */
    fun arrayTexture(arrayTexture: ArrayTexture, layer: Int, level: Int = 0) {
        renderTarget.attach(arrayTexture, layer, level)
    }

    /**
     * attach an existing [ArrayCubemap] with a named binding to [RenderTarget]
     * @param name the name for the attachment
     * @param arrayCubemap the [ArrayCubemap] to attach
     * @param side the [CubemapSide] of the [ArrayCubemap] to attach
     * @param layer the layer of the [ArrayCubemap] to attach
     * @param level mipmap level of the [ArrayCubemap] to attach, default is 0
     */
    fun arrayCubemap(name: String, arrayCubemap: ArrayCubemap, side: CubemapSide, layer: Int, level: Int = 0) {
        renderTarget.attach(arrayCubemap, side, layer, level, name)
    }

    /**
     * attach an existing [ArrayCubemap] with a nameless binding to [RenderTarget]
     * @param arrayCubemap the [ArrayCubemap] to attach
     * @param side the [CubemapSide] of the [ArrayCubemap] to attach
     * @param layer the layer of the [ArrayCubemap] to attach
     * @param level level of the [ArrayCubemap] to attach, default is 0
     */
    fun arrayCubemap(arrayCubemap: ArrayCubemap, side: CubemapSide, layer: Int, level: Int = 0) {
        renderTarget.attach(arrayCubemap, side, layer, level)
    }

    /**
     * attach an existing [Cubemap] with a named binding to [RenderTarget]
     * @param name the name for the attachment
     * @param cubemap the [Cubemap] to attach
     * @param side the [CubemapSide] of the [Cubemap] to attach
     * @param level mipmap level of the [Cubemap] to attach, default is 0
     */
    fun cubemap(name: String, cubemap: Cubemap, side: CubemapSide, level: Int = 0) {
        renderTarget.attach(cubemap, side, level, name)
    }

    /**
     * attach an existing [Cubemap] with a nameless binding to [RenderTarget]
     * @param cubemap the [Cubemap] to attach
     * @param side the [CubemapSide] of the [Cubemap] to attach
     * @param level mipmap level of the [Cubemap] to attach, default is 0
     */
    fun cubemap(cubemap: Cubemap, side: CubemapSide, level: Int = 0) {
        renderTarget.attach(cubemap, side, level)
    }

    /**
     * attach an existing [VolumeTexture] with a named binding to [RenderTarget]
     * @param name the name for the attachment
     * @param volumeTexture the [VolumeTexture] to attach
     * @param layer the layer of the [VolumeTexture] to attach
     * @param level mipmap level of the [VolumeTexture] to attach, default is 0
     */
    fun volumeTexture(name: String, volumeTexture: VolumeTexture, layer: Int, level: Int = 0) {
        renderTarget.attach(volumeTexture, layer, level, name)
    }

    /**
     * attach an existing [VolumeTexture] with a nameless binding to [RenderTarget]
     * @param volumeTexture the [VolumeTexture] to attach
     * @param layer the layer of the [VolumeTexture] to attach
     * @param level mipmap level of the [VolumeTexture] to attach, default is 0
     */
    fun volumeTexture(volumeTexture: VolumeTexture, layer: Int, level: Int = 0) {
        renderTarget.attach(volumeTexture, layer, level)
    }

    /**
     * create and attach a [DepthBuffer]
     * @param format the [DepthFormat] for the [DepthBuffer], default is [DepthFormat.DEPTH_STENCIL]
     */
    fun depthBuffer(format: DepthFormat = DepthFormat.DEPTH_STENCIL) {
        renderTarget.attach(_depthBuffer(renderTarget.effectiveWidth, renderTarget.effectiveHeight, format, renderTarget.multisample))
        renderTarget.clearDepth()
    }

    /**
     * attach an existing [DepthBuffer]
     *  @param depthBuffer the [DepthBuffer] to attach to [RenderTarget]
     */
    fun depthBuffer(depthBuffer: DepthBuffer) {
        if (depthBuffer.multisample == renderTarget.multisample) {
            renderTarget.attach(depthBuffer)
        } else {
            throw IllegalArgumentException("${depthBuffer.multisample} != ${renderTarget.multisample}")
        }
    }
}

/**
 * build a [RenderTarget]
 * @param width positive non-zero width of the [RenderTarget]
 * @param height positive non-zero height of the [RenderTarget]
 * @param multisample determine if the [RenderTarget] uses multi-sampling, default is disabled
 * @param session specify the session under which the [RenderTarget] should be created, default is [Session.active]
 */
fun renderTarget(width: Int, height: Int,
                 contentScale: Double = 1.0,
                 multisample: BufferMultisample = BufferMultisample.Disabled,
                 session: Session? = Session.active,
                 builder: RenderTargetBuilder.() -> Unit): RenderTarget {
    if (width <= 0 || height <= 0) {
        throw IllegalArgumentException("unsupported resolution ($width×$height)")
    }

    val renderTarget = Driver.instance.createRenderTarget(width, height, contentScale, multisample, session)
    RenderTargetBuilder(renderTarget).builder()
    return renderTarget
}