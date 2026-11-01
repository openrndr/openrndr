package org.openrndr.webgl

import WebGLRenderingFixedCompressedTexImage
import js.buffer.ArrayBufferLike
import js.buffer.ArrayBufferView
import js.typedarrays.Float16Array
import js.typedarrays.Float32Array
import js.typedarrays.Int16Array
import js.typedarrays.Int32Array
import js.typedarrays.Int8Array
import js.typedarrays.Uint16Array
import js.typedarrays.Uint32Array
import js.typedarrays.Uint8Array
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.internal.Driver
import org.openrndr.internal.shareGroupID
import org.openrndr.internal.glcommon.clipCopyRectangles
import org.openrndr.internal.glcommon.copyRectangleShader
import org.openrndr.internal.glcommon.defaultCopyRectangles
import org.openrndr.math.Vector2
import org.openrndr.math.Vector4
import org.openrndr.shape.IntRectangle
import org.openrndr.utils.buffer.MPPBuffer
import web.gl.*
import web.html.Image
import kotlin.math.log2
import kotlin.math.pow
import web.gl.WebGL2RenderingContext as GL

//inline val SRGB_ALPHA_EXT: GLenum
//    get() = GLenum(0x8C42)

/**
 * A color buffer of the WebGL back-end. WebGL 2 has no multisampled textures, multisampled color buffers are stored in
 * a multisampled [renderbuffer]: they can be attached to render targets and copied with [copyTo], which resolves them,
 * but not be sampled, written, read or mipmapped, like multisampled color buffers of the GL3 back-end.
 */
class ColorBufferWebGL(
    val context: GL,
    val target: GLenum,
    private val textureObject: WebGLTexture?,
    override val width: Int,
    override val height: Int,
    override val contentScale: Double,
    override val format: ColorFormat,
    override val type: ColorType,
    override val levels: Int,
    override val multisample: BufferMultisample,
    override val session: Session?,
    /** the storage of a multisampled color buffer */
    val renderbuffer: WebGLRenderbuffer? = null
) : ColorBuffer() {

    /**
     * The texture of a single sampled color buffer
     */
    val texture: WebGLTexture
        get() = textureObject ?: error("multisampled color buffers have no texture, copy them to a single sampled color buffer first: $this")

    private fun requireSingleSampled(operation: String) {
        require(multisample == BufferMultisample.Disabled) {
            "$operation is not possible for multisampled color buffers, copy them to a single sampled color buffer first: $this"
        }
    }

    /** the current filters, which copies that are drawn restore */
    private var filterMin = MinifyingFilter.LINEAR
    private var filterMag = MagnifyingFilter.LINEAR

    override fun close() {
        destroy()
    }

    companion object {
        /** the filters of copies that are drawn, one per share group (canvas) of the driver they were created with */
        private val copyRectangleFilters = mutableMapOf<Long, Filter>()
        private var copyRectangleFilterDriver: Driver? = null

        fun create(
            context: GL,
            width: Int,
            height: Int,
            contentScale: Double = 1.0,
            format: ColorFormat = ColorFormat.RGBa,
            type: ColorType = defaultColorType(format),
            multisample: BufferMultisample,
            levels: Int,
            session: Session?
        ): ColorBufferWebGL {
            context.checkErrors("pre-existing errors in ColorBufferWebGL.create()")
            val effectiveWidth = (width * contentScale).toInt()
            val effectiveHeight = (height * contentScale).toInt()
            val (internalFormat, glformat, gltype) = internalFormat(format, type)

            if (multisample is BufferMultisample.SampleCount) {
                require(!type.compressed && levels == 1) { "multisampled color buffers can not be compressed or have mipmaps" }
                require(type.colorSampling == ColorSampling.NORMALIZED) { "WebGL does not support multisampled integer color buffers" }
                // renderbuffers need sized formats, textures of 8 bit RGB(a) use the unsized ones
                val storageFormat = when (internalFormat) {
                    RGBA -> RGBA8
                    RGB -> RGB8
                    SRGB -> SRGB8
                    else -> internalFormat
                }
                val renderbuffer = context.createRenderbuffer() ?: error("failed to create renderbuffer")
                context.bindRenderbuffer(RENDERBUFFER, renderbuffer)
                context.renderbufferStorageMultisample(
                    RENDERBUFFER, multisample.samples(context), storageFormat, effectiveWidth, effectiveHeight
                )
                context.bindRenderbuffer(RENDERBUFFER, null)
                context.checkErrors("multisampled color buffer creation failed: $type $format $multisample")
                return ColorBufferWebGL(
                    context, TEXTURE_2D, null, width, height, contentScale, format, type, levels, multisample,
                    session, renderbuffer
                )
            }

            val texture = context.createTexture() ?: error("failed to create texture")
            context.checkErrors("create texture")
            context.activeTexture(TEXTURE0)
            context.checkErrors("active texture")
            context.bindTexture(TEXTURE_2D, texture)
            context.checkErrors("bindTexture")
            // without it a texture is incomplete for mipmap filters until all levels down to 1x1 exist
            context.texParameteri(TEXTURE_2D, TEXTURE_MAX_LEVEL, levels - 1)

            if (!type.compressed) {
                for (level in 0 until levels) {
                    val div = 1 shl level
                    context.texImage2D(
                        TEXTURE_2D,
                        level,
                        internalFormat,
                        effectiveWidth / div,
                        effectiveHeight / div,
                        0,
                        glformat,
                        gltype,
                        null
                    )
                    context.checkErrors("texture creation failed: $type $format")
                }
            } else {
                for (level in 0 until levels) {
                    val div = 1 shl level

                    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
                    val fcontext =
                        (context as? WebGLRenderingFixedCompressedTexImage) ?: error("cast failed")
                    fcontext.compressedTexImage2D(
                        TEXTURE_2D,
                        level,
                        internalFormat,
                        effectiveWidth / div,
                        effectiveHeight / div,
                        0,
                        null
                    )
                    context.checkErrors("compressedTexImage2D")
                }
            }

            val caps = (Driver.instance as DriverWebGL).capabilities
            val linear = type == ColorType.UINT8 || type == ColorType.FLOAT16 ||
                    (type == ColorType.FLOAT32 && caps.floatTexturesLinear)
            context.texParameteri(TEXTURE_2D, TEXTURE_WRAP_S, CLAMP_TO_EDGE)
            context.checkErrors()
            context.texParameteri(TEXTURE_2D, TEXTURE_WRAP_T, CLAMP_TO_EDGE)
            context.checkErrors()
            return ColorBufferWebGL(
                context,
                TEXTURE_2D,
                texture,
                width,
                height,
                contentScale,
                format,
                type,
                levels,
                multisample,
                session
            ).apply {
                if (linear) filter(MinifyingFilter.LINEAR, MagnifyingFilter.LINEAR)
                else filter(MinifyingFilter.NEAREST, MagnifyingFilter.NEAREST)
            }
        }

        fun fromImage(
            context: GL,
            image: Image,
            session: Session? = Session.active
        ): ColorBufferWebGL {
            val texture = context.createTexture() ?: error("failed to create texture")
            context.activeTexture(TEXTURE0)
            context.bindTexture(TEXTURE_2D, texture)
            // browsers decode images to sRGB colors, stored as sRGB they are decoded to linear when sampled
            context.texImage2D(TEXTURE_2D, 0, SRGB8_ALPHA8, RGBA, UNSIGNED_BYTE, image)
            if (log2(image.width.toDouble()) % 1.0 == 0.0 && log2(image.height.toDouble()) % 1.0 == 0.0) {
                context.generateMipmap(TEXTURE_2D)
            }
            context.texParameteri(TEXTURE_2D,TEXTURE_MIN_FILTER, LINEAR)
            context.checkErrors()
            context.texParameteri(TEXTURE_2D,TEXTURE_MAG_FILTER, LINEAR)
            context.checkErrors()
            context.texParameteri(TEXTURE_2D,TEXTURE_WRAP_S, CLAMP_TO_EDGE)
            context.checkErrors()
            context.texParameteri(TEXTURE_2D,TEXTURE_WRAP_T, CLAMP_TO_EDGE)
            context.checkErrors()
            return ColorBufferWebGL(
                context, TEXTURE_2D, texture, image.width, image.height, 1.0,
                ColorFormat.RGBa, ColorType.UINT8_SRGB, 1, BufferMultisample.Disabled, session
            )
        }
    }

    override fun destroy() {
        textureObject?.let { context.deleteTexture(it) }
        renderbuffer?.let { context.deleteRenderbuffer(it) }
        context.checkErrors()
    }

    fun bind(unit: Int) {
        requireSingleSampled("sampling")
        context.checkErrors("pre-existing errors")
        context.activeTexture(glTextureEnum(unit))
        context.checkErrors()
        context.bindTexture(target, texture)
        context.checkErrors("bindTexture unit:$unit $this")
    }

    override fun generateMipmaps() {
        requireSingleSampled("generating mipmaps")
        context.checkErrors("pre-existing errors")
        bind(0)
        context.generateMipmap(target)
        context.checkErrors("generateMipmap $this")
    }

    override var anisotropy: Double
        get() = TODO("Not yet implemented")
        set(_) {}

    override var flipV: Boolean = false

    override fun copyTo(
        target: ColorBuffer,
        fromLevel: Int,
        toLevel: Int,
        filter: MagnifyingFilter
    ) {
        // the overlapping region, anchored at the texture origin (the bottom-left corner), like the GL3 back-end
        val (sourceRectangle, targetRectangle) = defaultCopyRectangles(
            IntRectangle(0, 0, effectiveWidth / (1 shl fromLevel), effectiveHeight / (1 shl fromLevel)),
            IntRectangle(0, 0, target.effectiveWidth / (1 shl toLevel), target.effectiveHeight / (1 shl toLevel))
        )
        copyTo(target, fromLevel, toLevel, sourceRectangle, targetRectangle, filter)
    }

    @OptIn(ExperimentalWasmJsInterop::class)
    fun bound(f: ColorBufferWebGL.() -> Unit) {
        context.checkErrors("pre-existing errors in ColorBufferWebGL.bound()")
        context.activeTexture(TEXTURE0)
        val current = context.getParameter(TEXTURE_BINDING_2D) as WebGLTexture?
        context.bindTexture(target, texture)
        this.f()
        context.bindTexture(target, current)
    }

    /**
     * Copies [sourceRectangle] of level [fromLevel] to [targetRectangle] of level [toLevel] of [target], like the GL3
     * back-end under OpenGL ES: unscaled copies between single sampled color buffers of the same format and type use
     * copyTexSubImage2D, other unscaled copies that WebGL can blit are blitted, everything else is drawn, which scales
     * with [filter] and converts between sRGB and linear, fixed and floating point formats, and to multisampled
     * color buffers.
     */
    override fun copyTo(
        target: ColorBuffer,
        fromLevel: Int,
        toLevel: Int,
        sourceRectangle: IntRectangle,
        targetRectangle: IntRectangle,
        filter: MagnifyingFilter
    ) {
        target as ColorBufferWebGL
        require(target.format.componentCount != 3) { "WebGL can not copy to color buffers with an RGB format: $this -> $target" }
        require(!target.type.compressed) { "WebGL can not copy to compressed color buffers: $this -> $target" }

        val sourceLevel = IntRectangle(0, 0, effectiveWidth / (1 shl fromLevel), effectiveHeight / (1 shl fromLevel))
        val targetLevel = IntRectangle(0, 0, target.effectiveWidth / (1 shl toLevel), target.effectiveHeight / (1 shl toLevel))

        val scaled = sourceRectangle.width != targetRectangle.width || sourceRectangle.height != targetRectangle.height
        val sameFormat = type == target.type && format == target.format

        if (!scaled) {
            val (clippedSource, clippedTarget) =
                clipCopyRectangles(sourceRectangle, targetRectangle, sourceLevel, targetLevel) ?: return
            if (clippedSource != sourceRectangle || clippedTarget != targetRectangle) {
                return copyTo(target, fromLevel, toLevel, clippedSource, clippedTarget, filter)
            }
        }

        when {
            !scaled && sameFormat && multisample == BufferMultisample.Disabled && target.multisample == BufferMultisample.Disabled ->
                copyTexSubImage(target, fromLevel, toLevel, sourceRectangle, targetRectangle)

            !scaled && canBlit(target, sourceRectangle, targetRectangle) ->
                blit(target, fromLevel, toLevel, sourceRectangle, targetRectangle)

            else -> drawCopy(target, fromLevel, toLevel, sourceRectangle, targetRectangle, filter)
        }
    }

    /**
     * Can blitFramebuffer copy from [sourceRectangle] of this color buffer to [targetRectangle] of [target], without
     * scaling? WebGL blits follow OpenGL ES 3: they do not convert between fixed and floating point formats or encode
     * sRGB consistently, can not write to multisampled buffers and only resolve between identical formats and
     * rectangles.
     */
    private fun canBlit(target: ColorBufferWebGL, sourceRectangle: IntRectangle, targetRectangle: IntRectangle): Boolean {
        val sourceMultisampled = multisample != BufferMultisample.Disabled
        if (type.isSRGB != target.type.isSRGB) return false
        if (type.colorSampling != target.type.colorSampling) return false
        if (type.isFloat != target.type.isFloat || format.componentCount == 3) return false
        if (target.multisample != BufferMultisample.Disabled) return false
        if (sourceMultisampled && (type != target.type || format != target.format || sourceRectangle != targetRectangle)) return false
        return true
    }

    /**
     * A render target with level [level] of this color buffer as its only attachment
     */
    private fun levelTarget(level: Int): RenderTargetWebGL {
        val div = 1 shl level
        return renderTarget(width / div, height / div, contentScale, multisample = multisample) {
            colorBuffer(this@ColorBufferWebGL, level)
        } as RenderTargetWebGL
    }

    private fun RenderTargetWebGL.release() {
        detachColorAttachments()
        destroy()
    }

    /**
     * Copies without scaling between single sampled color buffers of the same format and type using copyTexSubImage2D
     */
    private fun copyTexSubImage(
        target: ColorBufferWebGL,
        fromLevel: Int,
        toLevel: Int,
        sourceRectangle: IntRectangle,
        targetRectangle: IntRectangle
    ) {
        val readTarget = levelTarget(fromLevel)
        readTarget.bind()
        context.readBuffer(COLOR_ATTACHMENT0)
        context.checkErrors("readBuffer $this $target")

        // color buffers are stored bottom-up, the rectangles are top-down
        val sourceHeight = effectiveHeight / (1 shl fromLevel)
        val targetHeight = target.effectiveHeight / (1 shl toLevel)
        target.bound {
            context.copyTexSubImage2D(
                target.target,
                toLevel,
                targetRectangle.x,
                targetHeight - targetRectangle.y - targetRectangle.height,
                sourceRectangle.x,
                sourceHeight - sourceRectangle.y - sourceRectangle.height,
                sourceRectangle.width,
                sourceRectangle.height
            )
            context.checkErrors("copyTexSubImage2D from $sourceRectangle of level $fromLevel to $targetRectangle of level $toLevel")
        }
        readTarget.unbind()
        readTarget.release()
    }

    /**
     * Copies without scaling using blitFramebuffer, which converts between some formats and resolves multisampled
     * color buffers
     */
    private fun blit(
        target: ColorBufferWebGL,
        fromLevel: Int,
        toLevel: Int,
        sourceRectangle: IntRectangle,
        targetRectangle: IntRectangle
    ) {
        val readTarget = levelTarget(fromLevel)
        val writeTarget = target.levelTarget(toLevel)

        writeTarget.bind()
        context.bindFramebuffer(READ_FRAMEBUFFER, readTarget.framebuffer)
        context.checkErrors("bindFramebuffer $this $target")

        // color buffers are stored bottom-up, the rectangles are top-down
        val sourceHeight = effectiveHeight / (1 shl fromLevel)
        val targetHeight = target.effectiveHeight / (1 shl toLevel)
        context.blitFramebuffer(
            sourceRectangle.x,
            sourceHeight - sourceRectangle.y,
            sourceRectangle.x + sourceRectangle.width,
            sourceHeight - sourceRectangle.y - sourceRectangle.height,
            targetRectangle.x,
            targetHeight - targetRectangle.y,
            targetRectangle.x + targetRectangle.width,
            targetHeight - targetRectangle.y - targetRectangle.height,
            COLOR_BUFFER_BIT,
            NEAREST
        )
        context.checkErrors("blitFramebuffer from $sourceRectangle of $this to $targetRectangle of $target")
        context.bindFramebuffer(READ_FRAMEBUFFER, null)
        writeTarget.unbind()

        writeTarget.release()
        readTarget.release()
    }

    /**
     * Copies by drawing the source rectangle into the target rectangle, which handles scaling with [filter],
     * conversions between sRGB and linear, fixed and floating point formats, and multisampled targets
     */
    private fun drawCopy(
        target: ColorBufferWebGL,
        fromLevel: Int,
        toLevel: Int,
        sourceRectangle: IntRectangle,
        targetRectangle: IntRectangle,
        filter: MagnifyingFilter
    ) {
        require(type.colorSampling == ColorSampling.NORMALIZED && target.type.colorSampling == ColorSampling.NORMALIZED) {
            "integer color buffers can only be copied to color buffers of the same format and type, without scaling: $this -> $target"
        }

        if (multisample != BufferMultisample.Disabled) {
            // multisampled color buffers can not be sampled, resolve them first
            val resolved = colorBuffer(width, height, contentScale, format, type, BufferMultisample.Disabled, 1) as ColorBufferWebGL
            copyTo(resolved)
            resolved.drawCopy(target, 0, toLevel, sourceRectangle, targetRectangle, filter)
            resolved.destroy()
            return
        }

        val writeTarget = target.levelTarget(toLevel)
        writeTarget.setBlendMode(0, BlendMode.REPLACE)

        val previousMin = filterMin
        val previousMag = filterMag
        this.filter(
            when (filter) {
                MagnifyingFilter.NEAREST -> if (levels > 1) MinifyingFilter.NEAREST_MIPMAP_NEAREST else MinifyingFilter.NEAREST
                MagnifyingFilter.LINEAR -> if (levels > 1) MinifyingFilter.LINEAR_MIPMAP_NEAREST else MinifyingFilter.LINEAR
            },
            filter
        )

        if (copyRectangleFilterDriver !== Driver.instance) {
            copyRectangleFilters.clear()
            copyRectangleFilterDriver = Driver.instance
        }
        val copy = copyRectangleFilters.getOrPut(Driver.instance.shareGroupID) {
            Filter(filterShaderFromCode(copyRectangleShader, "copy-rectangle")).apply { untrack() }
        }
        val fromDiv = 1 shl fromLevel
        val toDiv = 1 shl toLevel
        copy.parameters["sourceRectangle"] = Vector4(
            sourceRectangle.x.toDouble(), sourceRectangle.y.toDouble(),
            sourceRectangle.width.toDouble(), sourceRectangle.height.toDouble()
        )
        copy.parameters["targetRectangle"] = Vector4(
            targetRectangle.x.toDouble(), targetRectangle.y.toDouble(),
            targetRectangle.width.toDouble(), targetRectangle.height.toDouble()
        )
        copy.parameters["sourceLevelSize"] =
            Vector2((effectiveWidth / fromDiv).toDouble(), (effectiveHeight / fromDiv).toDouble())
        copy.parameters["targetLevelSize"] =
            Vector2((target.effectiveWidth / toDiv).toDouble(), (target.effectiveHeight / toDiv).toDouble())
        copy.parameters["sourceLevel"] = fromLevel.toDouble()
        copy.apply(arrayOf(this), writeTarget)

        this.filter(previousMin, previousMag)
        writeTarget.release()
    }

    override fun copyTo(target: ArrayTexture, layer: Int, fromLevel: Int, toLevel: Int) {
        TODO("Not yet implemented")
    }

    override fun write(
        source: TexImageSource,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        level: Int
    ) {
        require(!type.compressed)
        requireSingleSampled("writing")
        bind(0)
        context.pixelStorei(UNPACK_FLIP_Y_WEBGL, 1)
        this.context.texSubImage2D(target, level, x, y, RGBA, UNSIGNED_BYTE, source)
    }

    override fun write(
        source: ArrayBufferView<ArrayBufferLike>,
        sourceFormat: ColorFormat,
        sourceType: ColorType,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        level: Int
    ) {
        requireSingleSampled("writing")
        bind(0)
        // raw data is written row by row as given, with rows of any length, like the GL3 back-end
        context.pixelStorei(UNPACK_FLIP_Y_WEBGL, 0)
        context.pixelStorei(UNPACK_ALIGNMENT, 1)
        context.checkErrors("pixelStorei")

        
        
        fun format(colorFormat: ColorFormat, colorType: ColorType): GLenum {
            val srgb = context.getExtensionOrNull<EXT_sRGB>()

            return when (colorType) {
                ColorType.UINT8_SRGB -> {
                    when (colorFormat) {
                        ColorFormat.RGBa -> SRGB8_ALPHA8
                        else -> error("Unsupported color format $colorFormat for color type $colorType")
                    }
                }
                else -> colorFormat.glFormat()
            }
        }

        // typed arrays are created with their length in elements
        val sourceArray = when (sourceType) {
            ColorType.UINT8_SRGB, ColorType.UINT8, ColorType.UINT8_INT -> Uint8Array(source.buffer, source.byteOffset, source.byteLength)
            ColorType.SINT8_INT -> Int8Array(source.buffer, source.byteOffset, source.byteLength)
            ColorType.FLOAT16 -> Float16Array(source.buffer, source.byteOffset, source.byteLength / 2)
            ColorType.FLOAT32 -> Float32Array(source.buffer, source.byteOffset, source.byteLength / 4)
            ColorType.UINT16, ColorType.UINT16_INT -> Uint16Array(source.buffer, source.byteOffset, source.byteLength / 2)
            ColorType.SINT16_INT -> Int16Array(source.buffer, source.byteOffset, source.byteLength / 2)
            ColorType.UINT32_INT -> Uint32Array(source.buffer, source.byteOffset, source.byteLength / 4)
            ColorType.SINT32_INT -> Int32Array(source.buffer, source.byteOffset, source.byteLength / 4)
            else -> error("Unsupported color type $sourceType")
        }

        if (!sourceType.compressed) {
            this.context.texSubImage2D(
                target,
                level,
                x,
                y,
                width,
                height,
                // the pixel format of the table, integer data uses the *_INTEGER formats
                internalFormat(sourceFormat, sourceType).second,
                sourceType.glType(),
                sourceArray
            )
            context.checkErrors(
                "texSubImage2D, x:$x, y:$y, width: $width, height: $height, ${sourceFormat}, ${sourceType}, ${source.byteLength}, ${
                    format(
                        sourceFormat,
                        sourceType
                    )
                }"
            )
        } else {
            this.context.compressedTexSubImage2D(
                target,
                level,
                x,
                y,
                width,
                height,
                sourceType.glType(),
                sourceArray,
                0.0,
                0.toJsUInt()
            )
        }
    }

    override fun write(
        sourceBuffer: MPPBuffer,
        sourceFormat: ColorFormat,
        sourceType: ColorType,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        level: Int
    ) {
        @Suppress("UNCHECKED_CAST")
        write(
            sourceBuffer.dataView as ArrayBufferView<ArrayBufferLike>,
            sourceFormat, sourceType, x, y, width, height, level
        )
    }

    private val readFrameBuffer by lazy {
        context.createFramebuffer() ?: error("failed to create framebuffer")
    }

    @OptIn(ExperimentalWasmJsInterop::class)
    override fun read(
        target: ArrayBufferView<ArrayBufferLike>,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        level: Int
    ) {
        requireSingleSampled("reading")
        bind(0)
        val current = context.getParameter(FRAMEBUFFER_BINDING) as WebGLFramebuffer?
        context.bindFramebuffer(FRAMEBUFFER, readFrameBuffer)
        context.framebufferTexture2D(FRAMEBUFFER, COLOR_ATTACHMENT0, this.target, texture, 0)
        context.readPixels(x, y, effectiveWidth, effectiveHeight, RGBA, UNSIGNED_BYTE, target)
        context.bindFramebuffer(FRAMEBUFFER, current)
    }

    override fun filter(filterMin: MinifyingFilter, filterMag: MagnifyingFilter) {
        if (multisample != BufferMultisample.Disabled) {
            // multisampled color buffers are not sampled
            return
        }
        bind(0)
        context.texParameteri(target, TEXTURE_MIN_FILTER, filterMin.toGLFilter())
        context.texParameteri(target, TEXTURE_MAG_FILTER, filterMag.toGLFilter())
        this.filterMin = filterMin
        this.filterMag = filterMag
    }

    override var wrapU: WrapMode
        get() = TODO("Not yet implemented")
        set(_) {
            bound {
                context.texParameteri(target, TEXTURE_WRAP_S, wrapU.toGLWrap())
            }
        }
    override var wrapV: WrapMode
        get() = TODO("Not yet implemented")
        set(_) {
            bound {
                context.texParameteri(target, TEXTURE_WRAP_T, wrapU.toGLWrap())
            }
        }

    override fun fill(color: ColorRGBa, level: Int) {
        val lcolor = color.toLinear()
        val lwidth = (width / 2.0.pow(level.toDouble())).toInt()
        val lheight = (height / 2.0.pow(level.toDouble())).toInt()

        val writeTarget = renderTarget(lwidth, lheight, contentScale) {
            colorBuffer(this@ColorBufferWebGL, level)
        } as RenderTargetWebGL

        writeTarget.bind()
        val floatColorData =
            float32Array(
                lcolor.r.toFloat(),
                lcolor.g.toFloat(),
                lcolor.b.toFloat(),
                lcolor.alpha.toFloat()
            )
        context.clearBufferfv(COLOR, 0, floatColorData, 0.0)
        writeTarget.unbind()

        writeTarget.detachColorAttachments()
        writeTarget.destroy()
    }

    override fun toString(): String {
        return "ColorBufferWebGL(target=$target, width=$width, height=$height, contentScale=$contentScale, format=$format, type=$type, levels=$levels, multisample=$multisample, flipV=$flipV)"
    }
}

private fun WrapMode.toGLWrap(): GLenum {

    return when(this) {
        WrapMode.CLAMP_TO_EDGE -> CLAMP_TO_EDGE
        WrapMode.MIRRORED_REPEAT -> MIRRORED_REPEAT
        WrapMode.REPEAT -> REPEAT
    }
}
