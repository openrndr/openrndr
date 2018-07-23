package org.openrndr.draw

import org.openrndr.color.ColorRGBa
import org.openrndr.math.Matrix55
import org.openrndr.shape.Rectangle

enum class LineJoin {
    BEVEL,
    ROUND
}

enum class LineCap {
    ROUND,
    BUTT,
    SQUARE
}

enum class VertexElementType {
    FLOAT32
}

enum class DrawPrimitive {
    TRIANGLES,
    TRIANGLE_STRIP,
    TRIANGLE_FAN,
    POINTS,
    LINES
}

enum class StencilTest {
    NEVER,
    LESS,
    LESS_OR_EQUAL,
    GREATER,
    GREATER_OR_EQUAL,
    EQUAL,
    NOT_EQUAL,
    ALWAYS,
    DISABLED,
}

enum class StencilOperation {
    KEEP,
    ZERO,
    REPLACE,
    INCREASE,
    INCREASE_WRAP,
    DECREASE,
    DECREASE_WRAP,
    INVERT
}

enum class DepthFormat {
    DEPTH24,
    DEPTH32F,
    DEPTH24_STENCIL8,
    DEPTH32F_STENCIL8
}

@Suppress("EnumEntryName")
enum class ColorFormat {
    R,
    RG,
    RGB,
    BGR,
    RGBa,
    BGRa,
    sRGB,
    sRGBa;

    val componentCount: Int
        get() {
            return when (this) {
                R -> 1
                RG -> 2
                BGR, RGB, sRGB -> 3
                BGRa, RGBa, sRGBa -> 4
            }
        }
}

enum class ColorType {
    UINT8,
    UINT16,
    FLOAT16,
    FLOAT32,
    DXT1,
    DXT3,
    DXT5;

    val componentSize: Int
        get() {
            return when (this) {
                UINT8 -> 1
                UINT16, FLOAT16 -> 2
                FLOAT32 -> 4
                DXT1, DXT3, DXT5 -> throw RuntimeException("component size of compressed types cannot be queried")
            }
        }
}

enum class CullTestPass {
    ALWAYS,
    FRONT,
    BACK,
    NEVER
}

enum class DepthTestPass {
    ALWAYS,
    LESS,
    LESS_OR_EQUAL,
    EQUAL,
    GREATER,
    GREATER_OR_EQUAL,
    NEVER
}

enum class BlendMode {
    OVER,
    ADD,
    SUBTRACT,
    MULTIPLY,
    REPLACE
}

@Suppress("unused")
class ChannelMask(val red: Boolean, val green: Boolean, val blue: Boolean, val alpha: Boolean) {
    companion object {
        const val RED = 1
        const val GREEN = 2
        const val BLUE = 4
        const val ALPHA = 8
        val NONE = ChannelMask(false, false, false, false)
        val ALL = ChannelMask(true, true, true, true)
    }
}

data class DrawStyle(
        var clip: Rectangle? = null,
        var fill: ColorRGBa? = ColorRGBa.WHITE,
        var stroke: ColorRGBa? = ColorRGBa.BLACK,

        var lineCap: LineCap = LineCap.BUTT,
        var lineJoin: LineJoin = LineJoin.BEVEL,

        var strokeWeight: Double = 1.0,
        var smooth: Boolean = true,

        var quality: DrawQuality = DrawQuality.QUALITY,

        var depthTestPass: DepthTestPass = DepthTestPass.ALWAYS,
        var depthWrite: Boolean = false,
        var blendMode: BlendMode = BlendMode.OVER,
        var cullTestPass: CullTestPass = CullTestPass.ALWAYS,
        var channelWriteMask: ChannelMask = ChannelMask(true, true, true, true),

        var shadeStyle: ShadeStyle? = null,
        var fontMap: FontMap? = null,

        var stencil: StencilStyle = StencilStyle(),
        var frontStencil: StencilStyle = stencil,
        var backStencil: StencilStyle = stencil,

        var colorMatrix: Matrix55 = Matrix55.IDENTITY
) {

    fun applyToShader(shader: Shader) {
        if (shader.hasUniform("u_fill")) {
            shader.uniform("u_fill", fill ?: ColorRGBa.TRANSPARENT)
        }

        if (shader.hasUniform("u_stroke")) {
            shader.uniform("u_stroke", stroke ?: ColorRGBa.TRANSPARENT)
        }

        if (shader.hasUniform("u_strokeWeight")) {
            shader.uniform("u_strokeWeight", if (stroke != null) strokeWeight else 0.0)
        }

        if (shader.hasUniform("u_colorMatrix")) {
            shader.uniform("u_colorMatrix", colorMatrix.floatArray)
        }
    }
}
