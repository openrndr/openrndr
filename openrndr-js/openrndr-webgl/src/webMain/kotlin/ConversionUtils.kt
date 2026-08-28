package org.openrndr.webgl

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.math.*
import web.gl.*

internal fun MinifyingFilter.toGLFilter(): GLenum {
    return when (this) {
        MinifyingFilter.NEAREST -> NEAREST
        MinifyingFilter.LINEAR -> LINEAR
        MinifyingFilter.LINEAR_MIPMAP_LINEAR -> LINEAR_MIPMAP_LINEAR
        MinifyingFilter.LINEAR_MIPMAP_NEAREST -> LINEAR_MIPMAP_NEAREST
        MinifyingFilter.NEAREST_MIPMAP_LINEAR -> NEAREST_MIPMAP_LINEAR
        MinifyingFilter.NEAREST_MIPMAP_NEAREST -> NEAREST_MIPMAP_NEAREST
    }
}

internal fun MagnifyingFilter.toGLFilter(): GLenum {
    return when (this) {
        MagnifyingFilter.NEAREST -> NEAREST
        MagnifyingFilter.LINEAR -> LINEAR
    }
}

internal expect fun float32Array(vararg floats: Float): Float32List

internal fun Matrix44.toFloat32Array(): Float32List = float32Array(
    c0r0.toFloat(), c0r1.toFloat(), c0r2.toFloat(), c0r3.toFloat(),
    c1r0.toFloat(), c1r1.toFloat(), c1r2.toFloat(), c1r3.toFloat(),
    c2r0.toFloat(), c2r1.toFloat(), c2r2.toFloat(), c2r3.toFloat(),
    c3r0.toFloat(), c3r1.toFloat(), c3r2.toFloat(), c3r3.toFloat())

internal fun Matrix33.toFloat32Array(): Float32List = float32Array(
    c0r0.toFloat(), c0r1.toFloat(), c0r2.toFloat(),
    c1r0.toFloat(), c1r1.toFloat(), c1r2.toFloat(),
    c2r0.toFloat(), c2r1.toFloat(), c2r2.toFloat())

internal fun ColorRGBa.toFloat32Array(): Float32List = float32Array(
    r.toFloat(), g.toFloat(), b.toFloat(), alpha.toFloat()
)

internal fun Vector4.toFloat32Array(): Float32List = float32Array(
    x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat()
)

internal fun Vector3.toFloat32Array(): Float32List = float32Array(
    x.toFloat(), y.toFloat(), z.toFloat()
)

internal fun Vector2.toFloat32Array(): Float32List = float32Array(
    x.toFloat(), y.toFloat()
)

fun VertexElementType.glType(): GLenum = when (this) {
    VertexElementType.UINT8, VertexElementType.VECTOR2_UINT8, VertexElementType.VECTOR3_UINT8, VertexElementType.VECTOR4_UINT8 -> UNSIGNED_BYTE
    VertexElementType.UINT16, VertexElementType.VECTOR2_UINT16, VertexElementType.VECTOR3_UINT16, VertexElementType.VECTOR4_UINT16 -> UNSIGNED_SHORT
    VertexElementType.UINT32, VertexElementType.VECTOR2_UINT32, VertexElementType.VECTOR3_UINT32, VertexElementType.VECTOR4_UINT32 -> UNSIGNED_INT

    VertexElementType.INT8, VertexElementType.VECTOR2_INT8, VertexElementType.VECTOR3_INT8, VertexElementType.VECTOR4_INT8 -> BYTE
    VertexElementType.INT16, VertexElementType.VECTOR2_INT16, VertexElementType.VECTOR3_INT16, VertexElementType.VECTOR4_INT16 -> SHORT
    VertexElementType.INT32, VertexElementType.VECTOR2_INT32, VertexElementType.VECTOR3_INT32, VertexElementType.VECTOR4_INT32 -> INT

    VertexElementType.FLOAT32 -> FLOAT
    VertexElementType.MATRIX22_FLOAT32 -> FLOAT
    VertexElementType.MATRIX33_FLOAT32 -> FLOAT
    VertexElementType.MATRIX44_FLOAT32 -> FLOAT
    VertexElementType.VECTOR2_FLOAT32 -> FLOAT
    VertexElementType.VECTOR3_FLOAT32 -> FLOAT
    VertexElementType.VECTOR4_FLOAT32 -> FLOAT
}

fun DrawPrimitive.glType(): GLenum = when (this) {
    DrawPrimitive.TRIANGLES -> TRIANGLES
    DrawPrimitive.TRIANGLE_FAN -> TRIANGLE_FAN
    DrawPrimitive.TRIANGLE_STRIP -> TRIANGLE_STRIP
    DrawPrimitive.LINES -> LINES
    DrawPrimitive.LINE_STRIP -> LINE_STRIP
    DrawPrimitive.LINE_LOOP -> LINE_LOOP
    DrawPrimitive.POINTS -> POINTS
    DrawPrimitive.PATCHES -> error("not supported")
}

internal fun glStencilTest(test: StencilTest): GLenum {
    return when (test) {
        StencilTest.NEVER -> NEVER
        StencilTest.ALWAYS -> ALWAYS
        StencilTest.LESS -> LESS
        StencilTest.LESS_OR_EQUAL -> LEQUAL
        StencilTest.GREATER -> GREATER
        StencilTest.GREATER_OR_EQUAL -> GEQUAL
        StencilTest.EQUAL -> EQUAL
        StencilTest.NOT_EQUAL -> NOTEQUAL
        else -> throw RuntimeException("unsupported test: $test")
    }
}

internal fun glStencilOp(op: StencilOperation): GLenum {
    return when (op) {
        StencilOperation.KEEP -> KEEP
        StencilOperation.DECREASE -> DECR
        StencilOperation.DECREASE_WRAP -> DECR_WRAP
        StencilOperation.INCREASE -> INCR
        StencilOperation.INCREASE_WRAP -> INCR_WRAP
        StencilOperation.ZERO -> ZERO
        StencilOperation.INVERT -> INVERT
        StencilOperation.REPLACE -> REPLACE
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

        ConversionEntry(ColorFormat.R, ColorType.UINT8, R8, RED, UNSIGNED_BYTE),
        ConversionEntry(ColorFormat.RG, ColorType.UINT8, RG8, RG, UNSIGNED_BYTE),
        ConversionEntry(ColorFormat.RGB, ColorType.UINT8, RGB, RGB, UNSIGNED_BYTE),
        ConversionEntry(ColorFormat.RGBa, ColorType.UINT8, RGBA, RGBA, UNSIGNED_BYTE),
        ConversionEntry(ColorFormat.RGB, ColorType.UINT8_SRGB, SRGB, RGB, UNSIGNED_BYTE),
        ConversionEntry(ColorFormat.RGBa, ColorType.UINT8_SRGB, SRGB8_ALPHA8, RGBA, UNSIGNED_BYTE),
        ConversionEntry(ColorFormat.R, ColorType.FLOAT16, R16F, RED, HALF_FLOAT),
        ConversionEntry(ColorFormat.RG, ColorType.FLOAT16, RG16F, RG, HALF_FLOAT),
        ConversionEntry(ColorFormat.RGB, ColorType.FLOAT16, RGB16F, RGB, HALF_FLOAT),
        ConversionEntry(ColorFormat.RGBa, ColorType.FLOAT16, RGBA16F, RGBA,  HALF_FLOAT),
        ConversionEntry(ColorFormat.R, ColorType.FLOAT32, R16F, RED, FLOAT),
        ConversionEntry(ColorFormat.RG, ColorType.FLOAT32, RG16F, RG, FLOAT),
        ConversionEntry(ColorFormat.RGB, ColorType.FLOAT32, RGB32F, RGB, FLOAT),
        ConversionEntry(ColorFormat.RGBa, ColorType.FLOAT32,RGBA32F, RGBA, FLOAT),
        // TODO: add compressed types from kotlin-wrappers
//        ConversionEntry(ColorFormat.RGB, ColorType.DXT1, COMPRESSED_RGB_S3TC_DXT1_EXT, RGB, ZERO ),
//        ConversionEntry(ColorFormat.RGBa, ColorType.DXT1, COMPRESSED_RGBA_S3TC_DXT1_EXT, RGBA, ZERO),
//        ConversionEntry(ColorFormat.RGBa, ColorType.DXT3, COMPRESSED_RGBA_S3TC_DXT3_EXT, RGBA, ZERO),
//        ConversionEntry(ColorFormat.RGBa, ColorType.DXT5, COMPRESSED_RGBA_S3TC_DXT5_EXT, RGBA, ZERO)
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
        0 -> TEXTURE0
        1 -> TEXTURE1
        2 -> TEXTURE2
        3 -> TEXTURE3
        4 -> TEXTURE4
        5 -> TEXTURE5
        6 -> TEXTURE6
        7 -> TEXTURE7
        8 -> TEXTURE8
        9 -> TEXTURE9
        10 -> TEXTURE10
        11 -> TEXTURE11
        12 -> TEXTURE12
        13 -> TEXTURE13
        14 -> TEXTURE14
        15 -> TEXTURE15
        16 -> TEXTURE16
        17 -> TEXTURE17
        18 -> TEXTURE18
        19 -> TEXTURE19
        20 -> TEXTURE20
        21 -> TEXTURE21
        22 -> TEXTURE22
        23 -> TEXTURE23
        24 -> TEXTURE24
        25 -> TEXTURE25
        26 -> TEXTURE26
        27 -> TEXTURE27
        28 -> TEXTURE28
        29 -> TEXTURE29
        30 -> TEXTURE30
        31 -> TEXTURE31
        else -> TEXTURE0
    }
}

internal fun glColorAttachment(index: Int): GLenum {
    return when(index) {
        0 -> COLOR_ATTACHMENT0
        1 -> COLOR_ATTACHMENT1
        2 -> COLOR_ATTACHMENT2
        3 -> COLOR_ATTACHMENT3
        4 -> COLOR_ATTACHMENT4
        5 -> COLOR_ATTACHMENT5
        6 -> COLOR_ATTACHMENT6
        7 -> COLOR_ATTACHMENT7
        8 -> COLOR_ATTACHMENT8
        9 -> COLOR_ATTACHMENT9
        10 -> COLOR_ATTACHMENT10
        11 -> COLOR_ATTACHMENT11
        12 -> COLOR_ATTACHMENT12
        13 -> COLOR_ATTACHMENT13
        14 -> COLOR_ATTACHMENT14
        15 -> COLOR_ATTACHMENT15
        else -> error("too many color attachments")
    }
}

internal fun ColorFormat.glFormat(): GLenum {
    return when (this) {
        ColorFormat.R -> LUMINANCE
        ColorFormat.RG -> LUMINANCE_ALPHA
        ColorFormat.RGB -> RGB
        ColorFormat.RGBa -> RGBA
        ColorFormat.BGR -> error("BGR not supported")
        ColorFormat.BGRa -> error("BGRa not supported")
    }
}

@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
internal fun ColorType.glType(): GLenum {
    return when (this) {
        ColorType.UINT8_SRGB, ColorType.UINT8, ColorType.UINT8_INT -> UNSIGNED_BYTE
        ColorType.SINT8_INT -> BYTE
        ColorType.UINT16, ColorType.UINT16_INT -> UNSIGNED_SHORT
        ColorType.SINT16_INT -> SHORT
        ColorType.UINT32_INT -> UNSIGNED_INT
        ColorType.SINT32_INT -> INT
        ColorType.FLOAT16 -> HALF_FLOAT
        ColorType.FLOAT32 -> FLOAT
        ColorType.DXT1, ColorType.DXT3, ColorType.DXT5,
        ColorType.DXT1_SRGB, ColorType.DXT3_SRGB, ColorType.DXT5_SRGB,
        ColorType.BPTC_UNORM, ColorType.BPTC_UNORM_SRGB, ColorType.BPTC_FLOAT, ColorType.BPTC_UFLOAT -> throw RuntimeException("gl type of compressed types cannot be queried")
    } as GLenum
}

