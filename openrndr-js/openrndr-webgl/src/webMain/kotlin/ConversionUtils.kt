package org.openrndr.webgl

import js.buffer.ArrayBuffer
import js.typedarrays.Float32Array
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.math.*
import web.gl.GLenum
import web.gl.WebGL2RenderingContext as GL

internal fun MinifyingFilter.toGLFilter(): GLenum {
    return when (this) {
        MinifyingFilter.NEAREST -> GL.NEAREST
        MinifyingFilter.LINEAR -> GL.LINEAR
        MinifyingFilter.LINEAR_MIPMAP_LINEAR -> GL.LINEAR_MIPMAP_LINEAR
        MinifyingFilter.LINEAR_MIPMAP_NEAREST -> GL.LINEAR_MIPMAP_NEAREST
        MinifyingFilter.NEAREST_MIPMAP_LINEAR -> GL.NEAREST_MIPMAP_LINEAR
        MinifyingFilter.NEAREST_MIPMAP_NEAREST -> GL.NEAREST_MIPMAP_NEAREST
    }
}

internal fun MagnifyingFilter.toGLFilter(): GLenum {
    return when (this) {
        MagnifyingFilter.NEAREST -> GL.NEAREST
        MagnifyingFilter.LINEAR -> GL.LINEAR
    }
}

internal expect fun float32Array(vararg floats: Float): Float32Array<ArrayBuffer>

internal fun Matrix44.toFloat32Array(): Float32Array<ArrayBuffer> = float32Array(
    c0r0.toFloat(), c0r1.toFloat(), c0r2.toFloat(), c0r3.toFloat(),
    c1r0.toFloat(), c1r1.toFloat(), c1r2.toFloat(), c1r3.toFloat(),
    c2r0.toFloat(), c2r1.toFloat(), c2r2.toFloat(), c2r3.toFloat(),
    c3r0.toFloat(), c3r1.toFloat(), c3r2.toFloat(), c3r3.toFloat())

internal fun Matrix33.toFloat32Array(): Float32Array<ArrayBuffer> = float32Array(
    c0r0.toFloat(), c0r1.toFloat(), c0r2.toFloat(),
    c1r0.toFloat(), c1r1.toFloat(), c1r2.toFloat(),
    c2r0.toFloat(), c2r1.toFloat(), c2r2.toFloat())

internal fun ColorRGBa.toFloat32Array(): Float32Array<ArrayBuffer> = float32Array(
    r.toFloat(), g.toFloat(), b.toFloat(), alpha.toFloat()
)

internal fun Vector4.toFloat32Array(): Float32Array<ArrayBuffer> = float32Array(
    x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat()
)

internal fun Vector3.toFloat32Array(): Float32Array<ArrayBuffer> = float32Array(
    x.toFloat(), y.toFloat(), z.toFloat()
)

internal fun Vector2.toFloat32Array(): Float32Array<ArrayBuffer> = float32Array(
    x.toFloat(), y.toFloat()
)

fun VertexElementType.glType(): GLenum = when (this) {
    VertexElementType.UINT8, VertexElementType.VECTOR2_UINT8, VertexElementType.VECTOR3_UINT8, VertexElementType.VECTOR4_UINT8 -> GL.UNSIGNED_BYTE
    VertexElementType.UINT16, VertexElementType.VECTOR2_UINT16, VertexElementType.VECTOR3_UINT16, VertexElementType.VECTOR4_UINT16 -> GL.UNSIGNED_SHORT
    VertexElementType.UINT32, VertexElementType.VECTOR2_UINT32, VertexElementType.VECTOR3_UINT32, VertexElementType.VECTOR4_UINT32 -> GL.UNSIGNED_INT

    VertexElementType.INT8, VertexElementType.VECTOR2_INT8, VertexElementType.VECTOR3_INT8, VertexElementType.VECTOR4_INT8 -> GL.BYTE
    VertexElementType.INT16, VertexElementType.VECTOR2_INT16, VertexElementType.VECTOR3_INT16, VertexElementType.VECTOR4_INT16 -> GL.SHORT
    VertexElementType.INT32, VertexElementType.VECTOR2_INT32, VertexElementType.VECTOR3_INT32, VertexElementType.VECTOR4_INT32 -> GL.INT

    VertexElementType.FLOAT32 -> GL.FLOAT
    VertexElementType.MATRIX22_FLOAT32 -> GL.FLOAT
    VertexElementType.MATRIX33_FLOAT32 -> GL.FLOAT
    VertexElementType.MATRIX44_FLOAT32 -> GL.FLOAT
    VertexElementType.VECTOR2_FLOAT32 -> GL.FLOAT
    VertexElementType.VECTOR3_FLOAT32 -> GL.FLOAT
    VertexElementType.VECTOR4_FLOAT32 -> GL.FLOAT
}

fun DrawPrimitive.glType(): GLenum = when (this) {
    DrawPrimitive.TRIANGLES -> GL.TRIANGLES
    DrawPrimitive.TRIANGLE_FAN -> GL.TRIANGLE_FAN
    DrawPrimitive.TRIANGLE_STRIP -> GL.TRIANGLE_STRIP
    DrawPrimitive.LINES -> GL.LINES
    DrawPrimitive.LINE_STRIP -> GL.LINE_STRIP
    DrawPrimitive.LINE_LOOP -> GL.LINE_LOOP
    DrawPrimitive.POINTS -> GL.POINTS
    DrawPrimitive.PATCHES -> error("not supported")
}

internal fun glStencilTest(test: StencilTest): GLenum {
    return when (test) {
        StencilTest.NEVER -> GL.NEVER
        StencilTest.ALWAYS -> GL.ALWAYS
        StencilTest.LESS -> GL.LESS
        StencilTest.LESS_OR_EQUAL -> GL.LEQUAL
        StencilTest.GREATER -> GL.GREATER
        StencilTest.GREATER_OR_EQUAL -> GL.GEQUAL
        StencilTest.EQUAL -> GL.EQUAL
        StencilTest.NOT_EQUAL -> GL.NOTEQUAL
        else -> throw RuntimeException("unsupported test: $test")
    }
}

internal fun glStencilOp(op: StencilOperation): GLenum {
    return when (op) {
        StencilOperation.KEEP -> GL.KEEP
        StencilOperation.DECREASE -> GL.DECR
        StencilOperation.DECREASE_WRAP -> GL.DECR_WRAP
        StencilOperation.INCREASE -> GL.INCR
        StencilOperation.INCREASE_WRAP -> GL.INCR_WRAP
        StencilOperation.ZERO -> GL.ZERO
        StencilOperation.INVERT -> GL.INVERT
        StencilOperation.REPLACE -> GL.REPLACE
    }
}
internal data class ConversionEntry(val format: ColorFormat,
                                    val type: ColorType,
                                    val glInternalFormat: GLenum,
                                    val glFormat: GLenum,
                                    val glType: GLenum
)

internal fun internalFormat(format: ColorFormat, type: ColorType): Triple<GLenum, GLenum, GLenum> {
    val entries = listOf(

        ConversionEntry(ColorFormat.R, ColorType.UINT8, GL.R8, GL.RED, GL.UNSIGNED_BYTE),
        ConversionEntry(ColorFormat.RG, ColorType.UINT8, GL.RG8, GL.RG, GL.UNSIGNED_BYTE),
        ConversionEntry(ColorFormat.RGB, ColorType.UINT8, GL.RGB, GL.RGB, GL.UNSIGNED_BYTE),
        ConversionEntry(ColorFormat.RGBa, ColorType.UINT8, GL.RGBA, GL.RGBA, GL.UNSIGNED_BYTE),
        ConversionEntry(ColorFormat.RGB, ColorType.UINT8_SRGB, GL.SRGB, GL.RGB, GL.UNSIGNED_BYTE),
        ConversionEntry(ColorFormat.RGBa, ColorType.UINT8_SRGB, GL.SRGB8_ALPHA8, GL.RGBA, GL.UNSIGNED_BYTE),
        ConversionEntry(ColorFormat.R, ColorType.FLOAT16, GL.R16F, GL.RED, GL.HALF_FLOAT),
        ConversionEntry(ColorFormat.RG, ColorType.FLOAT16, GL.RG16F, GL.RG, GL.HALF_FLOAT),
        ConversionEntry(ColorFormat.RGB, ColorType.FLOAT16, GL.RGB16F, GL.RGB, GL.HALF_FLOAT),
        ConversionEntry(ColorFormat.RGBa, ColorType.FLOAT16, GL.RGBA16F, GL.RGBA,  GL.HALF_FLOAT),
        ConversionEntry(ColorFormat.R, ColorType.FLOAT32, GL.R16F, GL.RED, GL.FLOAT),
        ConversionEntry(ColorFormat.RG, ColorType.FLOAT32, GL.RG16F, GL.RG, GL.FLOAT),
        ConversionEntry(ColorFormat.RGB, ColorType.FLOAT32, GL.RGB32F, GL.RGB, GL.FLOAT),
        ConversionEntry(ColorFormat.RGBa, ColorType.FLOAT32,GL.RGBA32F, GL.RGBA, GL.FLOAT),
        // TODO: add compressed types from kotlin-wrappers
//        ConversionEntry(ColorFormat.RGB, ColorType.DXT1, COMPRESSED_RGB_S3TC_DXT1_EXT, GL.RGB, GL.ZERO ),
//        ConversionEntry(ColorFormat.RGBa, ColorType.DXT1, COMPRESSED_RGBA_S3TC_DXT1_EXT, GL.RGBA, GL.ZERO),
//        ConversionEntry(ColorFormat.RGBa, ColorType.DXT3, COMPRESSED_RGBA_S3TC_DXT3_EXT, GL.RGBA, GL.ZERO),
//        ConversionEntry(ColorFormat.RGBa, ColorType.DXT5, COMPRESSED_RGBA_S3TC_DXT5_EXT, GL.RGBA, GL.ZERO)
    )
    for (entry in entries) {
        if (entry.format === format && entry.type === type) {
            return Triple(entry.glInternalFormat, entry.glFormat, entry.glType)
        }
    }
    throw Exception("no conversion entry for $format/$type")
}

internal fun glTextureEnum(target: Int): GLenum {
    return when (target) {
        0 -> GL.TEXTURE0
        1 -> GL.TEXTURE1
        2 -> GL.TEXTURE2
        3 -> GL.TEXTURE3
        4 -> GL.TEXTURE4
        5 -> GL.TEXTURE5
        6 -> GL.TEXTURE6
        7 -> GL.TEXTURE7
        8 -> GL.TEXTURE8
        9 -> GL.TEXTURE9
        10 -> GL.TEXTURE10
        11 -> GL.TEXTURE11
        12 -> GL.TEXTURE12
        13 -> GL.TEXTURE13
        14 -> GL.TEXTURE14
        15 -> GL.TEXTURE15
        16 -> GL.TEXTURE16
        17 -> GL.TEXTURE17
        18 -> GL.TEXTURE18
        19 -> GL.TEXTURE19
        20 -> GL.TEXTURE20
        21 -> GL.TEXTURE21
        22 -> GL.TEXTURE22
        23 -> GL.TEXTURE23
        24 -> GL.TEXTURE24
        25 -> GL.TEXTURE25
        26 -> GL.TEXTURE26
        27 -> GL.TEXTURE27
        28 -> GL.TEXTURE28
        29 -> GL.TEXTURE29
        30 -> GL.TEXTURE30
        31 -> GL.TEXTURE31
        else -> GL.TEXTURE0
    }
}

internal fun glColorAttachment(index: Int): GLenum {
    return when(index) {
        0 -> GL.COLOR_ATTACHMENT0
        1 -> GL.COLOR_ATTACHMENT1
        2 -> GL.COLOR_ATTACHMENT2
        3 -> GL.COLOR_ATTACHMENT3
        4 -> GL.COLOR_ATTACHMENT4
        5 -> GL.COLOR_ATTACHMENT5
        6 -> GL.COLOR_ATTACHMENT6
        7 -> GL.COLOR_ATTACHMENT7
        8 -> GL.COLOR_ATTACHMENT8
        9 -> GL.COLOR_ATTACHMENT9
        10 -> GL.COLOR_ATTACHMENT10
        11 -> GL.COLOR_ATTACHMENT11
        12 -> GL.COLOR_ATTACHMENT12
        13 -> GL.COLOR_ATTACHMENT13
        14 -> GL.COLOR_ATTACHMENT14
        15 -> GL.COLOR_ATTACHMENT15
        else -> error("too many color attachments")
    }
}

internal fun ColorFormat.glFormat(): GLenum {
    return when (this) {
        ColorFormat.R -> GL.LUMINANCE
        ColorFormat.RG -> GL.LUMINANCE_ALPHA
        ColorFormat.RGB -> GL.RGB
        ColorFormat.RGBa -> GL.RGBA
        ColorFormat.BGR -> error("BGR not supported")
        ColorFormat.BGRa -> error("BGRa not supported")
    }
}

@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
internal fun ColorType.glType(): GLenum {
    return when (this) {
        ColorType.UINT8_SRGB, ColorType.UINT8, ColorType.UINT8_INT -> GL.UNSIGNED_BYTE
        ColorType.SINT8_INT -> GL.BYTE
        ColorType.UINT16, ColorType.UINT16_INT -> GL.UNSIGNED_SHORT
        ColorType.SINT16_INT -> GL.SHORT
        ColorType.UINT32_INT -> GL.UNSIGNED_INT
        ColorType.SINT32_INT -> GL.INT
        ColorType.FLOAT16 -> HALF_FLOAT_OES
        ColorType.FLOAT32 -> GL.FLOAT
        ColorType.DXT1, ColorType.DXT3, ColorType.DXT5,
        ColorType.DXT1_SRGB, ColorType.DXT3_SRGB, ColorType.DXT5_SRGB,
        ColorType.BPTC_UNORM, ColorType.BPTC_UNORM_SRGB, ColorType.BPTC_FLOAT, ColorType.BPTC_UFLOAT -> throw RuntimeException("gl type of compressed types cannot be queried")
    } as GLenum
}

