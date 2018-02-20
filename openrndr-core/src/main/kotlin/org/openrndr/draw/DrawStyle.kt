package org.openrndr.draw

import org.openrndr.color.ColorRGBa
import org.openrndr.math.Matrix55

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
    RGBa,
    sRGB,
    sRGBa;

    val componentCount: Int
        get() {
            return when (this) {
                R -> 1
                RG -> 2
                RGB, sRGB -> 3
                RGBa, sRGBa -> 4
            }
        }
}

enum class ColorType {
    UINT8,
    UINT16,
    FLOAT16,
    FLOAT32;

    val componentSize: Int
        get() {
            return when (this) {
                UINT8 -> 1
                UINT16, FLOAT16 -> 2
                FLOAT32 -> 4
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
        fill?.let { shader.uniform("u_fill", it) }
        stroke?.let { shader.uniform("u_stroke", it) }
        shader.uniform("u_strokeWeight", strokeWeight)
        shader.uniform("u_colorMatrix", colorMatrix.floatArray)
    }

}
