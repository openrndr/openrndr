package org.openrndr.webgl

import WebGL2RenderingContext
import org.khronos.webgl.Float32Array
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.math.*
import org.khronos.webgl.WebGLRenderingContext as GL

internal fun MinifyingFilter.toGLFilter(): Int {
    return when (this) {
        MinifyingFilter.NEAREST -> GL.NEAREST
        MinifyingFilter.LINEAR -> GL.LINEAR
        MinifyingFilter.LINEAR_MIPMAP_LINEAR -> GL.LINEAR_MIPMAP_LINEAR
        MinifyingFilter.LINEAR_MIPMAP_NEAREST -> GL.LINEAR_MIPMAP_NEAREST
        MinifyingFilter.NEAREST_MIPMAP_LINEAR -> GL.NEAREST_MIPMAP_LINEAR
        MinifyingFilter.NEAREST_MIPMAP_NEAREST -> GL.NEAREST_MIPMAP_NEAREST
    }
}

internal fun MagnifyingFilter.toGLFilter(): Int {
    return when (this) {
        MagnifyingFilter.NEAREST -> GL.NEAREST
        MagnifyingFilter.LINEAR -> GL.LINEAR
    }
}

internal fun float32Array(vararg floats: Float): Float32Array {
    return Float32Array(floats.toTypedArray())
}

internal fun Matrix44.toFloat32Array(): Float32Array = float32Array(
    c0r0.toFloat(), c0r1.toFloat(), c0r2.toFloat(), c0r3.toFloat(),
    c1r0.toFloat(), c1r1.toFloat(), c1r2.toFloat(), c1r3.toFloat(),
    c2r0.toFloat(), c2r1.toFloat(), c2r2.toFloat(), c2r3.toFloat(),
    c3r0.toFloat(), c3r1.toFloat(), c3r2.toFloat(), c3r3.toFloat())

internal fun Matrix33.toFloat32Array(): Float32Array = float32Array(
    c0r0.toFloat(), c0r1.toFloat(), c0r2.toFloat(),
    c1r0.toFloat(), c1r1.toFloat(), c1r2.toFloat(),
    c2r0.toFloat(), c2r1.toFloat(), c2r2.toFloat())

internal fun ColorRGBa.toFloat32Array(): Float32Array = float32Array(
    r.toFloat(), g.toFloat(), b.toFloat(), alpha.toFloat()
)

internal fun Vector4.toFloat32Array(): Float32Array = float32Array(
    x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat()
)

internal fun Vector3.toFloat32Array(): Float32Array = float32Array(
    x.toFloat(), y.toFloat(), z.toFloat()
)

internal fun Vector2.toFloat32Array(): Float32Array = float32Array(
    x.toFloat(), y.toFloat()
)

fun VertexElementType.glType(): Int = when (this) {
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

fun DrawPrimitive.glType(): Int = when (this) {
    DrawPrimitive.TRIANGLES -> GL.TRIANGLES
    DrawPrimitive.TRIANGLE_FAN -> GL.TRIANGLE_FAN
    DrawPrimitive.TRIANGLE_STRIP -> GL.TRIANGLE_STRIP
    DrawPrimitive.LINES -> GL.LINES
    DrawPrimitive.LINE_STRIP -> GL.LINE_STRIP
    DrawPrimitive.LINE_LOOP -> GL.LINE_LOOP
    DrawPrimitive.POINTS -> GL.POINTS
    DrawPrimitive.PATCHES -> error("not supported")
}

internal fun glStencilTest(test: StencilTest): Int {
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

internal fun glStencilOp(op: StencilOperation): Int {
    return when (op) {
        StencilOperation.KEEP -> GL.KEEP
        StencilOperation.DECREASE -> GL.DECR
        StencilOperation.DECREASE_WRAP -> GL.DECR_WRAP
        StencilOperation.INCREASE -> GL.INCR
        StencilOperation.INCREASE_WRAP -> GL.INCR_WRAP
        StencilOperation.ZERO -> GL.ZERO
        StencilOperation.INVERT -> GL.INVERT
        StencilOperation.REPLACE -> GL.REPLACE
        else -> throw RuntimeException("unsupported op")
    }
}
internal data class ConversionEntry(val format: ColorFormat, val type: ColorType,
                                    val glInternalFormat: Int,
                                    val glFormat: Int,
                                    val glType: Int
)

internal fun internalFormat(format: ColorFormat, type: ColorType): Triple<Int, Int, Int> {
    val entries = arrayOf(
        ConversionEntry(ColorFormat.R, ColorType.UINT8, WebGL2RenderingContext.R8, WebGL2RenderingContext.RED, GL.UNSIGNED_BYTE),
        ConversionEntry(ColorFormat.RG, ColorType.UINT8, WebGL2RenderingContext.RG8, WebGL2RenderingContext.RG, GL.UNSIGNED_BYTE),
        ConversionEntry(ColorFormat.RGB, ColorType.UINT8, GL.RGB, GL.RGB, GL.UNSIGNED_BYTE),
        ConversionEntry(ColorFormat.RGBa, ColorType.UINT8, GL.RGBA, GL.RGBA, GL.UNSIGNED_BYTE),
        ConversionEntry(ColorFormat.R, ColorType.FLOAT16, WebGL2RenderingContext.R16F, WebGL2RenderingContext.RED, WebGL2RenderingContext.HALF_FLOAT),
        ConversionEntry(ColorFormat.RG, ColorType.FLOAT16, WebGL2RenderingContext.RG16F, WebGL2RenderingContext.RG, WebGL2RenderingContext.HALF_FLOAT),
        ConversionEntry(ColorFormat.RGB, ColorType.FLOAT16, WebGL2RenderingContext.RGB16F, GL.RGB, WebGL2RenderingContext.HALF_FLOAT),
        ConversionEntry(ColorFormat.RGBa, ColorType.FLOAT16, WebGL2RenderingContext.RGBA16F, GL.RGBA,  WebGL2RenderingContext.HALF_FLOAT),
        ConversionEntry(ColorFormat.R, ColorType.FLOAT32, WebGL2RenderingContext.R16F, WebGL2RenderingContext.RED, GL.FLOAT),
        ConversionEntry(ColorFormat.RG, ColorType.FLOAT32, WebGL2RenderingContext.RG16F, WebGL2RenderingContext.RG, GL.FLOAT),
        ConversionEntry(ColorFormat.RGB, ColorType.FLOAT32, WebGL2RenderingContext.RGB32F, GL.RGB, GL.FLOAT),
        ConversionEntry(ColorFormat.RGBa, ColorType.FLOAT32,WebGL2RenderingContext.RGBA32F, GL.RGBA, GL.FLOAT),
        ConversionEntry(ColorFormat.RGB, ColorType.DXT1, COMPRESSED_RGB_S3TC_DXT1_EXT, GL.RGB, 0 ),
        ConversionEntry(ColorFormat.RGBa, ColorType.DXT1, COMPRESSED_RGBA_S3TC_DXT1_EXT, GL.RGBA, 0),
        ConversionEntry(ColorFormat.RGBa, ColorType.DXT3, COMPRESSED_RGBA_S3TC_DXT3_EXT, GL.RGBA, 0),
        ConversionEntry(ColorFormat.RGBa, ColorType.DXT5, COMPRESSED_RGBA_S3TC_DXT5_EXT, GL.RGBA, 0)
    )
    for (entry in entries) {
        if (entry.format === format && entry.type === type) {
            return Triple(entry.glInternalFormat, entry.glFormat, entry.glType)
        }
    }
    throw Exception("no conversion entry for $format/$type")
}

