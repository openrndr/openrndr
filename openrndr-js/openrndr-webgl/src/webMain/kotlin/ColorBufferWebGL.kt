package org.openrndr.webgl

import WebGLRenderingFixedCompressedTexImage
import js.buffer.ArrayBufferLike
import js.buffer.ArrayBufferView
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.internal.Driver
import org.openrndr.shape.IntRectangle
import org.openrndr.utils.buffer.MPPBuffer
import web.gl.GLenum
import web.gl.TexImageSource
import web.gl.WebGLFramebuffer
import web.gl.WebGLTexture
import web.html.Image
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.math.log2
import kotlin.math.pow
import web.gl.WebGL2RenderingContext as GL


class ColorBufferWebGL(
    val context: GL,
    val target: GLenum,
    val texture: WebGLTexture,
    override val width: Int,
    override val height: Int,
    override val contentScale: Double,
    override val format: ColorFormat,
    override val type: ColorType,
    override val levels: Int,
    override val multisample: BufferMultisample,
    override val session: Session?

) : ColorBuffer() {

    override fun close() {
        destroy()
    }

    companion object {
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
            if (type == ColorType.FLOAT16) {
                require((Driver.instance as DriverWebGL).capabilities.halfFloatTextures) {
                    """no support for half float textures."""
                }
            }
            if (type == ColorType.FLOAT32) {
                require((Driver.instance as DriverWebGL).capabilities.floatTextures) {
                    """no support for float textures"""
                }
            }
            val texture = context.createTexture() ?: error("failed to create texture")
            context.activeTexture(GL.TEXTURE0)
            when (multisample) {
                BufferMultisample.Disabled -> context.bindTexture(GL.TEXTURE_2D, texture)
                is BufferMultisample.SampleCount -> error("multisample not supported on WebGL(1)")
            }
            val effectiveWidth = (width * contentScale).toInt()
            val effectiveHeight = (height * contentScale).toInt()
            if (levels > 1) {
                //context.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_MAX_LEVEL, levels - 1)
            }
            val (internalFormat, glformat, gltype) = internalFormat(format, type)

            if (!type.compressed) {
                for (level in 0 until levels) {
                    val div = 1 shl level
                    context.texImage2D(
                        GL.TEXTURE_2D,
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
                        GL.TEXTURE_2D,
                        level,
                        internalFormat,
                        effectiveWidth / div,
                        effectiveHeight / div,
                        0,
                        null
                    )
                }
            }

            val caps = (Driver.instance as DriverWebGL).capabilities
            if (type == ColorType.UINT8 ||
                (type == ColorType.FLOAT16 && caps.halfFloatTexturesLinear) ||
                (type == ColorType.FLOAT32 && caps.floatTexturesLinear)
            ) {
                context.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_MIN_FILTER, GL.LINEAR)
                context.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_MAG_FILTER, GL.LINEAR)
            } else {
                context.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_MIN_FILTER, GL.NEAREST)
                context.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_MAG_FILTER, GL.NEAREST)
            }
            context.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_WRAP_S, GL.CLAMP_TO_EDGE)
            context.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_WRAP_T, GL.CLAMP_TO_EDGE)
            return ColorBufferWebGL(
                context,
                GL.TEXTURE_2D,
                texture,
                width,
                height,
                contentScale,
                format,
                type,
                levels,
                multisample,
                session
            )
        }

        fun fromImage(
            context: GL,
            image: Image,
            session: Session? = Session.active
        ): ColorBufferWebGL {
            val texture = context.createTexture() ?: error("failed to create texture")
            context.activeTexture(GL.TEXTURE0)
            context.bindTexture(GL.TEXTURE_2D, texture)
            context.texImage2D(GL.TEXTURE_2D, 0, GL.RGBA, GL.RGBA, GL.UNSIGNED_BYTE, image)
            if (log2(image.width.toDouble()) % 1.0 == 0.0 && log2(image.height.toDouble()) % 1.0 == 0.0) {
                context.generateMipmap(GL.TEXTURE_2D)
            }
            context.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_MIN_FILTER, GL.LINEAR)
            context.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_MAG_FILTER, GL.LINEAR)
            context.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_WRAP_S, GL.CLAMP_TO_EDGE)
            context.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_WRAP_T, GL.CLAMP_TO_EDGE)
            return ColorBufferWebGL(
                context, GL.TEXTURE_2D, texture, image.width, image.height, 1.0,
                ColorFormat.RGBa, ColorType.UINT8_SRGB, 1, BufferMultisample.Disabled, session
            )
        }
    }

    override fun destroy() {
        context.deleteTexture(texture)
    }

    override fun bind(unit: Int) {
        context.checkErrors("pre-existing errors")
        context.activeTexture(glTextureEnum(unit))
        context.bindTexture(target, texture)
        context.checkErrors("bindTexture unit:$unit $this")
    }

    override fun generateMipmaps() {
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
        val sourceRectangle = IntRectangle(
            0,
            0,
            this.effectiveWidth / (1 shl fromLevel),
            this.effectiveHeight / (1 shl fromLevel)
        )
        val targetRectangle = IntRectangle(
            0,
            0,
            sourceRectangle.width,
            sourceRectangle.height
        )
        copyTo(target, fromLevel, toLevel, sourceRectangle, targetRectangle, filter)
    }

    fun bound(f: ColorBufferWebGL.() -> Unit) {
        context.activeTexture(GL.TEXTURE0)
        val current = context.getParameter(GL.TEXTURE_BINDING_2D) as WebGLTexture?
        context.bindTexture(target, texture)
        this.f()
        context.bindTexture(target, current)
    }

    override fun copyTo(
        target: ColorBuffer,
        fromLevel: Int,
        toLevel: Int,
        sourceRectangle: IntRectangle,
        targetRectangle: IntRectangle,
        filter: MagnifyingFilter
    ) {
        val fromDiv = 1 shl fromLevel
        val toDiv = 1 shl toLevel
        val refRectangle = IntRectangle(0, 0, effectiveWidth / fromDiv, effectiveHeight / fromDiv)

        val useTexSubImage = false
        //target.type.compressed || (refRectangle == sourceRectangle && refRectangle == targetRectangle && multisample == target.multisample)

        if (!useTexSubImage) {
            val readTarget = renderTarget(
                width / fromDiv,
                height / fromDiv,
                contentScale,
                multisample = multisample
            ) {
                colorBuffer(this@ColorBufferWebGL, fromLevel)
            } as RenderTargetWebGL

            val writeTarget = renderTarget(
                target.width / toDiv,
                target.height / toDiv,
                target.contentScale,
                multisample = target.multisample
            ) {
                colorBuffer(target, toLevel)
            } as RenderTargetWebGL

            writeTarget.bind()
            context.bindFramebuffer(GL.READ_FRAMEBUFFER, readTarget.framebuffer)
            context.checkErrors("bindFrameBuffer $this $target")

            val ssx = sourceRectangle.x
            val ssy = sourceRectangle.y
            val sex = sourceRectangle.width + ssx
            val sey = sourceRectangle.height + ssy

            val tsx = targetRectangle.x
            val tsy = targetRectangle.y
            val tex = targetRectangle.width + tsx
            val tey = targetRectangle.height + tsy

            fun sflip(y: Int): Int {
                return this.effectiveHeight / fromDiv - y
            }

            fun tflip(y: Int): Int {
                return target.effectiveHeight / toDiv - y
            }

            context.blitFramebuffer(
                ssx,
                sflip(ssy),
                sex,
                sflip(sey),
                tsx,
                tflip(tsy),
                tex,
                tflip(tey),
                GL.COLOR_BUFFER_BIT,
                filter.toGLFilter()
            )
            context.bindFramebuffer(GL.READ_FRAMEBUFFER, null)
            context.checkErrors("blitFramebuffer $this $target")
            writeTarget.unbind()

            writeTarget.detachColorAttachments()
            writeTarget.destroy()

            readTarget.detachColorAttachments()
            readTarget.destroy()
        } else {
            require(sourceRectangle == refRectangle && targetRectangle == refRectangle) {
                "cropped or scaled copyTo is not allowed with the selected color buffers: $this -> $target"
            }

            val useFrameBufferCopy =
                true // Driver.glVersion < DriverVersionGL.VERSION_4_3 || (type != target.type || format != target.format)

            if (useFrameBufferCopy) {
//                checkDestroyed()
                val readTarget = renderTarget(width / fromDiv, height / fromDiv, contentScale) {
                    colorBuffer(this@ColorBufferWebGL, fromLevel)
                } as RenderTargetWebGL

                target as ColorBufferWebGL
                readTarget.bind()
                context.readBuffer(GL.COLOR_ATTACHMENT0)
                context.checkErrors("readBuffer $this $target")
                target.bound {
                    context.copyTexSubImage2D(
                        target.target,
                        toLevel,
                        0,
                        0,
                        0,
                        0,
                        target.effectiveWidth / toDiv,
                        target.effectiveHeight / toDiv
                    )
                    context.checkErrors("copyTexSubImage2D $this $target")
//                    debugGLErrors() {
//                        when (it) {
//                            GL_INVALID_VALUE -> "level ($toLevel) less than 0, effective target is GL_TEXTURE_RECTANGLE (${target.target == GL_TEXTURE_RECTANGLE} and level is not 0"
//                            else -> null
//                        }
//                    }
                }
                readTarget.unbind()

                readTarget.detachColorAttachments()
                readTarget.destroy()
            }
        }
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
        bind(0)
        context.pixelStorei(GL.UNPACK_FLIP_Y_WEBGL, 1)
        this.context.texSubImage2D(target, level, x, y, GL.RGBA, GL.UNSIGNED_BYTE, source)
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
        bind(0)
        context.pixelStorei(GL.UNPACK_FLIP_Y_WEBGL, 1)

        if (!sourceType.compressed) {
            this.context.texSubImage2D(
                target,
                level,
                x,
                y,
                width,
                height,
                sourceFormat.glFormat(),
                sourceType.glType(),
                source
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
                source,
                null,
                null
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
        write(sourceBuffer, sourceFormat, sourceType, x, y, width, height, level)
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
        bind(0)
        val current = context.getParameter(GL.FRAMEBUFFER_BINDING) as WebGLFramebuffer?
        context.bindFramebuffer(GL.FRAMEBUFFER, readFrameBuffer)
        context.framebufferTexture2D(GL.FRAMEBUFFER, GL.COLOR_ATTACHMENT0, this.target, texture, 0)
        context.readPixels(x, y, effectiveWidth, effectiveHeight, GL.RGBA, GL.UNSIGNED_BYTE, target)
        context.bindFramebuffer(GL.FRAMEBUFFER, current)
    }

    override fun filter(filterMin: MinifyingFilter, filterMag: MagnifyingFilter) {
        bind(0)
        context.texParameteri(target, GL.TEXTURE_MIN_FILTER, filterMin.toGLFilter())
        context.texParameteri(target, GL.TEXTURE_MAG_FILTER, filterMag.toGLFilter())
    }

    override var wrapU: WrapMode
        get() = TODO("Not yet implemented")
        set(_) {}
    override var wrapV: WrapMode
        get() = TODO("Not yet implemented")
        set(_) {}

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
        context.clearBufferfv(GL.COLOR, 0, floatColorData, null)
        writeTarget.unbind()

        writeTarget.detachColorAttachments()
        writeTarget.destroy()
    }

    override fun toString(): String {
        return "ColorBufferWebGL(target=$target, width=$width, height=$height, contentScale=$contentScale, format=$format, type=$type, levels=$levels, multisample=$multisample, flipV=$flipV)"
    }
}
